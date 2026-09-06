package com.example.task_management.converter;

import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.entity.ProjectMemberEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberConverter {
    public ProjectMemberResponse toProjectMemberResponse(ProjectMemberEntity entity) {
        if (entity == null) return null;

        return ProjectMemberResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .userId(entity.getUser().getId())
                .fullName(entity.getUser().getFullName())
                .email(entity.getUser().getEmail())
                .avatarUrl(entity.getUser().getAvatarUrl())
                .role(entity.getRole())
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}

