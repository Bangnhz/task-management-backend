package com.example.task_management.config;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import com.example.task_management.dto.request.TaskCreateRequest;
import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.repository.TaskListRepository;
import com.example.task_management.service.TaskListService;
import com.example.task_management.service.TaskService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@Configuration
public class AiToolsConfig {

    public record ProjectTaskListRequest(
            @JsonPropertyDescription("ID của project cần lấy danh sách TaskList") Long projectId) {
    }

    public record TaskRequest(
            @JsonPropertyDescription("ID của project chứa task (cần thiết khi tìm cột theo taskListTitle)") Long projectId,
            @JsonPropertyDescription("Tên hoặc trạng thái của cột (ví dụ: 'Done', 'In Progress', 'To Do')") String taskListTitle,
            @JsonPropertyDescription("ID của task list nếu đã biết (không bắt buộc)") Long taskListId,
            @JsonProperty(required = true) @JsonPropertyDescription("Tiêu đề công việc") String title,
            @JsonPropertyDescription("Mô tả chi tiết công việc") String description,
            @JsonPropertyDescription("Mức độ ưu tiên: LOW, MEDIUM, HIGH, URGENT (mặc định là MEDIUM)") String priority,
            @JsonPropertyDescription("Hạn chót công việc (định dạng YYYY-MM-DD)") String dueDate) {
        public TaskCreateRequest toTaskCreateRequest(Long targetListId) {
            LocalDate parsedDueDate = null;
            if (dueDate != null && !dueDate.isBlank()) {
                try {
                    parsedDueDate = LocalDate.parse(dueDate.trim());
                } catch (Exception ignored) {
                }
            }

            return TaskCreateRequest.builder()
                    .listId(targetListId != null ? targetListId : taskListId)
                    .title(title)
                    .description(description)
                    .priority(priority != null && !priority.isBlank() ? priority.toUpperCase() : "MEDIUM")
                    .dueDate(parsedDueDate)
                    .build();
        }

        public TaskCreateRequest toTaskCreateRequest() {
            return toTaskCreateRequest(taskListId);
        }
    }

    public record BatchCreateTasksRequest(
            @JsonPropertyDescription("ID của project cần tạo hàng loạt task") Long projectId,
            @JsonPropertyDescription("ID mặc định của cột nếu đã biết") Long taskListId,
            @JsonProperty(required = true) List<TaskRequest> tasks) {
    }

    public record TaskListRequest(
            @JsonProperty(required = true) @JsonPropertyDescription("ID của project cần tạo TaskList") Long projectId,
            @JsonProperty(required = true) @JsonPropertyDescription("Tên TaskList/cột/trạng thái cần tạo") String title) {

    }

    public record TaskDeleteRequest(
            @JsonPropertyDescription("ID số của task cần xoá (nếu người dùng có cung cấp ID)") Long taskId,
            @JsonPropertyDescription("ID của project chứa task cần xoá (không bắt buộc nếu đã có taskId)") Long projectId,
            @JsonPropertyDescription("Tên hoặc tiêu đề của task cần xoá (nếu người dùng chỉ cung cấp tên)") String taskTitle,
            @JsonPropertyDescription("ID cột/TaskList nếu người dùng có chỉ định rõ (không bắt buộc)") Long taskListId) {
    }

    @Bean(name = { "createTaskListTool", "createTaskListFunction" })
    @Description("Tạo TaskList/cột/trạng thái mới trong một project")
    public Function<TaskListRequest, String> createTaskListFunction(
            TaskListService taskListService,
            TaskListRepository taskListRepository) {

        return request -> {

            if (request == null) {
                throw new IllegalArgumentException("Request không được null");
            }

            if (request.projectId() == null) {
                throw new IllegalArgumentException("projectId không được để trống");
            }

            if (request.title() == null || request.title().isBlank()) {
                throw new IllegalArgumentException("Tên TaskList không được để trống");
            }

            String title = request.title().trim();

            // Kiểm tra tránh tạo trùng TaskList
            List<TaskListEntity> existingLists = taskListRepository
                    .findByProjectIdOrderByPositionAsc(request.projectId());

            boolean existed = existingLists.stream()
                    .anyMatch(list -> list.getTitle().equalsIgnoreCase(title));

            if (existed) {
                return "TaskList '" + title + "' đã tồn tại trong project "
                        + request.projectId();
            }

            TaskListCreateRequest createRequest = TaskListCreateRequest.builder()
                    .title(title)
                    .isDone("Done".equalsIgnoreCase(title) || "Hoàn thành".equalsIgnoreCase(title))
                    .build();

            taskListService.createTaskList(request.projectId(), createRequest);

            return "Tạo TaskList '" + title + "' thành công trong project "
                    + request.projectId() + "!";
        };
    }

    @Bean(name = { "getTaskListsByProject", "getTaskListsByProjectFunction" })
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

    @Bean(name = { "createTaskTool", "createTaskFunction" })
    @Description("Tạo task mới vào hệ thống quản lý công việc")
    public Function<TaskRequest, String> createTaskFunction(
            TaskService taskService,
            TaskListService taskListService) {
        return request -> {
            if (request == null) {
                return "Lỗi: Dữ liệu yêu cầu không hợp lệ.";
            }

            Long targetListId = request.taskListId();

            // Nếu chưa có taskListId nhưng có taskListTitle và projectId -> tìm qua
            // TaskListService
            if (targetListId == null) {
                if (request.taskListTitle() != null && !request.taskListTitle().isBlank()
                        && request.projectId() != null) {
                    var taskListOpt = taskListService
                            .findByProjectIdAndTitle(request.projectId(), request.taskListTitle());

                    if (taskListOpt.isEmpty()) {
                        return "TaskList '" + request.taskListTitle() + "' không tồn tại trong project "
                                + request.projectId() + ".";
                    }
                    targetListId = taskListOpt.get().getId();
                } else {
                    return "Lỗi: Vui lòng cung cấp taskListId hoặc (taskListTitle và projectId) để tạo task.";
                }
            }

            try {
                taskService.createTask(targetListId, request.toTaskCreateRequest(targetListId));
                return "Tạo task '" + request.title() + "' thành công!";
            } catch (Exception e) {
                return "Tạo task thất bại: " + e.getMessage();
            }
        };
    }

    @Bean(name = { "batchCreateTasksTool" })
    @Description("Tạo hàng loạt task mới cùng lúc vào một project từ danh sách trích xuất được")
    public Function<BatchCreateTasksRequest, String> batchCreateTasksFunction(
            TaskService taskService,
            TaskListRepository taskListRepository) {
        return request -> {
            if (request == null || request.tasks() == null || request.tasks().isEmpty()) {
                return "Không có task nào để tạo.";
            }

            Long projectId = request.projectId();
            List<TaskListEntity> existingLists = (projectId != null)
                    ? taskListRepository.findByProjectIdOrderByPositionAsc(projectId)
                    : List.of();

            int count = 0;
            for (TaskRequest item : request.tasks()) {
                Long targetListId = item.taskListId();

                if (targetListId == null && item.taskListTitle() != null && !existingLists.isEmpty()) {
                    targetListId = existingLists.stream()
                            .filter(l -> l.getTitle().equalsIgnoreCase(item.taskListTitle().trim()))
                            .map(TaskListEntity::getId)
                            .findFirst()
                            .orElse(null);
                }

                if (targetListId == null) {
                    targetListId = request.taskListId();
                }

                if (targetListId == null && !existingLists.isEmpty()) {
                    targetListId = existingLists.get(0).getId();
                }

                if (targetListId != null) {
                    try {
                        taskService.createTask(targetListId, item.toTaskCreateRequest(targetListId));
                        count++;
                    } catch (Exception ignored) {
                    }
                }
            }

            return "Đã tạo thành công " + count + " task vào project " + request.projectId() + "!";
        };
    }

    @Bean(name = { "deleteTaskTool" })
    @Description("Xoá task khỏi hệ thống quản lý công việc")
    public Function<TaskDeleteRequest, String> deleteTaskFunction(TaskService taskService) {
        return request -> {
            if (request == null
                    || (request.projectId() == null && request.taskListId() == null && request.taskTitle().isBlank())
                            && request.taskId() == null) {
                return "Lỗi: Không tìm thấy projectId hoặc taskListId hoặc tên task để thực hiện xoá task.";
            }

            try {
                if (request.taskId() != null) {
                    taskService.deleteTask(request.taskId());
                    return "Đã xoá thành công task có ID " + request.taskId() + "!";
                } else if (!request.taskTitle().isBlank() && request.projectId() != null) {
                    taskService.deleteTaskByTitleAndProject(request.taskTitle(), request.projectId());
                    return "Đã xoá thành công task có tên " + request.taskTitle() + "!";
                } else if (!request.taskTitle().isBlank() && request.taskListId() != null) {
                    taskService.deleteTaskByTitleAndTaskList(request.taskTitle(), request.taskListId());
                    return "Đã xoá thành công task có tên " + request.taskTitle() + "!";
                } else {
                    return "Lỗi: Không tìm thấy thông tin để thực hiện xoá task.";
                }
            } catch (Exception e) {
                return "Xoá task thất bại: " + e.getMessage();
            }
        };
    }
}
