package com.example.task_management.controller;

import com.example.task_management.dto.request.ProjectInvitationCreateRequest;
import com.example.task_management.dto.response.InvitePreviewResponse;
import com.example.task_management.dto.response.ProjectInvitationResponse;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.exception.GlobalExceptionHandler;
import com.example.task_management.service.ProjectInvitationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectInvitationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectInvitationService projectInvitationService;

    @InjectMocks
    private ProjectInvitationController invitationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invitationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateInvitation_Success() throws Exception {
        ProjectInvitationCreateRequest request = ProjectInvitationCreateRequest.builder()
                .expireDays(7)
                .role(ProjectRole.VIEWER)
                .build();

        ProjectInvitationResponse response = ProjectInvitationResponse.builder()
                .id(1L)
                .token("test-token")
                .role(ProjectRole.VIEWER)
                .build();

        when(projectInvitationService.createInvitation(eq(10L), any(ProjectInvitationCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/project-invitations/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    void testGetPreview_Success() throws Exception {
        InvitePreviewResponse response = InvitePreviewResponse.builder()
                .projectId(10L)
                .projectTitle("Test Project")
                .assignedRole(ProjectRole.VIEWER)
                .build();

        when(projectInvitationService.getInvitePreview("test-token")).thenReturn(response);

        mockMvc.perform(get("/api/project-invitations/test-token/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(10))
                .andExpect(jsonPath("$.projectTitle").value("Test Project"));
    }

    @Test
    void testGetActiveInvitationByUser_Success() throws Exception {
        ProjectInvitationResponse response = ProjectInvitationResponse.builder()
                .id(1L)
                .token("active-token")
                .build();

        when(projectInvitationService.getActiveInvitationByUser(10L)).thenReturn(response);

        mockMvc.perform(get("/api/project-invitations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("active-token"));
    }

    @Test
    void testAcceptInvitation_Success() throws Exception {
        ProjectMemberResponse response = ProjectMemberResponse.builder()
                .id(5L)
                .status(MemberStatus.ACTIVE)
                .build();

        when(projectInvitationService.acceptInvitation("test-token")).thenReturn(response);

        mockMvc.perform(post("/api/project-invitations/test-token/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testCancelJoinRequest_Success() throws Exception {
        doNothing().when(projectInvitationService).cancelJoinRequest("test-token");

        mockMvc.perform(post("/api/project-invitations/test-token/cancel"))
                .andExpect(status().isNoContent());
    }
}
