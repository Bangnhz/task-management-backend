package com.example.task_management.controller;

import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.enums.ProjectVisibility;
import com.example.task_management.exception.GlobalExceptionHandler;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.ProjectService;
import com.example.task_management.service.TaskListService;
import com.example.task_management.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskListService taskListService;

    @InjectMocks
    private ProjectController projectController;

    private ObjectMapper objectMapper;

    private final Long currentUserId = 1L;
    private final Long projectId = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        CustomUserDetails userDetails = new CustomUserDetails(
                currentUserId, "test@example.com", "pass", "User", null, Collections.emptyList()
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
    @DisplayName("GET /api/projects/me Tests")
    class GetMyProjectsTests {

        @Test
        @DisplayName("Should return paged projects with default query parameters (updatedAt desc, page=0, size=10)")
        void testGetMyProjects_DefaultParams_Success() throws Exception {
            ProjectCardResponse card = ProjectCardResponse.builder()
                    .id(projectId)
                    .name("Project Alpha")
                    .totalTasks(8L)
                    .visibility(ProjectVisibility.WORKSPACE)
                    .build();

            Page<ProjectCardResponse> pageResult = new PageImpl<>(
                    List.of(card), PageRequest.of(0, 10, Sort.by("updatedAt").descending()), 1
            );

            when(projectService.getProjectSummariesByUserId(eq(currentUserId), any(Pageable.class)))
                    .thenReturn(pageResult);

            mockMvc.perform(get("/api/projects/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(projectId))
                    .andExpect(jsonPath("$.content[0].name").value("Project Alpha"))
                    .andExpect(jsonPath("$.content[0].totalTasks").value(8))
                    .andExpect(jsonPath("$.content[0].visibility").value("WORKSPACE"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectService, times(1)).getProjectSummariesByUserId(eq(currentUserId), pageableCaptor.capture());
            Pageable captured = pageableCaptor.getValue();
            assertEquals(0, captured.getPageNumber());
            assertEquals(10, captured.getPageSize());
            assertEquals(Sort.by("updatedAt").descending(), captured.getSort());
        }

        @Test
        @DisplayName("Should parse custom pagination and ascending sort parameters correctly")
        void testGetMyProjects_CustomParams_Ascending_Success() throws Exception {
            ProjectCardResponse card1 = ProjectCardResponse.builder().id(1L).name("A").build();
            ProjectCardResponse card2 = ProjectCardResponse.builder().id(2L).name("B").build();

            Page<ProjectCardResponse> pageResult = new PageImpl<>(
                    List.of(card1, card2), PageRequest.of(1, 5, Sort.by("title").ascending()), 2
            );

            when(projectService.getProjectSummariesByUserId(eq(currentUserId), any(Pageable.class)))
                    .thenReturn(pageResult);

            mockMvc.perform(get("/api/projects/me")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sortBy", "title")
                            .param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].name").value("A"))
                    .andExpect(jsonPath("$.content[1].name").value("B"));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectService, times(1)).getProjectSummariesByUserId(eq(currentUserId), pageableCaptor.capture());
            Pageable captured = pageableCaptor.getValue();
            assertEquals(1, captured.getPageNumber());
            assertEquals(5, captured.getPageSize());
            assertEquals(Sort.by("title").ascending(), captured.getSort());
        }

        @Test
        @DisplayName("Should return empty list in content when user has no projects")
        void testGetMyProjects_EmptyPage() throws Exception {
            Page<ProjectCardResponse> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0
            );

            when(projectService.getProjectSummariesByUserId(eq(currentUserId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/projects/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/projects/{projectId} Tests")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return 200 OK and project details when project exists")
        void testGetProjectById_Success() throws Exception {
            ProjectResponse response = ProjectResponse.builder()
                    .id(projectId)
                    .title("Alpha Project")
                    .visibility(ProjectVisibility.WORKSPACE)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(projectService.getProjectById(projectId)).thenReturn(response);

            mockMvc.perform(get("/api/projects/{projectId}", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(projectId))
                    .andExpect(jsonPath("$.title").value("Alpha Project"))
                    .andExpect(jsonPath("$.visibility").value("WORKSPACE"));

            verify(projectService, times(1)).getProjectById(projectId);
        }

        @Test
        @DisplayName("Should return 400 Bad Request via GlobalExceptionHandler when project is not found")
        void testGetProjectById_NotFound_ReturnsBadRequest() throws Exception {
            when(projectService.getProjectById(999L))
                    .thenThrow(new RuntimeException("Project not found with id: 999"));

            mockMvc.perform(get("/api/projects/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Project not found with id: 999"));

            verify(projectService, times(1)).getProjectById(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/projects/{projectId}/tasks Tests")
    class GetTasksByProjectIdTests {

        @Test
        @DisplayName("Should return list of task cards for project")
        void testGetTasksByProjectId_Success() throws Exception {
            TaskCardResponse task1 = TaskCardResponse.builder().id(101L).title("Task 1").priority("HIGH").build();
            TaskCardResponse task2 = TaskCardResponse.builder().id(102L).title("Task 2").priority("MEDIUM").build();

            when(taskService.getTasksByProjectId(projectId)).thenReturn(List.of(task1, task2));

            mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(101))
                    .andExpect(jsonPath("$[0].title").value("Task 1"))
                    .andExpect(jsonPath("$[1].id").value(102))
                    .andExpect(jsonPath("$[1].title").value("Task 2"));

            verify(taskService, times(1)).getTasksByProjectId(projectId);
        }

        @Test
        @DisplayName("Should return empty list when project has no tasks")
        void testGetTasksByProjectId_EmptyList() throws Exception {
            when(taskService.getTasksByProjectId(projectId)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(taskService, times(1)).getTasksByProjectId(projectId);
        }
    }

    @Nested
    @DisplayName("GET /api/projects/{projectId}/task-lists Tests")
    class GetTaskListsByProjectIdTests {

        @Test
        @DisplayName("Should return task lists for project")
        void testGetTaskListsByProjectId_Success() throws Exception {
            TaskListResponse list1 = TaskListResponse.builder().id(1L).title("Backlog").position(65536.0).build();
            TaskListResponse list2 = TaskListResponse.builder().id(2L).title("To Do").position(131072.0).build();

            when(taskListService.getTaskListsByProjectId(projectId)).thenReturn(List.of(list1, list2));

            mockMvc.perform(get("/api/projects/{projectId}/task-lists", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Backlog"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].title").value("To Do"));

            verify(taskListService, times(1)).getTaskListsByProjectId(projectId);
        }

        @Test
        @DisplayName("Should return empty list when project has no task lists")
        void testGetTaskListsByProjectId_EmptyList() throws Exception {
            when(taskListService.getTaskListsByProjectId(projectId)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/projects/{projectId}/task-lists", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(taskListService, times(1)).getTaskListsByProjectId(projectId);
        }
    }

    @Nested
    @DisplayName("POST /api/projects/{projectId}/task-lists Tests")
    class CreateTaskListTests {

        @Test
        @DisplayName("Should return 201 Created and new task list response when payload is valid")
        void testCreateTaskList_Success() throws Exception {
            TaskListCreateRequest request = TaskListCreateRequest.builder()
                    .title("In Review")
                    .isDone(false)
                    .build();

            TaskListResponse response = TaskListResponse.builder()
                    .id(5L)
                    .title("In Review")
                    .position(196608.0)
                    .isDone(false)
                    .build();

            when(taskListService.createTaskList(eq(projectId), any(TaskListCreateRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/projects/{projectId}/task-lists", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.title").value("In Review"))
                    .andExpect(jsonPath("$.position").value(196608.0))
                    .andExpect(jsonPath("$.isDone").value(false));

            verify(taskListService, times(1)).createTaskList(eq(projectId), any(TaskListCreateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when title is blank")
        void testCreateTaskList_ValidationError_BlankTitle() throws Exception {
            TaskListCreateRequest request = TaskListCreateRequest.builder()
                    .title("")
                    .isDone(false)
                    .build();

            mockMvc.perform(post("/api/projects/{projectId}/task-lists", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details.title").exists());

            verify(taskListService, never()).createTaskList(any(), any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when title exceeds 100 characters")
        void testCreateTaskList_ValidationError_TitleTooLong() throws Exception {
            String longTitle = "A".repeat(101);
            TaskListCreateRequest request = TaskListCreateRequest.builder()
                    .title(longTitle)
                    .isDone(false)
                    .build();

            mockMvc.perform(post("/api/projects/{projectId}/task-lists", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Error"))
                    .andExpect(jsonPath("$.details.title").exists());

            verify(taskListService, never()).createTaskList(any(), any());
        }
    }
}
