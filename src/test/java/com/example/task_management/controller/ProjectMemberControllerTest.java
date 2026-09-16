package com.example.task_management.controller;

import com.example.task_management.dto.request.ProjectMemberStatusRequest;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.exception.GlobalExceptionHandler;
import com.example.task_management.service.ProjectMemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private ProjectMemberController projectMemberController;

    private ObjectMapper objectMapper;

    private final Long projectId = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectMemberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET /{projectId}/members/mention-suggestions Tests")
    class SearchMentionUsersTests {

        @Test
        @DisplayName("Should return matching user mention suggestions when query provided")
        void testSearchMentionUsers_WithQuery_Success() throws Exception {
            UserMentionResponse u1 = UserMentionResponse.builder()
                    .id(1L)
                    .fullName("Alice Smith")
                    .email("alice@example.com")
                    .build();

            when(projectMemberService.searchMembersForMention(projectId, "ali")).thenReturn(List.of(u1));

            mockMvc.perform(get("/api/projects/{projectId}/members/mention-suggestions", projectId)
                            .param("query", "ali"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].fullName").value("Alice Smith"));

            verify(projectMemberService, times(1)).searchMembersForMention(projectId, "ali");
        }

        @Test
        @DisplayName("Should default query to empty string when not provided")
        void testSearchMentionUsers_DefaultEmptyQuery_Success() throws Exception {
            when(projectMemberService.searchMembersForMention(projectId, "")).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/projects/{projectId}/members/mention-suggestions", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(projectMemberService, times(1)).searchMembersForMention(projectId, "");
        }
    }

    @Nested
    @DisplayName("GET /{projectId}/members/pending Tests")
    class GetPendingMembersTests {

        @Test
        @DisplayName("Should return pending project members")
        void testGetPendingMembers_Success() throws Exception {
            ProjectMemberResponse pending = ProjectMemberResponse.builder()
                    .id(100L)
                    .userId(2L)
                    .fullName("Pending User")
                    .role(ProjectRole.MEMBER)
                    .status(MemberStatus.PENDING)
                    .build();

            when(projectMemberService.getPendingMembers(projectId)).thenReturn(List.of(pending));

            mockMvc.perform(get("/api/projects/{projectId}/members/pending", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            verify(projectMemberService, times(1)).getPendingMembers(projectId);
        }
    }

    @Nested
    @DisplayName("GET /{projectId}/members Tests")
    class GetMembersTests {

        @Test
        @DisplayName("Should return active project members")
        void testGetMembers_Success() throws Exception {
            ProjectMemberResponse member = ProjectMemberResponse.builder()
                    .id(101L)
                    .userId(3L)
                    .fullName("Active Member")
                    .role(ProjectRole.MEMBER)
                    .status(MemberStatus.ACTIVE)
                    .build();

            when(projectMemberService.getMembers(projectId)).thenReturn(List.of(member));

            mockMvc.perform(get("/api/projects/{projectId}/members", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(101))
                    .andExpect(jsonPath("$[0].fullName").value("Active Member"));

            verify(projectMemberService, times(1)).getMembers(projectId);
        }
    }

    @Nested
    @DisplayName("PATCH /{projectId}/members/{memberId} Tests")
    class UpdateMemberStatusTests {

        @Test
        @DisplayName("Should update member status successfully")
        void testUpdateMemberStatus_Success() throws Exception {
            Long memberId = 50L;
            ProjectMemberStatusRequest request = new ProjectMemberStatusRequest();
            request.setStatus(MemberStatus.ACTIVE);

            ProjectMemberResponse updated = ProjectMemberResponse.builder()
                    .id(memberId)
                    .status(MemberStatus.ACTIVE)
                    .build();

            when(projectMemberService.updateMemberStatus(projectId, memberId, MemberStatus.ACTIVE))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/projects/{projectId}/members/{memberId}", projectId, memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(memberId))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(projectMemberService, times(1)).updateMemberStatus(projectId, memberId, MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when status is null")
        void testUpdateMemberStatus_ValidationError_NullStatus() throws Exception {
            Long memberId = 50L;
            ProjectMemberStatusRequest request = new ProjectMemberStatusRequest();
            request.setStatus(null);

            mockMvc.perform(patch("/api/projects/{projectId}/members/{memberId}", projectId, memberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Error"));

            verify(projectMemberService, never()).updateMemberStatus(any(), any(), any());
        }
    }
}
