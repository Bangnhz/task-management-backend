package com.example.task_management.converter;

import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.response.ActivityLogResponse;
import com.example.task_management.entity.*;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ActivityLogConverter {

    @Autowired
    private UserConverter userConverter;

    /**
     * Chuyển đổi ActivityLogEntity sang ActivityLogResponse trả về cho Client.
     */
    public ActivityLogResponse toResponse(ActivityLogEntity entity) {
        if (entity == null) {
            return null;
        }

        return ActivityLogResponse.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspace() != null ? entity.getWorkspace().getId() : null)
                .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .actor(userConverter.toUserSummaryResponse(entity.getActor()))
                .details(entity.getDetails() != null ? entity.getDetails() : Map.of())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Đóng gói log cho các sự kiện liên quan tới TASK (CREATED, UPDATED, MOVED, COMPLETED, DELETED...).
     */
    public ActivityLogCreateRequest toTaskLogRequest(
            TaskEntity task,
            ActivityAction action,
            Long actorId,
            Map<String, Object> extraDetails
    ) {
        if (task == null || task.getTaskList() == null || task.getTaskList().getProject() == null) {
            return null;
        }

        ProjectEntity project = task.getTaskList().getProject();
        WorkspaceEntity workspace = project.getWorkspace();

        Map<String, Object> details = new HashMap<>();
        details.put("taskTitle", task.getTitle());
        details.put("listTitle", task.getTaskList().getTitle());

        if (task.getPriority() != null) {
            details.put("priority", task.getPriority().name());
        }
        if (task.getAssignee() != null) {
            details.put("assigneeName", task.getAssignee().getFullName());
        }

        if (extraDetails != null && !extraDetails.isEmpty()) {
            details.putAll(extraDetails);
        }

        return ActivityLogCreateRequest.builder()
                .workspaceId(workspace != null ? workspace.getId() : null)
                .projectId(project.getId())
                .entityType(EntityType.TASK)
                .entityId(task.getId())
                .action(action)
                .actorId(actorId)
                .details(details)
                .build();
    }

    /**
     * Đóng gói log cho sự kiện bình luận (COMMENT).
     */
    public ActivityLogCreateRequest toCommentLogRequest(
            CommentEntity comment,
            Long actorId
    ) {
        if (comment == null || comment.getTask() == null) {
            return null;
        }

        TaskEntity task = comment.getTask();
        TaskListEntity taskList = task.getTaskList();
        if (taskList == null || taskList.getProject() == null) {
            return null;
        }

        ProjectEntity project = taskList.getProject();
        WorkspaceEntity workspace = project.getWorkspace();

        Map<String, Object> details = new HashMap<>();
        details.put("taskTitle", task.getTitle());
        // Giới hạn độ dài chuỗi lưu trữ tóm tắt bình luận
        String snippet = comment.getContent();
        if (snippet != null && snippet.length() > 60) {
            snippet = snippet.substring(0, 57) + "...";
        }
        details.put("commentSnippet", snippet);

        return ActivityLogCreateRequest.builder()
                .workspaceId(workspace != null ? workspace.getId() : null)
                .projectId(project.getId())
                .entityType(EntityType.COMMENT)
                .entityId(comment.getId())
                .action(ActivityAction.CREATED)
                .actorId(actorId)
                .details(details)
                .build();
    }

    /**
     * Đóng gói log cho các sự kiện Thành viên dự án (JOINED, LEFT, KICKED...).
     */
    public ActivityLogCreateRequest toProjectMemberLogRequest(
            ProjectMemberEntity member,
            ActivityAction action,
            Long actorId,
            Map<String, Object> extraDetails
    ) {
        if (member == null || member.getProject() == null) {
            return null;
        }

        ProjectEntity project = member.getProject();
        WorkspaceEntity workspace = project.getWorkspace();

        Map<String, Object> details = new HashMap<>();
        if (member.getUser() != null) {
            details.put("memberId", member.getUser().getId());
            details.put("memberName", member.getUser().getFullName());
            details.put("memberEmail", member.getUser().getEmail());
        }
        if (member.getRole() != null) {
            details.put("role", member.getRole().name());
        }

        if (extraDetails != null && !extraDetails.isEmpty()) {
            details.putAll(extraDetails);
        }

        return ActivityLogCreateRequest.builder()
                .workspaceId(workspace != null ? workspace.getId() : null)
                .projectId(project.getId())
                .entityType(EntityType.MEMBER)
                .entityId(member.getId())
                .action(action)
                .actorId(actorId)
                .details(details)
                .build();
    }

    /**
     * Đóng gói log cho sự kiện tạo mới hoặc chỉnh sửa PROJECT.
     */
    public ActivityLogCreateRequest toProjectLogRequest(
            ProjectEntity project,
            ActivityAction action,
            Long actorId
    ) {
        if (project == null || project.getWorkspace() == null) {
            return null;
        }

        Map<String, Object> details = new HashMap<>();
        details.put("projectTitle", project.getTitle());
        if (project.getVisibility() != null) {
            details.put("visibility", project.getVisibility().name());
        }

        return ActivityLogCreateRequest.builder()
                .workspaceId(project.getWorkspace().getId())
                .projectId(project.getId())
                .entityType(EntityType.PROJECT)
                .entityId(project.getId())
                .action(action)
                .actorId(actorId)
                .details(details)
                .build();
    }
}