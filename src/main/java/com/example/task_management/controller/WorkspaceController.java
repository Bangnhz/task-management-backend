package com.example.task_management.controller;

import com.example.task_management.dto.request.ProjectCreateRequest;
import com.example.task_management.dto.request.WorkspaceCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ProjectService;
import com.example.task_management.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@CrossOrigin(origins = "*")
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@Valid @RequestBody WorkspaceCreateRequest request) {
        WorkspaceResponse createdWorkspace = workspaceService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkspace);
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces(){
        return ResponseEntity.ok(workspaceService.getAllWorkspaces());
    }

    @GetMapping("/me")
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspacesMe() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(workspaceService.getWorkspacesByUserId(userId));
    }

    @GetMapping("/{workspaceId}/projects")
    public ResponseEntity<?> getProjectsByWorkspaceId(
            @PathVariable("workspaceId") Long workspaceId) {
        List<ProjectCardResponse> summaries = projectService.getProjectSummariesByWorkspaceId(workspaceId);
        return ResponseEntity.ok(summaries);
    }

    @PostMapping("/{workspaceId}/projects")
    public ResponseEntity<ProjectCardResponse> createProject(
            @PathVariable("workspaceId") Long workspaceId,
            @RequestBody ProjectCreateRequest request) {
        ProjectCardResponse projectCardResponse = projectService.createProject(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(projectCardResponse);
    }

}
