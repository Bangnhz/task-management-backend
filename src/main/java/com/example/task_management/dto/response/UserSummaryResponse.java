package com.example.task_management.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSummaryResponse {
    private Long id;
    private String fullName;
    private String avatarUrl;
}
