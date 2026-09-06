package com.example.task_management.converter;

import com.example.task_management.dto.response.AttachmentResponse;
import com.example.task_management.entity.AttachmentEntity;
import com.example.task_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AttachmentConverter {
    @Autowired
    private UserConverter userConverter;
    @Autowired
    private UserRepository userRepository;

    public AttachmentResponse toAttachmentResponse(AttachmentEntity attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .fileSize(attachment.getFileSize())
                .uploadedBy(userConverter.toUserSummaryResponse(attachment.getUploadedBy()))
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}

