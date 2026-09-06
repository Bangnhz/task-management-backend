package com.example.task_management.dto.request;

import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogCreateRequest {

    @NotNull(message = "Workspace ID is required")
    private Long workspaceId;

    private Long projectId;

    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    @NotNull(message = "Entity ID is required")
    private Long entityId;

    @NotNull(message = "Action is required")
    private ActivityAction action;

    @NotNull(message = "Actor ID is required")
    private Long actorId;

    @Builder.Default
    private Map<String, Object> details = Map.of();
}