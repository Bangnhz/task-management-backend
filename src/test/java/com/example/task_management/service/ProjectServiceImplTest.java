package com.example.task_management.service;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.ProjectConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.request.ProjectCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.ProjectMemberEntity;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.enums.ProjectVisibility;
import com.example.task_management.repository.ProjectMemberRepository;
import com.example.task_management.repository.ProjectRepository;
import com.example.task_management.repository.TaskListRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.repository.WorkspaceRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectConverter projectConverter;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TaskListRepository taskListRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private ActivityLogConverter activityLogConverter;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private final Long userId = 1L;
    private final Long workspaceId = 10L;
    private final Long projectId = 100L;

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

    @Nested
    @DisplayName("getProjectSummariesByUserId Tests")
    class GetProjectSummariesByUserIdTests {

        @Test
        @DisplayName("Should return paged project summaries when userId is valid and has projects")
        void testGetProjectSummariesByUserId_Success() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
            ProjectCardResponse card1 = ProjectCardResponse.builder()
                    .id(projectId)
                    .name("Test Project 1")
                    .totalTasks(5L)
                    .visibility(ProjectVisibility.WORKSPACE)
                    .build();
            ProjectCardResponse card2 = ProjectCardResponse.builder()
                    .id(101L)
                    .name("Test Project 2")
                    .totalTasks(12L)
                    .visibility(ProjectVisibility.PRIVATE)
                    .build();
            Page<ProjectCardResponse> page = new PageImpl<>(List.of(card1, card2), pageable, 2);

            when(projectRepository.getProjectSummariesByUserId(userId, pageable)).thenReturn(page);

            Page<ProjectCardResponse> result = projectService.getProjectSummariesByUserId(userId, pageable);

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            assertEquals("Test Project 1", result.getContent().get(0).getName());
            assertEquals("Test Project 2", result.getContent().get(1).getName());
            verify(projectRepository, times(1)).getProjectSummariesByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Should return empty page when user has no projects")
        void testGetProjectSummariesByUserId_EmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProjectCardResponse> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(projectRepository.getProjectSummariesByUserId(userId, pageable)).thenReturn(emptyPage);

            Page<ProjectCardResponse> result = projectService.getProjectSummariesByUserId(userId, pageable);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
            verify(projectRepository, times(1)).getProjectSummariesByUserId(userId, pageable);
        }

        @Test
        @DisplayName("Should throw RuntimeException when userId is null")
        void testGetProjectSummariesByUserId_NullUserId_ThrowsException() {
            Pageable pageable = PageRequest.of(0, 10);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> projectService.getProjectSummariesByUserId(null, pageable));

            assertEquals("UserId is null", exception.getMessage());
            verify(projectRepository, never()).getProjectSummariesByUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("getProjectsByWorkspaceId Tests")
    class GetProjectsByWorkspaceIdTests {

        @Test
        @DisplayName("Should return list of projects converted to ProjectResponse")
        void testGetProjectsByWorkspaceId_Success() {
            ProjectEntity project1 = ProjectEntity.builder().id(projectId).title("Project 1").build();
            ProjectEntity project2 = ProjectEntity.builder().id(101L).title("Project 2").build();
            ProjectResponse response1 = ProjectResponse.builder().id(projectId).title("Project 1").build();
            ProjectResponse response2 = ProjectResponse.builder().id(101L).title("Project 2").build();

            when(projectRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(project1, project2));
            when(projectConverter.toProjectResponse(project1)).thenReturn(response1);
            when(projectConverter.toProjectResponse(project2)).thenReturn(response2);

            List<ProjectResponse> result = projectService.getProjectsByWorkspaceId(workspaceId);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(projectId, result.get(0).getId());
            assertEquals(101L, result.get(1).getId());
            verify(projectRepository, times(1)).findByWorkspaceId(workspaceId);
            verify(projectConverter, times(1)).toProjectResponse(project1);
            verify(projectConverter, times(1)).toProjectResponse(project2);
        }

        @Test
        @DisplayName("Should return empty list when workspace has no projects")
        void testGetProjectsByWorkspaceId_EmptyList() {
            when(projectRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());

            List<ProjectResponse> result = projectService.getProjectsByWorkspaceId(workspaceId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(projectRepository, times(1)).findByWorkspaceId(workspaceId);
            verify(projectConverter, never()).toProjectResponse(any());
        }
    }

    @Nested
    @DisplayName("getProjectSummariesByWorkspaceId Tests")
    class GetProjectSummariesByWorkspaceIdTests {

        @Test
        @DisplayName("Should return summaries for workspace using current authenticated userId")
        void testGetProjectSummariesByWorkspaceId_Success() {
            ProjectCardResponse card = ProjectCardResponse.builder().id(projectId).name("P1").build();
            when(projectRepository.getProjectSummariesByWorkspaceId(workspaceId, userId)).thenReturn(List.of(card));

            List<ProjectCardResponse> result = projectService.getProjectSummariesByWorkspaceId(workspaceId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("P1", result.get(0).getName());
            verify(projectRepository, times(1)).getProjectSummariesByWorkspaceId(workspaceId, userId);
        }

        @Test
        @DisplayName("Should return empty list when no summaries found for workspace")
        void testGetProjectSummariesByWorkspaceId_EmptyList() {
            when(projectRepository.getProjectSummariesByWorkspaceId(workspaceId, userId)).thenReturn(Collections.emptyList());

            List<ProjectCardResponse> result = projectService.getProjectSummariesByWorkspaceId(workspaceId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(projectRepository, times(1)).getProjectSummariesByWorkspaceId(workspaceId, userId);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when user is unauthenticated")
        void testGetProjectSummariesByWorkspaceId_Unauthenticated_ThrowsException() {
            SecurityContextHolder.clearContext();

            assertThrows(IllegalStateException.class,
                    () -> projectService.getProjectSummariesByWorkspaceId(workspaceId));

            verify(projectRepository, never()).getProjectSummariesByWorkspaceId(any(), any());
        }
    }

    @Nested
    @DisplayName("createProject Tests")
    class CreateProjectTests {

        @Test
        @DisplayName("Should create project, 4 default task lists, owner member, and activity log successfully")
        void testCreateProject_Success_WithAuthenticatedUser() {
            Long customUserId = 42L;
            CustomUserDetails customUserDetails = new CustomUserDetails(
                    customUserId, "custom@example.com", "pass", "Custom User", null, Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities())
            );

            ProjectCreateRequest request = ProjectCreateRequest.builder()
                    .title("New Enterprise Project")
                    .visibility(ProjectVisibility.WORKSPACE)
                    .build();

            WorkspaceEntity workspace = WorkspaceEntity.builder().id(workspaceId).name("Engineering Workspace").build();
            UserEntity user = UserEntity.builder().id(customUserId).fullName("Custom User").build();

            ProjectEntity savedProject = ProjectEntity.builder()
                    .id(projectId)
                    .title("New Enterprise Project")
                    .visibility(ProjectVisibility.WORKSPACE)
                    .workspace(workspace)
                    .build();

            ProjectCardResponse cardResponse = ProjectCardResponse.builder()
                    .id(projectId)
                    .name("New Enterprise Project")
                    .totalTasks(0L)
                    .visibility(ProjectVisibility.WORKSPACE)
                    .build();

            ActivityLogCreateRequest logRequest = ActivityLogCreateRequest.builder().build();

            when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
            when(projectRepository.save(any(ProjectEntity.class))).thenReturn(savedProject);
            when(userRepository.findById(customUserId)).thenReturn(Optional.of(user));
            when(activityLogConverter.toProjectLogRequest(savedProject, ActivityAction.CREATED, customUserId)).thenReturn(logRequest);
            when(projectConverter.toProjectCardResponse(savedProject)).thenReturn(cardResponse);

            ProjectCardResponse result = projectService.createProject(workspaceId, request);

            assertNotNull(result);
            assertEquals(projectId, result.getId());
            assertEquals("New Enterprise Project", result.getName());

            // 1. Verify ProjectEntity save details
            ArgumentCaptor<ProjectEntity> projectCaptor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectRepository, times(1)).save(projectCaptor.capture());
            ProjectEntity capturedProject = projectCaptor.getValue();
            assertEquals("New Enterprise Project", capturedProject.getTitle());
            assertEquals(ProjectVisibility.WORKSPACE, capturedProject.getVisibility());
            assertEquals(workspace, capturedProject.getWorkspace());

            // 2. Verify 4 default task lists creation with exact titles and positions
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TaskListEntity>> listsCaptor = ArgumentCaptor.forClass(List.class);
            verify(taskListRepository, times(1)).saveAll(listsCaptor.capture());
            List<TaskListEntity> savedLists = listsCaptor.getValue();
            assertEquals(4, savedLists.size());

            assertEquals("Backlog", savedLists.get(0).getTitle());
            assertEquals(65536.0 * 1, savedLists.get(0).getPosition(), 0.001);
            assertEquals(savedProject, savedLists.get(0).getProject());
            assertNotNull(savedLists.get(0).getTasks());

            assertEquals("To Do", savedLists.get(1).getTitle());
            assertEquals(65536.0 * 2, savedLists.get(1).getPosition(), 0.001);
            assertEquals(savedProject, savedLists.get(1).getProject());

            assertEquals("In Progress", savedLists.get(2).getTitle());
            assertEquals(65536.0 * 3, savedLists.get(2).getPosition(), 0.001);
            assertEquals(savedProject, savedLists.get(2).getProject());

            assertEquals("Done", savedLists.get(3).getTitle());
            assertEquals(65536.0 * 4, savedLists.get(3).getPosition(), 0.001);
            assertEquals(savedProject, savedLists.get(3).getProject());

            // 3. Verify ProjectMember creation as OWNER and ACTIVE
            ArgumentCaptor<ProjectMemberEntity> memberCaptor = ArgumentCaptor.forClass(ProjectMemberEntity.class);
            verify(projectMemberRepository, times(1)).save(memberCaptor.capture());
            ProjectMemberEntity savedMember = memberCaptor.getValue();
            assertEquals(ProjectRole.OWNER, savedMember.getRole());
            assertEquals(MemberStatus.ACTIVE, savedMember.getStatus());
            assertEquals(user, savedMember.getUser());
            assertEquals(savedProject, savedMember.getProject());

            // 4. Verify ActivityLog call
            verify(activityLogConverter, times(1)).toProjectLogRequest(savedProject, ActivityAction.CREATED, customUserId);
            verify(activityLogService, times(1)).log(logRequest);

            // 5. Verify Converter call
            verify(projectConverter, times(1)).toProjectCardResponse(savedProject);
        }

        @Test
        @DisplayName("Should fallback to userId = 1L when SecurityUtils returns null")
        void testCreateProject_Success_WhenSecurityContextUserIsNull_FallbackToUserId1() {
            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(null);

                ProjectCreateRequest request = ProjectCreateRequest.builder()
                        .title("Fallback User Project")
                        .visibility(ProjectVisibility.PUBLIC)
                        .build();

                WorkspaceEntity workspace = WorkspaceEntity.builder().id(workspaceId).build();
                UserEntity fallbackUser = UserEntity.builder().id(1L).fullName("Default Admin").build();

                ProjectEntity savedProject = ProjectEntity.builder()
                        .id(projectId)
                        .title("Fallback User Project")
                        .visibility(ProjectVisibility.PUBLIC)
                        .workspace(workspace)
                        .build();

                ProjectCardResponse cardResponse = ProjectCardResponse.builder().id(projectId).build();
                ActivityLogCreateRequest logRequest = ActivityLogCreateRequest.builder().build();

                when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
                when(projectRepository.save(any(ProjectEntity.class))).thenReturn(savedProject);
                when(userRepository.findById(1L)).thenReturn(Optional.of(fallbackUser));
                when(activityLogConverter.toProjectLogRequest(savedProject, ActivityAction.CREATED, 1L)).thenReturn(logRequest);
                when(projectConverter.toProjectCardResponse(savedProject)).thenReturn(cardResponse);

                ProjectCardResponse result = projectService.createProject(workspaceId, request);

                assertNotNull(result);
                assertEquals(projectId, result.getId());

                verify(userRepository, times(1)).findById(1L);
                verify(activityLogConverter, times(1)).toProjectLogRequest(savedProject, ActivityAction.CREATED, 1L);
                verify(activityLogService, times(1)).log(logRequest);
            }
        }

        @Test
        @DisplayName("Should throw IllegalStateException when unauthenticated")
        void testCreateProject_Unauthenticated_ThrowsException() {
            SecurityContextHolder.clearContext();
            ProjectCreateRequest request = ProjectCreateRequest.builder().title("Unauth").build();

            assertThrows(IllegalStateException.class,
                    () -> projectService.createProject(workspaceId, request));

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when workspace is not found")
        void testCreateProject_WorkspaceNotFound_ThrowsException() {
            ProjectCreateRequest request = ProjectCreateRequest.builder().title("Missing Workspace Project").build();
            when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> projectService.createProject(workspaceId, request));

            assertEquals("Workspace not found", exception.getMessage());
            verify(projectRepository, never()).save(any());
            verify(taskListRepository, never()).saveAll(any());
            verify(projectMemberRepository, never()).save(any());
            verify(activityLogService, never()).log(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when user is not found")
        void testCreateProject_UserNotFound_ThrowsException() {
            ProjectCreateRequest request = ProjectCreateRequest.builder().title("P").build();
            WorkspaceEntity workspace = WorkspaceEntity.builder().id(workspaceId).build();
            ProjectEntity savedProject = ProjectEntity.builder().id(projectId).workspace(workspace).build();

            when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
            when(projectRepository.save(any(ProjectEntity.class))).thenReturn(savedProject);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> projectService.createProject(workspaceId, request));

            assertEquals("User not found!", exception.getMessage());
            verify(projectMemberRepository, never()).save(any());
            verify(activityLogService, never()).log(any());
        }
    }

    @Nested
    @DisplayName("getProjectById Tests")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return ProjectResponse when project exists")
        void testGetProjectById_Success() {
            ProjectEntity project = ProjectEntity.builder()
                    .id(projectId)
                    .title("Design System")
                    .visibility(ProjectVisibility.WORKSPACE)
                    .createdAt(LocalDateTime.now())
                    .build();

            ProjectResponse response = ProjectResponse.builder()
                    .id(projectId)
                    .title("Design System")
                    .visibility(ProjectVisibility.WORKSPACE)
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(projectConverter.toProjectResponse(project)).thenReturn(response);

            ProjectResponse result = projectService.getProjectById(projectId);

            assertNotNull(result);
            assertEquals(projectId, result.getId());
            assertEquals("Design System", result.getTitle());
            assertEquals(ProjectVisibility.WORKSPACE, result.getVisibility());
            verify(projectRepository, times(1)).findById(projectId);
            verify(projectConverter, times(1)).toProjectResponse(project);
        }

        @Test
        @DisplayName("Should throw RuntimeException when project does not exist")
        void testGetProjectById_NotFound_ThrowsException() {
            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> projectService.getProjectById(projectId));

            assertEquals("Project not found with id: " + projectId, exception.getMessage());
            verify(projectRepository, times(1)).findById(projectId);
            verify(projectConverter, never()).toProjectResponse(any());
        }
    }
}
