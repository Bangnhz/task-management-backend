package com.example.task_management.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMentionResponse {
    private Long id;
    private String fullName;
    private String email;
    private String avatarUrl;
}
