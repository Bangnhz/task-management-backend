package com.example.task_management.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiModelConfig {

    private final ApplicationContext applicationContext;

    public AiModelConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private FunctionCallbackContext createFunctionCallbackContext() {
        FunctionCallbackContext context = new FunctionCallbackContext();
        context.setApplicationContext(applicationContext);
        return context;
    }

    private RetryTemplate createRetryTemplate() {
        // Tối đa 1 lần thử để chuyển sang fallback nhanh chóng khi model chính gặp lỗi (429, 404, timeout...)
        return RetryTemplate.builder()
                .maxAttempts(1)
                .build();
    }

    private String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String trimmed = baseUrl.trim();
        // Spring AI OpenAiApi tự động nối thêm /v1/chat/completions vào baseUrl
        // Nên nếu baseUrl đã có /v1 hoặc /v1/, ta cắt bỏ để tránh URL bị duplicate thành /v1/v1/...
        if (trimmed.endsWith("/v1/")) {
            return trimmed.substring(0, trimmed.length() - 4);
        } else if (trimmed.endsWith("/v1")) {
            return trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed;
    }

    // =========================
    // GEMINI - PRIMARY
    // =========================

    @Bean("geminiChatModel")
    public OpenAiChatModel geminiChatModel(
            @Value("${ai.gemini.api-key:${GEMINI_API_KEY:${spring.ai.openai.api-key:}}}") String apiKey,
            @Value("${ai.gemini.base-url:${spring.ai.openai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}}") String baseUrl,
            @Value("${ai.gemini.model:${spring.ai.openai.chat.options.model:gemini-3.5-flash-lite}}") String model,
            @Value("${ai.gemini.temperature:0.2}") Float temperature,
            @Qualifier("geminiThoughtSignatureRestClientCustomizer") RestClientCustomizer geminiThoughtSignatureRestClientCustomizer) {

        RestClient.Builder restClientBuilder = RestClient.builder();
        geminiThoughtSignatureRestClientCustomizer.customize(restClientBuilder);

        OpenAiApi openAiApi = new OpenAiApi(sanitizeBaseUrl(baseUrl), apiKey, restClientBuilder, WebClient.builder());

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .build();

        return new OpenAiChatModel(openAiApi, options, createFunctionCallbackContext(), createRetryTemplate());
    }

    // =========================
    // GROQ - FALLBACK
    // =========================

    @Bean("groqChatModel")
    public OpenAiChatModel groqChatModel(
            @Value("${ai.groq.api-key:${GROQ_API_KEY:}}") String apiKey,
            @Value("${ai.groq.base-url:https://api.groq.com/openai}") String baseUrl,
            @Value("${ai.groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${ai.groq.temperature:0.2}") Float temperature) {

        OpenAiApi openAiApi = new OpenAiApi(sanitizeBaseUrl(baseUrl), apiKey);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .build();

        return new OpenAiChatModel(openAiApi, options, createFunctionCallbackContext(), createRetryTemplate());
    }
}