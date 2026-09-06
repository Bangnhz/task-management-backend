package com.example.task_management.dto.response;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCardResponse {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private UserSummaryResponse assignee;
    private Integer commentCount;
    private Instant createdAt;
    private Instant updatedAt;
}
