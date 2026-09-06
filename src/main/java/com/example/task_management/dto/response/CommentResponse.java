package com.example.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummaryResponse author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AttachmentResponse> attachments;
    private List<CommentMentionResponse> mentions;
}
