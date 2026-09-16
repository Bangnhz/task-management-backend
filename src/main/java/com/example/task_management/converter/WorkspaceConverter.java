package com.example.task_management.converter;

import com.example.task_management.dto.response.WorkspaceMemberResponse;
import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.entity.WorkspaceMemberEntity;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceConverter {

    public WorkspaceResponse toWorkspaceResponse(WorkspaceEntity entity) {
        if (entity == null) return null;
        return WorkspaceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public WorkspaceMemberResponse toWorkspaceMemberResponse(WorkspaceMemberEntity entity) {
        if (entity == null) return null;
        return WorkspaceMemberResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .fullName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .email(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .avatarUrl(entity.getUser() != null ? entity.getUser().getAvatarUrl() : null)
                .role(entity.getRole())
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}

