package com.example.task_management.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.task_management.service.FileParseService;

import java.util.List;
import java.util.Map;
import java.util.Set;

// AiToolsConfig
//     ↓
// @Bean(name = "createTaskTool")
//     ↓
// Spring ApplicationContext
//     ↓
// ChatBotController
//     ↓
// .withFunction("createTaskTool")
//     ↓
// AI quyết định gọi tool
//     ↓
// Function<TaskRequest, String>
//     ↓
// TaskService.createTask(...)
@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatBotController {

    private final ChatModel groqChatModel;
    private final ChatModel geminiChatModel;
    private final FileParseService fileParserService;

    public ChatBotController(
            @Qualifier("groqChatModel") ChatModel groqChatModel,
            @Qualifier("geminiChatModel") ChatModel geminiChatModel,
            FileParseService fileParserService) {
        this.groqChatModel = groqChatModel;
        this.geminiChatModel = geminiChatModel;
        this.fileParserService = fileParserService;
    }

    // Danh sách model Gemini (ưu tiên chạy trước)
    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-3.5-flash-lite", // Nhẹ nhất, quota Free Tier thoải mái nhất
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-3.7-flash");

    // Danh sách model Groq (fallback khi Gemini lỗi hoặc hết quota)
    private static final List<String> GROQ_MODELS = List.of(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant");

    private static final Set<String> AI_TOOLS = Set.of(
            "createTaskTool",
            "batchCreateTasksTool",
            "createTaskListTool",
            "getTaskListsByProject",
            "deleteTaskTool");

    @PostMapping
    public ResponseEntity<Map<String, String>> chatWithBot(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Project-Id", required = false) Long projectId) {
        String userMessageText = request.get("message");
        if (userMessageText == null || userMessageText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung tin nhắn không được để trống."));
        }

        String projectContext = projectId != null ? String.valueOf(projectId)
                : "Chưa xác định (hỏi người dùng projectId hoặc tên dự án nếu cần thực hiện thao tác)";

        SystemMessage systemMessage = new SystemMessage(
                """
                        Bạn là trợ lý quản lý công việc trong hệ thống Task Management.

                        NGỮ CẢNH:
                        - Project ID hiện tại: %s

                        QUY TẮC TẠO TASK:

                        1. Khi người dùng yêu cầu tạo task, cần xác định:
                            - projectId
                            - tên TaskList/trạng thái/status
                            - title
                            - description
                            - priority
                            - dueDate
                            - Tránh nhầm lẫn thêm task list với thêm task

                        2. Xác định projectId theo thứ tự ưu tiên:
                            - Nếu người dùng CÓ chỉ định rõ tên project hoặc projectId trong yêu cầu: ưu tiên sử dụng project đó.
                            - Nếu người dùng KHÔNG đề cập đến project: BẮT BUỘC tự động sử dụng Project ID hiện tại (%s).
                            - Chỉ hỏi người dùng projectId nếu cả hai điều kiện trên đều không có.

                        3. Nếu người dùng cung cấp tên trạng thái ví dụ:
                            Backlog, To Do, In Progress, Done,
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

                        QUY TẮC TẠO TASKLIST:

                        10. Khi người dùng yêu cầu tạo TaskList/cột/trạng thái/status mới,
                            cần xác định:
                            - projectId
                            - tên TaskList

                        11. Xác định projectId theo thứ tự ưu tiên:
                            - Nếu người dùng chỉ định rõ projectId hoặc project:
                                sử dụng project đó.
                            - Nếu người dùng không chỉ định project:
                                BẮT BUỘC sử dụng Project ID hiện tại (%s).

                        12. Trước khi tạo TaskList:
                            BẮT BUỘC gọi getTaskListsByProject(projectId)
                            để kiểm tra danh sách TaskList hiện có.

                        13. Nếu TaskList có cùng tên đã tồn tại:
                            KHÔNG được tạo TaskList mới.
                            Trả lời rằng TaskList đó đã tồn tại.

                        14. Nếu TaskList chưa tồn tại:
                            gọi createTaskListTool với:
                            - projectId
                            - name

                        15. Nếu người dùng yêu cầu tạo nhiều TaskList,
                            ví dụ:
                            "Tạo các cột Backlog, To Do, In Progress và Done"
                            thì:
                            - gọi getTaskListsByProject(projectId) trước
                            - kiểm tra từng tên
                            - chỉ tạo những TaskList chưa tồn tại
                            - không tạo trùng TaskList đã có.

                        16. Khi tạo TaskList thành công, trả lời ngắn gọn:
                            tên TaskList và projectId.

                        17. Phân biệt rõ:
                            - "Tạo task" -> sử dụng createTaskTool
                            - "Tạo tasklist", "tạo cột", "tạo trạng thái",
                                "thêm cột", "thêm trạng thái"
                                -> sử dụng createTaskListTool.

                        18. Khi người dùng yêu cầu tạo task và có chỉ định tên cột/trạng thái,
                        ví dụ: "Tạo task A vào cột To Do",
                        thì phải hiểu rằng người dùng muốn tạo TASK trong một TaskList đã tồn tại.

                        QUY TẮC XOÁ TASK:

                        19. Khi người dùng yêu cầu xoá task ("xoá task", "delete task", "remove task"):
                            - Nếu có taskId: gọi deleteTaskTool với taskId.
                            - Nếu chỉ có tên task: gọi deleteTaskTool với:
                                + taskTitle = tên task
                                + projectId = %s (hoặc projectId người dùng chỉ định)
                                + taskListId = ID cột (nếu người dùng có chỉ định rõ cột)
                            - Khi xoá thành công, phản hồi ngắn gọn cho người dùng.
                                                """
                        .formatted(projectContext, projectContext, projectContext, projectContext));

        UserMessage userMessage = new UserMessage(userMessageText);
        StringBuilder errorDetails = new StringBuilder();

        // 1. Ưu tiên sử dụng Gemini
        for (String modelName : GEMINI_MODELS) {
            try {
                log.info("Đang xử lý request bằng Gemini model: {}", modelName);

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withModel(modelName)
                        .withFunctions(AI_TOOLS)
                        .build();

                Prompt prompt = new Prompt(List.of(systemMessage, userMessage), options);
                String aiReply = geminiChatModel.call(prompt).getResult().getOutput().getContent();

                if (aiReply == null || aiReply.isBlank()) {
                    aiReply = "Đã thực hiện xong yêu cầu của bạn!";
                }

                return ResponseEntity.ok(Map.of("reply", aiReply));

            } catch (Exception e) {
                log.warn("Gemini model {} thất bại: {}", modelName, e.getMessage());
                errorDetails.append("[Gemini-").append(modelName).append(": ").append(e.getMessage()).append("] ");
            }
        }

        // 2. Nếu tất cả model Gemini đều lỗi (hết quota, timeout, ...), chuyển sang
        // fallback dùng Groq
        log.warn("Toàn bộ model Gemini thất bại, chuyển sang sử dụng Groq fallback...");
        for (String groqModel : GROQ_MODELS) {
            try {
                log.info("Đang xử lý request bằng Groq model: {}", groqModel);

                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withModel(groqModel)
                        .withFunctions(AI_TOOLS)
                        .build();

                Prompt prompt = new Prompt(List.of(systemMessage, userMessage), options);
                String aiReply = groqChatModel.call(prompt).getResult().getOutput().getContent();

                if (aiReply == null || aiReply.isBlank()) {
                    aiReply = "Đã thực hiện xong yêu cầu của bạn!";
                }

                return ResponseEntity.ok(Map.of("reply", aiReply));

            } catch (Exception e) {
                log.warn("Groq model {} thất bại: {}", groqModel, e.getMessage());
                errorDetails.append("[Groq-").append(groqModel).append(": ").append(e.getMessage()).append("] ");
            }
        }

        // 3. Nếu cả Gemini và Groq đều lỗi, trả về HTTP 500 kèm chi tiết
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "reply", "Không thể kết nối đến các model AI (Gemini và Groq đều thất bại).",
                        "error", errorDetails.toString()));
    }

    @PostMapping(value = "/upload-and-create", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadFileAndCreateTasks(
            @RequestParam(value = "file[]", required = false) MultipartFile[] fileArrayParam,
            @RequestParam(value = "files", required = false) MultipartFile[] filesParam,
            @RequestParam(value = "file", required = false) MultipartFile[] fileParam,
            @RequestParam(value = "message", required = false) String userInstruction,
            @RequestHeader(value = "X-Project-Id", required = false) Long projectId) {

        MultipartFile[] files = (fileArrayParam != null && fileArrayParam.length > 0) ? fileArrayParam
                : ((filesParam != null && filesParam.length > 0) ? filesParam : fileParam);

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn ít nhất một file để tải lên."));
        }

        if (projectId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Thiếu header X-Project-Id. Vui lòng mở trang dự án trước khi tải file."));
        }

        try {
            // 1. Trích xuất text từ mảng file Excel / Word bằng FileParseService
            String combinedText = fileParserService.extractContent(files);

            if (combinedText == null || combinedText.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Không trích xuất được dữ liệu nào từ các file đã tải lên."));
            }

            int validFiles = 0;
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    validFiles++;
                }
            }

            // 2. Ghép nội dung file vào Prompt gửi AI
            String promptContent = """
                    Người dùng đã tải lên %d tài liệu chứa danh sách công việc.
                    YÊU CẦU CỦA NGƯỜI DÙNG: %s

                    DỮ LIỆU ĐỌC ĐƯỢC TỪ CÁC FILE:
                    %s

                    HÃY THỰC HIỆN:
                    1. Đọc và phân tích các dòng công việc trên (tiêu đề, hạn chót, trạng thái, người làm...).
                    2. Gọi công cụ `batchCreateTasksTool` để tạo toàn bộ các task này vào project ID: %d.
                    """.formatted(
                    validFiles,
                    userInstruction != null && !userInstruction.isBlank() ? userInstruction
                            : "Tạo các task này vào dự án",
                    combinedText,
                    projectId);

            UserMessage userMessage = new UserMessage(promptContent);
            StringBuilder errorDetails = new StringBuilder();

            // 3. Ưu tiên sử dụng Gemini
            for (String modelName : GEMINI_MODELS) {
                try {
                    log.info("Đang xử lý upload file bằng Gemini model: {}", modelName);

                    OpenAiChatOptions options = OpenAiChatOptions.builder()
                            .withModel(modelName)
                            .withFunctions(AI_TOOLS)
                            .build();

                    Prompt prompt = new Prompt(List.of(userMessage), options);
                    String aiReply = geminiChatModel.call(prompt).getResult().getOutput().getContent();

                    if (aiReply == null || aiReply.isBlank()) {
                        aiReply = "Đã trích xuất và tạo xong các task từ file tải lên!";
                    }

                    return ResponseEntity.ok(Map.of("reply", aiReply));

                } catch (Exception e) {
                    log.warn("Gemini model {} thất bại khi xử lý upload file: {}", modelName, e.getMessage());
                    errorDetails.append("[Gemini-").append(modelName).append(": ").append(e.getMessage()).append("] ");
                }
            }

            // 4. Nếu Gemini lỗi, chuyển sang Groq fallback
            log.warn("Toàn bộ model Gemini thất bại khi xử lý file, chuyển sang Groq fallback...");
            for (String groqModel : GROQ_MODELS) {
                try {
                    log.info("Đang xử lý upload file bằng Groq model: {}", groqModel);

                    OpenAiChatOptions options = OpenAiChatOptions.builder()
                            .withModel(groqModel)
                            .withFunctions(AI_TOOLS)
                            .build();

                    Prompt prompt = new Prompt(List.of(userMessage), options);
                    String aiReply = groqChatModel.call(prompt).getResult().getOutput().getContent();

                    if (aiReply == null || aiReply.isBlank()) {
                        aiReply = "Đã trích xuất và tạo xong các task từ file tải lên!";
                    }

                    return ResponseEntity.ok(Map.of("reply", aiReply));

                } catch (Exception e) {
                    log.warn("Groq model {} thất bại khi xử lý upload file: {}", groqModel, e.getMessage());
                    errorDetails.append("[Groq-").append(groqModel).append(": ").append(e.getMessage()).append("] ");
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "reply", "Không thể xử lý tạo task từ file bằng AI (Gemini và Groq đều thất bại).",
                            "error", errorDetails.toString()));

        } catch (Exception e) {
            log.error("Lỗi xử lý file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi xử lý file: " + e.getMessage()));
        }
    }
}