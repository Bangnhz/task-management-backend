package com.example.task_management.dto.response;

import com.example.task_management.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private UserSummaryResponse actor;
    private String title;
    private String content;
    private NotificationType type;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}