package com.example.task_management.controller;

import com.example.task_management.dto.request.ProjectCreateRequest;
import com.example.task_management.dto.request.WorkspaceCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.exception.GlobalExceptionHandler;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.ProjectService;
import com.example.task_management.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private WorkspaceController workspaceController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workspaceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "user@example.com", "pass", "User", null, Collections.emptyList()
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
    void testCreateWorkspace_Success() throws Exception {
        WorkspaceCreateRequest request = WorkspaceCreateRequest.builder().name("My Workspace").build();
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("My Workspace").build();

        when(workspaceService.createWorkspace(any(WorkspaceCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("My Workspace"));
    }

    @Test
    void testGetAllWorkspaces_Success() throws Exception {
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("W1").build();
        when(workspaceService.getAllWorkspaces()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("W1"));
    }

    @Test
    void testGetMyWorkspaces_Success() throws Exception {
        WorkspaceResponse response = WorkspaceResponse.builder().id(10L).name("W1").build();
        when(workspaceService.getWorkspacesByUserId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/workspaces/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("W1"));
    }

    @Test
    void testGetProjectsByWorkspaceId_Success() throws Exception {
        ProjectCardResponse card = ProjectCardResponse.builder().id(100L).name("Project 100").build();
        when(projectService.getProjectSummariesByWorkspaceId(10L)).thenReturn(List.of(card));

        mockMvc.perform(get("/api/workspaces/10/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].name").value("Project 100"));
    }

    @Test
    void testCreateProjectInWorkspace_Success() throws Exception {
        ProjectCreateRequest request = ProjectCreateRequest.builder().title("New Project").build();
        ProjectCardResponse card = ProjectCardResponse.builder().id(101L).name("New Project").build();

        when(projectService.createProject(eq(10L), any(ProjectCreateRequest.class))).thenReturn(card);

        mockMvc.perform(post("/api/workspaces/10/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.name").value("New Project"));
    }
}
