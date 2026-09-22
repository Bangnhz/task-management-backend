package com.example.task_management.dto.response;

import com.example.task_management.enums.TaskPriority;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSummaryResponse {
    private Long id;
    private String title;
    private Long projectId;

    private TaskPriority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String listTitle;
    private String projectTitle;
}
