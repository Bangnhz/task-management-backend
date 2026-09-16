package com.example.task_management.service;

import com.example.task_management.converter.TaskListConverter;
import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.dto.request.TaskListUpdateRequest;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.entity.*;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectVisibility;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.*;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.TaskListServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskListServiceImplTest {

    @Mock
    private TaskListRepository taskListRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private TaskListConverter taskListConverter;

    @InjectMocks
    private TaskListServiceImpl taskListService;

    private final Long userId = 1L;
    private final Long projectId = 10L;
    private final Long workspaceId = 100L;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId, "user@example.com", "pass", "User", null, Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetTaskLists_DirectActiveProjectMember_Allowed() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).visibility(ProjectVisibility.PRIVATE).build();
        ProjectMemberEntity member = ProjectMemberEntity.builder().status(MemberStatus.ACTIVE).build();
        TaskListEntity list = TaskListEntity.builder().id(1L).title("To Do").build();
        TaskListResponse response = TaskListResponse.builder().id(1L).title("To Do").build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));
        when(taskListRepository.findByProjectIdOrderByPositionAsc(projectId)).thenReturn(List.of(list));
        when(taskListConverter.toTaskListResponse(list)).thenReturn(response);

        List<TaskListResponse> result = taskListService.getTaskListsByProjectId(projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("To Do", result.get(0).getTitle());
    }

    @Test
    void testGetTaskLists_PublicProject_Allowed() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).visibility(ProjectVisibility.PUBLIC).build();
        TaskListEntity list = TaskListEntity.builder().id(1L).title("Public List").build();
        TaskListResponse response = TaskListResponse.builder().id(1L).title("Public List").build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(taskListRepository.findByProjectIdOrderByPositionAsc(projectId)).thenReturn(List.of(list));
        when(taskListConverter.toTaskListResponse(list)).thenReturn(response);

        List<TaskListResponse> result = taskListService.getTaskListsByProjectId(projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetTaskLists_WorkspaceMemberNonGuest_Allowed() {
        WorkspaceEntity workspace = WorkspaceEntity.builder().id(workspaceId).build();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .visibility(ProjectVisibility.WORKSPACE)
                .workspace(workspace)
                .build();
        WorkspaceMemberEntity wm = WorkspaceMemberEntity.builder()
                .status(MemberStatus.ACTIVE)
                .role(WorkspaceRole.MEMBER)
                .build();

        TaskListEntity list = TaskListEntity.builder().id(1L).title("Workspace List").build();
        TaskListResponse response = TaskListResponse.builder().id(1L).title("Workspace List").build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(wm));
        when(taskListRepository.findByProjectIdOrderByPositionAsc(projectId)).thenReturn(List.of(list));
        when(taskListConverter.toTaskListResponse(list)).thenReturn(response);

        List<TaskListResponse> result = taskListService.getTaskListsByProjectId(projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetTaskLists_WorkspaceMemberGuest_Denied() {
        WorkspaceEntity workspace = WorkspaceEntity.builder().id(workspaceId).build();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .visibility(ProjectVisibility.WORKSPACE)
                .workspace(workspace)
                .build();
        WorkspaceMemberEntity wm = WorkspaceMemberEntity.builder()
                .status(MemberStatus.ACTIVE)
                .role(WorkspaceRole.GUEST)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(wm));

        assertThrows(AccessDeniedException.class, () -> taskListService.getTaskListsByProjectId(projectId));
    }

    @Test
    void testGetTaskLists_ProjectNotFound_ThrowsException() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> taskListService.getTaskListsByProjectId(projectId));
    }

    @Test
    void testCreateTaskList_Success_IsDoneTrue_ResetsOtherDoneList() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).build();
        TaskListEntity existingDoneList = TaskListEntity.builder().id(5L).isDone(true).build();

        TaskListCreateRequest request = TaskListCreateRequest.builder()
                .title("Done Column")
                .isDone(true)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskListRepository.findMaxPositionByProjectId(projectId)).thenReturn(1000.0);
        when(taskListRepository.findByProjectIdAndIsDoneTrue(projectId)).thenReturn(Optional.of(existingDoneList));

        TaskListEntity savedList = TaskListEntity.builder().id(6L).title("Done Column").isDone(true).position(2000.0).build();
        when(taskListRepository.save(any(TaskListEntity.class))).thenReturn(savedList);
        when(taskListConverter.toTaskListResponse(savedList)).thenReturn(TaskListResponse.builder().id(6L).title("Done Column").isDone(true).build());

        TaskListResponse result = taskListService.createTaskList(projectId, request);

        assertNotNull(result);
        assertTrue(result.getIsDone());
        // Verify previous done list was reset
        assertFalse(existingDoneList.getIsDone());
        verify(taskListRepository, times(1)).save(existingDoneList);
    }

    @Test
    void testUpdateTaskList_UpdateTitleAndIsDone() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).build();
        TaskListEntity listToUpdate = TaskListEntity.builder().id(1L).title("Old Title").project(project).isDone(false).build();
        TaskListEntity otherDoneList = TaskListEntity.builder().id(2L).title("Done").isDone(true).build();

        TaskListUpdateRequest request = TaskListUpdateRequest.builder()
                .title("New Title")
                .isDone(true)
                .build();

        when(taskListRepository.findById(1L)).thenReturn(Optional.of(listToUpdate));
        when(taskListRepository.findByProjectIdAndIsDoneTrueAndIdNot(projectId, 1L)).thenReturn(Optional.of(otherDoneList));
        when(taskListRepository.save(any(TaskListEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskListConverter.toTaskListResponse(listToUpdate))
                .thenReturn(TaskListResponse.builder().id(1L).title("New Title").isDone(true).build());

        TaskListResponse result = taskListService.updateTaskList(1L, request);

        assertNotNull(result);
        assertEquals("New Title", listToUpdate.getTitle());
        assertTrue(listToUpdate.getIsDone());
        assertFalse(otherDoneList.getIsDone());
        verify(taskListRepository, times(1)).save(otherDoneList);
    }

    @Test
    void testUpdateTaskList_NotFound_ThrowsException() {
        TaskListUpdateRequest request = TaskListUpdateRequest.builder().title("Title").build();
        when(taskListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskListService.updateTaskList(99L, request));
    }
}
