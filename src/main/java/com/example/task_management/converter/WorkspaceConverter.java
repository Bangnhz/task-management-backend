package com.example.task_management.converter;

import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.entity.WorkspaceEntity;
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
}

