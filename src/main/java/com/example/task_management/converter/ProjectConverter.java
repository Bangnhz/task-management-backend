package com.example.task_management.converter;

import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import com.example.task_management.entity.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectConverter {

    public ProjectResponse toProjectResponse(ProjectEntity entity) {
        if (entity == null) return null;
        return ProjectResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .visibility(entity.getVisibility())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ProjectCardResponse toProjectCardResponse(ProjectEntity entity) {
        long totalTasks = entity.getTaskLists() == null ? 0 :
                entity.getTaskLists().stream()
                        .mapToLong(list -> list.getTasks() == null ? 0 : list.getTasks().size())
                        .sum();

        return ProjectCardResponse.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .totalTasks(totalTasks)
                .visibility(entity.getVisibility())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

