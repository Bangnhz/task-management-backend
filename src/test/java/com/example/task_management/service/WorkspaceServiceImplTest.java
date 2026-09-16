package com.example.task_management.service;

import com.example.task_management.converter.WorkspaceConverter;
import com.example.task_management.dto.request.WorkspaceCreateRequest;
import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.entity.WorkspaceMemberEntity;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.repository.WorkspaceMemberRepository;
import com.example.task_management.repository.WorkspaceRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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

        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "pass", "User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateWorkspace_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workspaceRepository.save(any(WorkspaceEntity.class))).thenReturn(workspaceEntity);
        when(workspaceMemberRepository.save(any(WorkspaceMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceConverter.toWorkspaceResponse(workspaceEntity)).thenReturn(responseDTO);

        WorkspaceResponse result = workspaceService.createWorkspace(createRequest);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Test Workspace", result.getName());
        verify(workspaceRepository, times(1)).save(any(WorkspaceEntity.class));

        ArgumentCaptor<WorkspaceMemberEntity> memberCaptor = ArgumentCaptor.forClass(WorkspaceMemberEntity.class);
        verify(workspaceMemberRepository, times(1)).save(memberCaptor.capture());
        assertEquals(WorkspaceRole.OWNER, memberCaptor.getValue().getRole());
        assertEquals(user, memberCaptor.getValue().getUser());
    }

    @Test
    void testCreateWorkspace_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> workspaceService.createWorkspace(createRequest));
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testGetAllWorkspaces_Success() {
        when(workspaceRepository.findAll()).thenReturn(List.of(workspaceEntity));
        when(workspaceConverter.toWorkspaceResponse(workspaceEntity)).thenReturn(responseDTO);

        List<WorkspaceResponse> results = workspaceService.getAllWorkspaces();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getId());
    }

    @Test
    void testGetWorkspacesByUserId_Success() {
        when(workspaceRepository.findByUserId(1L)).thenReturn(List.of(workspaceEntity));
        when(workspaceConverter.toWorkspaceResponse(workspaceEntity)).thenReturn(responseDTO);

        List<WorkspaceResponse> results = workspaceService.getWorkspacesByUserId(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Workspace", results.get(0).getName());
    }
}
