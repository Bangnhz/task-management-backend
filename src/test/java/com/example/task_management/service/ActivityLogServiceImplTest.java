package com.example.task_management.service;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.response.ActivityLogResponse;
import com.example.task_management.entity.ActivityLogEntity;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.EntityType;
import com.example.task_management.repository.ActivityLogRepository;
import com.example.task_management.repository.ProjectRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.repository.WorkspaceRepository;
import com.example.task_management.service.impl.ActivityLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceImplTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogConverter activityLogConverter;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    @Test
    void testLog_Success() {
        ActivityLogCreateRequest request = ActivityLogCreateRequest.builder()
                .workspaceId(10L)
                .projectId(100L)
                .actorId(1L)
                .entityType(EntityType.TASK)
                .entityId(50L)
                .action(ActivityAction.CREATED)
                .build();

        WorkspaceEntity workspace = WorkspaceEntity.builder().id(10L).build();
        ProjectEntity project = ProjectEntity.builder().id(100L).build();
        UserEntity actor = UserEntity.builder().id(1L).build();

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        activityLogService.log(request);

        ArgumentCaptor<ActivityLogEntity> captor = ArgumentCaptor.forClass(ActivityLogEntity.class);
        verify(activityLogRepository, times(1)).save(captor.capture());
        ActivityLogEntity saved = captor.getValue();
        assertEquals(workspace, saved.getWorkspace());
        assertEquals(project, saved.getProject());
        assertEquals(actor, saved.getActor());
        assertEquals(EntityType.TASK, saved.getEntityType());
        assertEquals(ActivityAction.CREATED, saved.getAction());
    }

    @Test
    void testLog_WorkspaceNotFound_DoesNotSave() {
        ActivityLogCreateRequest request = ActivityLogCreateRequest.builder()
                .workspaceId(999L)
                .build();

        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

        activityLogService.log(request);

        verify(activityLogRepository, never()).save(any());
    }

    @Test
    void testLog_ActorNotFound_DoesNotSave() {
        ActivityLogCreateRequest request = ActivityLogCreateRequest.builder()
                .workspaceId(10L)
                .actorId(999L)
                .build();

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(WorkspaceEntity.builder().id(10L).build()));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        activityLogService.log(request);

        verify(activityLogRepository, never()).save(any());
    }

    @Test
    void testGetLogsByProject_Success() {
        ActivityLogEntity log = ActivityLogEntity.builder().id(1L).build();
        ActivityLogResponse resp = ActivityLogResponse.builder().id(1L).build();

        when(activityLogRepository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(log));
        when(activityLogConverter.toResponse(log)).thenReturn(resp);

        List<ActivityLogResponse> results = activityLogService.getLogsByProject(100L);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetLogsByEntity_Success() {
        ActivityLogEntity log = ActivityLogEntity.builder().id(1L).build();
        ActivityLogResponse resp = ActivityLogResponse.builder().id(1L).build();

        when(activityLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(EntityType.TASK, 50L))
                .thenReturn(List.of(log));
        when(activityLogConverter.toResponse(log)).thenReturn(resp);

        List<ActivityLogResponse> results = activityLogService.getLogsByEntity(EntityType.TASK, 50L);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetLogsByWorkspace_Success() {
        ActivityLogEntity log = ActivityLogEntity.builder().id(1L).build();
        ActivityLogResponse resp = ActivityLogResponse.builder().id(1L).build();

        when(activityLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(log));
        when(activityLogConverter.toResponse(log)).thenReturn(resp);

        List<ActivityLogResponse> results = activityLogService.getLogsByWorkspace(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
    }
}
