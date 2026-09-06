package com.example.task_management.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentMentionRequest {

    private Long userId;
    private Integer startIndex;
    private Integer length;
}
