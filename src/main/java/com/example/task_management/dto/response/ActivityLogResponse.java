package com.example.task_management.dto.response;

import com.example.task_management.dto.response.UserSummaryResponse;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.EntityType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {

    private Long id;
    private Long workspaceId;
    private Long projectId;
    private EntityType entityType;
    private Long entityId;
    private ActivityAction action;
    private UserSummaryResponse actor;
    private Map<String, Object> details;
    private LocalDateTime createdAt;
}