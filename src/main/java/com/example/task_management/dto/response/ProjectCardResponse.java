package com.example.task_management.dto.response;

import com.example.task_management.enums.ProjectVisibility;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCardResponse {
    private Long id;
    private String name;
    private Long totalTasks;
    private ProjectVisibility visibility;
    private LocalDateTime updatedAt;
}
