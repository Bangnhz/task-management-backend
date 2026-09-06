package com.example.task_management.controller;

import com.example.task_management.dto.response.ActivityLogResponse;
import com.example.task_management.enums.EntityType;
import com.example.task_management.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping("/projects/{projectId}")
    // @PreAuthorize("@projectSecurity.isMember(#p0)")
    public ResponseEntity<List<ActivityLogResponse>> getLogsByProject(
            @PathVariable("projectId") Long projectId) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByProject(projectId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entities/{entityType}/{entityId}")
    public ResponseEntity<List<ActivityLogResponse>> getLogsByEntity(
            @PathVariable("entityType") EntityType entityType,
            @PathVariable("entityId") Long entityId) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/workspaces/{workspaceId}")
    public ResponseEntity<List<ActivityLogResponse>> getLogsByWorkspace(
            @PathVariable("workspaceId") Long workspaceId) {
        List<ActivityLogResponse> logs = activityLogService.getLogsByWorkspace(workspaceId);
        return ResponseEntity.ok(logs);
    }
}