package com.example.task_management.converter;

import com.example.task_management.dto.response.CommentMentionResponse;
import com.example.task_management.dto.response.CommentResponse;
import com.example.task_management.entity.CommentEntity;
import com.example.task_management.entity.CommentMentionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class CommentConverter {

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private AttachmentConverter attachmentConverter;

    public CommentResponse toCommentResponse(CommentEntity commentEntity) {

        return CommentResponse.builder()
                .id(commentEntity.getId())
                .content(commentEntity.getContent())

                .author(
                        userConverter.toUserSummaryResponse(
                                commentEntity.getAuthor()
                        )
                )

                .createdAt(commentEntity.getCreatedAt())
                .updatedAt(commentEntity.getUpdatedAt())

                .attachments(
                        commentEntity.getAttachments() == null
                                ? Collections.emptyList()
                                : commentEntity.getAttachments()
                                .stream()
                                .map(attachmentConverter::toAttachmentResponse)
                                .toList()
                )

                .mentions(
                        commentEntity.getCommentMentions() == null
                                ? Collections.emptyList()
                                : commentEntity.getCommentMentions()
                                .stream()
                                .map(this::toCommentMentionResponse)
                                .toList()
                )

                .build();
    }

    private CommentMentionResponse toCommentMentionResponse(
            CommentMentionEntity entity
    ) {
        return CommentMentionResponse.builder()
                .user(
                        userConverter.toUserSummaryResponse(
                                entity.getUser()
                        )
                )
                .startIndex(entity.getStartIndex())
                .length(entity.getLength())
                .build();
    }
}