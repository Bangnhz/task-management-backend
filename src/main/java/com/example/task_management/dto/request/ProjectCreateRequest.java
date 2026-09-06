package com.example.task_management.dto.request;

import com.example.task_management.enums.ProjectVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCreateRequest {
    private String title;

    @Builder.Default
    private ProjectVisibility visibility = ProjectVisibility.WORKSPACE;
}
