package com.example.task_management.service;

import com.example.task_management.converter.WorkspaceConverter;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.entity.WorkspaceMemberEntity;
import com.example.task_management.dto.request.WorkspaceCreateRequest;
import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.repository.WorkspaceMemberRepository;
import com.example.task_management.repository.WorkspaceRepository;
import com.example.task_management.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceConverter workspaceConverter;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private UserEntity user;
    private WorkspaceCreateRequest createRequest;
    private WorkspaceEntity workspaceEntity;
    private WorkspaceResponse responseDTO;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(1L).email("test@example.com").build();
        createRequest = WorkspaceCreateRequest.builder().name("Test Workspace").build();
        workspaceEntity = WorkspaceEntity.builder().id(10L).name("Test Workspace").createdAt(LocalDateTime.now()).build();
        responseDTO = WorkspaceResponse.builder().id(10L).name("Test Workspace").createdAt(workspaceEntity.getCreatedAt()).build();
    }

    @Test
    void testCreateWorkspace_Success() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(workspaceRepository.save(any(WorkspaceEntity.class))).thenReturn(workspaceEntity);
        when(workspaceMemberRepository.save(any(WorkspaceMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceConverter.toWorkspaceResponse(workspaceEntity)).thenReturn(responseDTO);

        WorkspaceResponse result = workspaceService.createWorkspace(createRequest);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Test Workspace", result.getName());
        verify(workspaceRepository, times(1)).save(any(WorkspaceEntity.class));
        verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMemberEntity.class));
    }
}
