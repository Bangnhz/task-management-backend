package com.example.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDetailResponse {
    private Long id;
    private String name;
    private String description;
    private Long workspaceId;
    private UserSummaryResponse owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TaskSummaryResponse> tasks;
}
