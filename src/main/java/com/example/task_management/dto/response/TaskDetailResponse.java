package com.example.task_management.dto.response;

import com.example.task_management.enums.TaskPriority;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDetailResponse {
    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private String status;
    private List<UserSummaryResponse> assignees;
    private List<CommentResponse> comments;
    private List<AttachmentResponse> attachments;
}
