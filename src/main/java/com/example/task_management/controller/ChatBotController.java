package com.example.task_management.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatBotController {

    private final ChatModel chatModel;

    // Danh sách các model thế hệ 3.x thực tế đang khả dụng trong tài khoản của bạn
    // Tuyệt đối KHÔNG dùng bất kỳ model nào có chữ "pro" (vì Free tier bị limit =
    // 0)
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.5-flash-lite", // Nhẹ nhất, quota Free Tier thoải mái nhất
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-3.7-flash");

    @PostMapping
    public ResponseEntity<Map<String, String>> chatWithBot(@RequestBody Map<String, String> request) {
        String userMessageText = request.get("message");
        if (userMessageText == null || userMessageText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống."));
        }

        SystemMessage systemMessage = new SystemMessage("""
                    Bạn là trợ lý quản lý công việc trong hệ thống Task Management.

                    QUY TẮC TẠO TASK:

                    1. Khi người dùng yêu cầu tạo task, cần xác định:
                       - projectId
                       - tên TaskList/trạng thái
                       - title
                       - description
                       - priority
                       - dueDate

                    2. Nếu thiếu projectId:
                       KHÔNG gọi createTaskTool.
                       Hãy hỏi người dùng projectId.

                    3. Nếu người dùng cung cấp tên trạng thái như:
                       Backlog, To Do, In Progress, Done
                       thì KHÔNG được tự đoán taskListId.

                    4. Luôn gọi getTaskListsByProject(projectId)
                       để lấy danh sách TaskList của project.

                    5. Tìm TaskList có tên khớp với trạng thái người dùng yêu cầu.

                    6. Chỉ sau khi xác định được đúng taskListId mới được gọi createTaskTool.

                    7. Tuyệt đối không tự mặc định taskListId.

                    8. Priority hợp lệ:
                       LOW, MEDIUM, HIGH, URGENT.
                       Nếu không được chỉ định thì dùng MEDIUM.

                    9. Khi tạo task thành công, trả lời ngắn gọn:
                       tên task, trạng thái và priority.
                """);

        UserMessage userMessage = new UserMessage(userMessageText);
        StringBuilder errorDetails = new StringBuilder();

        for (String modelName : FALLBACK_MODELS) {
            try {
                log.info("Đang xử lý request bằng model: {}", modelName);

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withModel(modelName)
                        .withFunction("createTaskTool")
                        .withFunction("getTaskListsByProject")
                        .build();

                Prompt prompt = new Prompt(List.of(systemMessage, userMessage), options);
                String aiReply = chatModel.call(prompt).getResult().getOutput().getContent();

                if (aiReply == null || aiReply.isBlank()) {
                    aiReply = "Đã thực hiện xong yêu cầu của bạn!";
                }

                return ResponseEntity.ok(Map.of("reply", aiReply));

            } catch (Exception e) {
                log.warn("Model {} thất bại: {}", modelName, e.getMessage());
                errorDetails.append("[").append(modelName).append(": ").append(e.getMessage()).append("] ");
            }
        }

        // Nếu tất cả model trong danh sách fallback đều lỗi, trả về HTTP 500 kèm chi
        // tiết
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "reply", "Không thể kết nối đến các model AI.",
                        "error", errorDetails.toString()));
    }
}