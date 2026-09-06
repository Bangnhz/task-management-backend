package com.example.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentMentionResponse {
    private UserSummaryResponse user;
    private Integer startIndex;
    private Integer length;
}
