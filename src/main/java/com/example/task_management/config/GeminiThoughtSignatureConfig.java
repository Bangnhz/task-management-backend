package com.example.task_management.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class GeminiThoughtSignatureConfig {

    private final Map<String, String> signatureCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public RestClientCustomizer geminiThoughtSignatureRestClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestInterceptor(new GeminiThoughtSignatureInterceptor());
    }

    public class GeminiThoughtSignatureInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            HttpRequest actualRequest = request;
            byte[] modifiedBody = body;
            if (body != null && body.length > 0) {
                try {
                    JsonNode root = objectMapper.readTree(body);
                    if (root.has("messages") && root.get("messages").isArray()) {
                        boolean modified = false;
                        for (JsonNode message : root.get("messages")) {
                            if (message.has("tool_calls") && message.get("tool_calls").isArray()) {
                                for (JsonNode toolCallNode : message.get("tool_calls")) {
                                    if (toolCallNode instanceof ObjectNode toolCallObj) {
                                        if (!toolCallObj.has("extra_content") || toolCallObj.get("extra_content").isNull()) {
                                            String toolCallId = toolCallObj.has("id") ? toolCallObj.get("id").asText() : "";
                                            String signature = signatureCache.getOrDefault(toolCallId, "skip_thought_signature_validator");

                                            ObjectNode extraContent = toolCallObj.putObject("extra_content");
                                            ObjectNode google = extraContent.putObject("google");
                                            google.put("thought_signature", signature);
                                            modified = true;
                                            log.info("Gemini Interceptor: Attached thought_signature to tool_call id '{}' (using {})",
                                                    toolCallId,
                                                    signature.equals("skip_thought_signature_validator") ? "skip_validator" : "cached_signature");
                                        }
                                    }
                                }
                            }
                        }
                        if (modified) {
                            modifiedBody = objectMapper.writeValueAsBytes(root);
                            final byte[] finalBody = modifiedBody;
                            try {
                                request.getHeaders().setContentLength(finalBody.length);
                            } catch (Exception ignored) {
                            }
                            actualRequest = new org.springframework.http.client.support.HttpRequestWrapper(request) {
                                private final HttpHeaders headers;
                                {
                                    this.headers = new HttpHeaders();
                                    this.headers.putAll(request.getHeaders());
                                    this.headers.setContentLength(finalBody.length);
                                }

                                @Override
                                public HttpHeaders getHeaders() {
                                    return this.headers;
                                }
                            };
                        }
                    }
                } catch (Exception e) {
                    log.warn("Gemini Interceptor: Failed to process request body: {}", e.getMessage());
                }
            }

            ClientHttpResponse originalResponse = execution.execute(actualRequest, modifiedBody);

            if (originalResponse.getStatusCode().is2xxSuccessful()) {
                try {
                    BufferingClientHttpResponse bufferedResponse = new BufferingClientHttpResponse(originalResponse);
                    if (bufferedResponse.bodyBytes != null && bufferedResponse.bodyBytes.length > 0) {
                        JsonNode resNode = objectMapper.readTree(bufferedResponse.bodyBytes);
                        if (resNode.has("choices") && resNode.get("choices").isArray()) {
                            for (JsonNode choice : resNode.get("choices")) {
                                JsonNode msg = choice.path("message");
                                if (msg.has("tool_calls") && msg.get("tool_calls").isArray()) {
                                    for (JsonNode tc : msg.get("tool_calls")) {
                                        String id = tc.path("id").asText(null);
                                        String sig = tc.path("extra_content").path("google").path("thought_signature").asText(null);
                                        if (id != null && sig != null) {
                                            signatureCache.put(id, sig);
                                            log.info("Gemini Interceptor: Cached thought_signature for tool_call id '{}'", id);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return bufferedResponse;
                } catch (Exception e) {
                    log.warn("Gemini Interceptor: Failed to buffer response: {}", e.getMessage());
                    return originalResponse;
                }
            }

            return originalResponse;
        }
    }

    private static class BufferingClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse response;
        private final byte[] bodyBytes;

        public BufferingClientHttpResponse(ClientHttpResponse response) throws IOException {
            this.response = response;
            InputStream is = response.getBody();
            this.bodyBytes = (is != null) ? StreamUtils.copyToByteArray(is) : new byte[0];
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(this.bodyBytes);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }

        @Override
        public void close() {
            response.close();
        }
    }
}
