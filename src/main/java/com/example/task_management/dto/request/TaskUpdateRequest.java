package com.example.task_management.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateRequest {
    private String title;
    private String description;
    private String priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Long assigneeId;
    private Long listId;
}
