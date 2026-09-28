package com.example.task_management.config;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import com.example.task_management.dto.request.TaskCreateRequest;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.repository.TaskListRepository;
import com.example.task_management.service.TaskService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@Configuration
public class AiToolsConfig {

    public record ProjectTaskListRequest(
            @JsonPropertyDescription("ID của project cần lấy danh sách TaskList. Mặc định là 1 nếu người dùng không chỉ định")
            Long projectId
    ) {}

    public record TaskRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("ID của task list (cột) cần thêm task vào")
            Long taskListId,

            @JsonProperty(required = true)
            @JsonPropertyDescription("Tiêu đề công việc")
            String title,

            @JsonPropertyDescription("Mô tả chi tiết công việc")
            String description,

            @JsonPropertyDescription("Mức độ ưu tiên: LOW, MEDIUM, HIGH, URGENT (mặc định là MEDIUM)")
            String priority,

            @JsonPropertyDescription("Hạn chót công việc (định dạng YYYY-MM-DD)")
            String dueDate
    ) {
        public TaskCreateRequest toTaskCreateRequest() {
            LocalDate parsedDueDate = null;
            if (dueDate != null && !dueDate.isBlank()) {
                try {
                    parsedDueDate = LocalDate.parse(dueDate.trim());
                } catch (Exception ignored) {
                }
            }

            return TaskCreateRequest.builder()
                    .listId(taskListId)
                    .title(title)
                    .description(description)
                    .priority(priority != null && !priority.isBlank() ? priority.toUpperCase() : "MEDIUM")
                    .dueDate(parsedDueDate)
                    .build();
        }
    }

    @Bean(name = {"getTaskListsByProject", "getTaskListsByProjectFunction"})
    @Description("Lấy danh sách các TaskList (cột/trạng thái như Backlog, To Do, In Progress, Done) của project")
    public Function<ProjectTaskListRequest, String> getTaskListsByProject(TaskListRepository taskListRepository) {
        return request -> {
            Long projectId = (request != null && request.projectId() != null) ? request.projectId() : 1L;
            List<TaskListEntity> lists = taskListRepository.findByProjectIdOrderByPositionAsc(projectId);
            if (lists.isEmpty()) {
                return "Không tìm thấy TaskList nào cho project id " + projectId;
            }
            StringBuilder sb = new StringBuilder("Danh sách TaskList của project ").append(projectId).append(":\n");
            for (TaskListEntity list : lists) {
                sb.append("- ID: ").append(list.getId()).append(", Tên: \"").append(list.getTitle()).append("\"\n");
            }
            return sb.toString();
        };
    }

    @Bean(name = {"createTaskTool", "createTaskFunction"})
    @Description("Tạo task mới vào hệ thống quản lý công việc")
    public Function<TaskRequest, String> createTaskFunction(TaskService taskService) {
        return request -> {
            // Gọi thẳng service nội bộ, không cần qua HTTP
            taskService.createTask(request.taskListId(), request.toTaskCreateRequest());
            return "Tạo task '" + request.title() + "' thành công!";
        };
    }
}
