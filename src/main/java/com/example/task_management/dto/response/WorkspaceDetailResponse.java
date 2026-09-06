package com.example.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDetailResponse {

    private Long id;
    private String name;
    private LocalDateTime createdAt;

    private List<ProjectCardResponse> projects;

    private List<WorkspaceMemberResponse> members;
}
