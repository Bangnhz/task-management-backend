package com.example.task_management.converter;

import com.example.task_management.dto.response.NotificationResponse;
import com.example.task_management.entity.NotificationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {

    @Autowired
    private UserConverter userConverter;

    public NotificationResponse toResponse(NotificationEntity entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .id(entity.getId())
                .actor(entity.getActor() != null ? userConverter.toUserSummaryResponse(entity.getActor()) : null)
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .targetId(entity.getTargetId())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}