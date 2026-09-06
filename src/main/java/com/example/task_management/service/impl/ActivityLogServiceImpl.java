package com.example.task_management.service.impl;
import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.response.ActivityLogResponse;
import com.example.task_management.entity.ActivityLogEntity;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.enums.EntityType;
import com.example.task_management.repository.ActivityLogRepository;
import com.example.task_management.repository.ProjectRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.repository.WorkspaceRepository;
import com.example.task_management.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityLogConverter activityLogConverter;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(ActivityLogCreateRequest request) {
        try {
            WorkspaceEntity workspace = workspaceRepository.findById(request.getWorkspaceId())
                    .orElse(null);
            if (workspace == null) {
                log.warn("Cannot create activity log: Workspace ID {} not found", request.getWorkspaceId());
                return;
            }

            ProjectEntity project = null;
            if (request.getProjectId() != null) {
                project = projectRepository.findById(request.getProjectId()).orElse(null);
            }

            UserEntity actor = userRepository.findById(request.getActorId()).orElse(null);
            if (actor == null) {
                log.warn("Cannot create activity log: Actor ID {} not found", request.getActorId());
                return;
            }

            ActivityLogEntity entity = ActivityLogEntity.builder()
                    .workspace(workspace)
                    .project(project)
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .action(request.getAction())
                    .actor(actor)
                    .details(request.getDetails())
                    .build();

            activityLogRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to save activity log: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByProject(Long projectId) {
        return activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(activityLogConverter::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByEntity(EntityType entityType, Long entityId) {
        return activityLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(activityLogConverter::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getLogsByWorkspace(Long workspaceId) {
        return activityLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(activityLogConverter::toResponse)
                .toList();
    }
}