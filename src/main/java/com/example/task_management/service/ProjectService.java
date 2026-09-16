package com.example.task_management.service;

import com.example.task_management.dto.request.ProjectCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectService {
    Page<ProjectCardResponse> getProjectSummariesByUserId(Long userId, Pageable pageable);
    List<ProjectResponse> getProjectsByWorkspaceId(Long workspaceId);
    List<ProjectCardResponse> getProjectSummariesByWorkspaceId(Long workspaceId);
    ProjectCardResponse createProject(Long workspaceId, ProjectCreateRequest projectCreateRequest);
    ProjectResponse getProjectById(Long projectId);
}

