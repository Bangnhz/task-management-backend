package com.example.task_management.service;

import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.response.ActivityLogResponse;
import com.example.task_management.enums.EntityType;
import java.util.List;
public interface ActivityLogService {
    void log(ActivityLogCreateRequest request);

    List<ActivityLogResponse> getLogsByProject(Long projectId);

    List<ActivityLogResponse> getLogsByEntity(EntityType entityType, Long entityId);

    List<ActivityLogResponse> getLogsByWorkspace(Long workspaceId);
}
