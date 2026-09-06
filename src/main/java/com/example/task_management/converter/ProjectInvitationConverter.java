package com.example.task_management.converter;

import com.example.task_management.dto.response.ProjectInvitationResponse;
import com.example.task_management.entity.ProjectInvitationEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProjectInvitationConverter {
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public ProjectInvitationResponse toProjectInvitationResponse(ProjectInvitationEntity entity) {
        if (entity == null) return null;

        boolean isExpired = entity.getExpiresAt() != null &&
                entity.getExpiresAt().isBefore(LocalDateTime.now());

        return ProjectInvitationResponse.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .role(entity.getRole())
                .requiresApproval(entity.getRequiresApproval())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}

