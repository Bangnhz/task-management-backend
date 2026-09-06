package com.example.task_management.service.impl;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.TaskConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.request.TaskCreateRequest;
import com.example.task_management.dto.request.TaskMoveRequest;
import com.example.task_management.dto.request.TaskUpdateRequest;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;
import com.example.task_management.entity.TaskEntity;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.enums.TaskPriority;
import com.example.task_management.repository.TaskListRepository;
import com.example.task_management.repository.TaskRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ActivityLogService;
import com.example.task_management.service.NotificationService;
import com.example.task_management.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskConverter taskConverter;

    @Autowired
    private ActivityLogConverter activityLogConverter;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public List<TaskSummaryResponse> getTaskByUser(Long userId) {
        List<TaskEntity> tasks = taskRepository.findByAssigneeId(userId);

        return tasks.stream()
                .map(task -> TaskSummaryResponse.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .priority(task.getPriority())
                        .startDate(task.getStartDate())
                        .dueDate(task.getDueDate())
                        .listTitle(task.getTaskList() != null ? task.getTaskList().getTitle() : null)
                        .projectTitle(task.getTaskList() != null && task.getTaskList().getProject() != null
                                ? task.getTaskList().getProject().getTitle()
                                : null)
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void moveTask(Long taskId, TaskMoveRequest request) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        TaskListEntity targetList = taskListRepository.findById(request.getTargetListId())
                .orElseThrow(() -> new RuntimeException("TaskList not found"));

        TaskListEntity oldList = task.getTaskList();
        Long actorId = SecurityUtils.getCurrentUserId();
        UserEntity actor = userRepository.findById(actorId).orElseThrow(() -> new RuntimeException("User not found"));
        task.setPosition(request.getPosition() != null ? Double.valueOf(request.getPosition()) : task.getPosition());
        task.setTaskList(targetList);
        taskRepository.save(task);

        if (task.getAssignee() != null && (oldList == null || !oldList.getId().equals(targetList.getId()))) {
            //log
            Map<String, Object> moveDetails = new HashMap<>();
            moveDetails.put("fromListTitle", oldList != null ? oldList.getTitle() : null);
            moveDetails.put("toListTitle", targetList.getTitle());

            ActivityLogCreateRequest logRequest = activityLogConverter.toTaskLogRequest(
                    task,
                    ActivityAction.MOVED,
                    actorId,
                    moveDetails
            );
            activityLogService.log(logRequest);
            //notification
            notificationService.sendNotification(
                    task.getAssignee(),
                    actor,
                    "Trạng thái task đã thay đổi",
                    "Task \"" + task.getTitle() + "\" đã được chuyển sang danh sách \"" + targetList.getTitle() + "\"",
                    NotificationType.STATUS_CHANGED,
                    task.getId()
            );
        }
    }

    @Override
    public List<TaskCardResponse> getTasksByProjectId(Long projectId) {
        List<TaskEntity> tasks = taskRepository.findByTaskListProjectId(projectId);
        return tasks.stream()
                .map(taskConverter::toTaskCardResponse)
                .toList();
    }

    @Override
    @Transactional
    public TaskCardResponse createTask(Long taskListId, TaskCreateRequest request) {
        TaskListEntity list = taskListRepository.findById(taskListId)
                .orElseThrow(() -> new RuntimeException("TaskList not found with id: " + taskListId));

        UserEntity createdBy = null;
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                createdBy = userRepository.findById(currentUserId).orElse(null);
            }
        } catch (Exception ignored) {
        }
        if (createdBy == null) {
            createdBy = userRepository.findAll().stream().findFirst().orElse(null);
        }

        UserEntity assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId()).orElse(null);
        }

        Double maxPos = taskRepository.findMaxPositionByTaskListId(taskListId);
        Double position = (maxPos != null) ? maxPos + 1000.0 : 1000.0;

        TaskPriority priority = TaskPriority.MEDIUM;
        if (request.getPriority() != null) {
            try {
                priority = TaskPriority.valueOf(request.getPriority().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        TaskEntity task = TaskEntity.builder()
                .taskList(list)
                .title(request.getTitle())
                .description(request.getDescription())
                .position(position)
                .priority(priority)
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .assignee(assignee)
                .createdBy(createdBy)
                .build();

        TaskEntity savedTask = taskRepository.save(task);

        // Ghi log tạo task mới
        Long actorId = (createdBy != null) ? createdBy.getId() : SecurityUtils.getCurrentUserId();
        ActivityLogCreateRequest logRequest = activityLogConverter.toTaskLogRequest(
                savedTask,
                ActivityAction.CREATED,
                actorId,
                null
        );
        activityLogService.log(logRequest);

        return taskConverter.toTaskCardResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskCardResponse updateTask(Long taskId, TaskUpdateRequest request) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        Long actorId = SecurityUtils.getCurrentUserId();
        UserEntity actor = userRepository.findById(actorId).orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> changeDetails = new HashMap<>();

        if (request.getTitle() != null && !request.getTitle().isBlank() && !request.getTitle().equals(task.getTitle())) {
            changeDetails.put("oldTitle", task.getTitle());
            changeDetails.put("newTitle", request.getTitle());
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            try {
                TaskPriority newPriority = TaskPriority.valueOf(request.getPriority().toUpperCase());
                if (task.getPriority() != newPriority) {
                    changeDetails.put("oldPriority", task.getPriority() != null ? task.getPriority().name() : null);
                    changeDetails.put("newPriority", newPriority.name());
                    task.setPriority(newPriority);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssigneeId() != null) {
            Long currentAssigneeId = (task.getAssignee() != null) ? task.getAssignee().getId() : null;
            if (!Objects.equals(currentAssigneeId, request.getAssigneeId())) {
                UserEntity assignee = userRepository.findById(request.getAssigneeId()).orElse(null);
                changeDetails.put("oldAssignee", task.getAssignee() != null ? task.getAssignee().getFullName() : null);
                changeDetails.put("newAssignee", assignee != null ? assignee.getFullName() : null);
                task.setAssignee(assignee);
            }
            if (task.getAssignee() != null) {
                notificationService.sendNotification(
                        task.getAssignee(),
                        actor,
                        "Bạn được giao một task mới",
                        "Bạn đã được chỉ định vào task: " + task.getTitle(),
                        NotificationType.ASSIGNED,
                        task.getId()
                );
            }
        }
        if (request.getListId() != null) {
            TaskListEntity list = taskListRepository.findById(request.getListId()).orElse(null);
            if (list != null && !list.getId().equals(task.getTaskList().getId())) {
                changeDetails.put("fromListTitle", task.getTaskList().getTitle());
                changeDetails.put("toListTitle", list.getTitle());
                task.setTaskList(list);
            }
        }

        TaskEntity updatedTask = taskRepository.save(task);

        // Ghi log cập nhật task
        ActivityLogCreateRequest logRequest = activityLogConverter.toTaskLogRequest(
                updatedTask,
                ActivityAction.UPDATED,
                actorId,
                changeDetails
        );
        activityLogService.log(logRequest);

        return taskConverter.toTaskCardResponse(updatedTask);
    }

    @Override
    public TaskCardResponse getTaskById(Long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        return taskConverter.toTaskCardResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        Long actorId = SecurityUtils.getCurrentUserId();

        // Ghi log trước khi xóa Task khỏi CSDL
        ActivityLogCreateRequest logRequest = activityLogConverter.toTaskLogRequest(
                task,
                ActivityAction.DELETED,
                actorId,
                null
        );
        activityLogService.log(logRequest);

        taskRepository.delete(task);
    }
}