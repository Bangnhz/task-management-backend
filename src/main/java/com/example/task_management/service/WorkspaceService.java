package com.example.task_management.service;

import com.example.task_management.dto.request.WorkspaceCreateRequest;
import com.example.task_management.dto.response.WorkspaceResponse;

import java.util.List;

public interface WorkspaceService {
    List<WorkspaceResponse> getAllWorkspaces();
    List<WorkspaceResponse> getWorkspacesByUserId(Long userId);
    WorkspaceResponse createWorkspace(WorkspaceCreateRequest request);
}


