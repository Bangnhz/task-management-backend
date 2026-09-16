package com.example.task_management.service;

import com.example.task_management.converter.ProjectInvitationConverter;
import com.example.task_management.converter.ProjectMemberConverter;
import com.example.task_management.dto.request.ProjectInvitationCreateRequest;
import com.example.task_management.dto.response.InvitePreviewResponse;
import com.example.task_management.dto.response.ProjectInvitationResponse;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.entity.*;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.*;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.ProjectInvitationServiceImpl;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectInvitationServiceImplTest {

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private ProjectInvitationConverter projectInvitationConverter;

    @Mock
    private ProjectMemberConverter projectMemberConverter;

    @InjectMocks
    private ProjectInvitationServiceImpl invitationService;

    private final Long userId = 1L;
    private final Long projectId = 10L;
    private final Long workspaceId = 100L;
    private UserEntity user;
    private ProjectEntity project;
    private WorkspaceEntity workspace;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(userId).fullName("User").email("u@example.com").build();
        workspace = WorkspaceEntity.builder().id(workspaceId).name("Workspace").build();
        project = ProjectEntity.builder().id(projectId).title("Project").workspace(workspace).build();

        CustomUserDetails userDetails = new CustomUserDetails(userId, "u@example.com", "pass", "User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateInvitation_Success_DeactivatesOldInvitation() {
        ProjectInvitationCreateRequest request = ProjectInvitationCreateRequest.builder()
                .expireDays(14)
                .role(ProjectRole.MEMBER)
                .requiresApproval(true)
                .build();

        ProjectInvitationEntity oldInv = ProjectInvitationEntity.builder().isActive(true).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectInvitationRepository.findFirstByProject_IdAndCreatedBy_IdAndIsActiveTrueAndExpiresAtAfter(
                eq(projectId), eq(userId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(oldInv));

        ProjectInvitationEntity saved = ProjectInvitationEntity.builder().id(1L).token("abc").build();
        when(projectInvitationRepository.save(any(ProjectInvitationEntity.class))).thenReturn(saved);
        when(projectInvitationConverter.toProjectInvitationResponse(saved))
                .thenReturn(ProjectInvitationResponse.builder().id(1L).token("abc").build());

        ProjectInvitationResponse result = invitationService.createInvitation(projectId, request);

        assertNotNull(result);
        assertEquals("abc", result.getToken());
        assertFalse(oldInv.getIsActive()); // Old invitation was deactivated

        ArgumentCaptor<ProjectInvitationEntity> captor = ArgumentCaptor.forClass(ProjectInvitationEntity.class);
        verify(projectInvitationRepository, times(1)).save(captor.capture());
        ProjectInvitationEntity captured = captor.getValue();
        assertEquals(ProjectRole.MEMBER, captured.getRole());
        assertTrue(captured.getRequiresApproval());
        assertTrue(captured.getIsActive());
    }

    @Test
    void testGetInvitePreview_Success() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("valid_token")
                .project(project)
                .role(ProjectRole.VIEWER)
                .requiresApproval(false)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        when(projectInvitationRepository.findByToken("valid_token")).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        InvitePreviewResponse result = invitationService.getInvitePreview("valid_token");

        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals("Project", result.getProjectTitle());
        assertNull(result.getMemberStatus());
    }

    @Test
    void testGetInvitePreview_Expired_ThrowsException() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("expired_token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();

        when(projectInvitationRepository.findByToken("expired_token")).thenReturn(Optional.of(invitation));

        assertThrows(IllegalStateException.class, () -> invitationService.getInvitePreview("expired_token"));
    }

    @Test
    void testAcceptInvitation_RequiresApproval_BecomesPending() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("invite_token")
                .project(project)
                .role(ProjectRole.MEMBER)
                .requiresApproval(true)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        when(projectInvitationRepository.findByToken("invite_token")).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.save(any(ProjectMemberEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(mock(WorkspaceMemberEntity.class)));
        when(projectMemberConverter.toProjectMemberResponse(any(ProjectMemberEntity.class)))
                .thenAnswer(i -> {
                    ProjectMemberEntity entity = i.getArgument(0);
                    return ProjectMemberResponse.builder().status(entity.getStatus()).build();
                });

        ProjectMemberResponse response = invitationService.acceptInvitation("invite_token");

        assertNotNull(response);
        assertEquals(MemberStatus.PENDING, response.getStatus());
    }

    @Test
    void testAcceptInvitation_DirectActive_AutoAddsWorkspaceGuest() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("direct_token")
                .project(project)
                .role(ProjectRole.VIEWER)
                .requiresApproval(false)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        when(projectInvitationRepository.findByToken("direct_token")).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.save(any(ProjectMemberEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.empty()); // Not a workspace member
        when(projectMemberConverter.toProjectMemberResponse(any(ProjectMemberEntity.class)))
                .thenAnswer(i -> {
                    ProjectMemberEntity entity = i.getArgument(0);
                    return ProjectMemberResponse.builder().status(entity.getStatus()).build();
                });

        ProjectMemberResponse response = invitationService.acceptInvitation("direct_token");

        assertNotNull(response);
        assertEquals(MemberStatus.ACTIVE, response.getStatus());

        // Verify WorkspaceMember was created as GUEST
        ArgumentCaptor<WorkspaceMemberEntity> wmCaptor = ArgumentCaptor.forClass(WorkspaceMemberEntity.class);
        verify(workspaceMemberRepository, times(1)).save(wmCaptor.capture());
        assertEquals(WorkspaceRole.GUEST, wmCaptor.getValue().getRole());
        assertEquals(workspace, wmCaptor.getValue().getWorkspace());
        assertEquals(user, wmCaptor.getValue().getUser());
    }

    @Test
    void testAcceptInvitation_AlreadyMember_ReturnsExisting() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("invite_token")
                .project(project)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        ProjectMemberEntity existing = ProjectMemberEntity.builder().id(10L).status(MemberStatus.ACTIVE).build();

        when(projectInvitationRepository.findByToken("invite_token")).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(existing));
        when(projectMemberConverter.toProjectMemberResponse(existing))
                .thenReturn(ProjectMemberResponse.builder().id(10L).status(MemberStatus.ACTIVE).build());

        ProjectMemberResponse response = invitationService.acceptInvitation("invite_token");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        verify(projectMemberRepository, never()).save(any(ProjectMemberEntity.class));
    }

    @Test
    void testCancelJoinRequest_Success() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("cancel_token")
                .project(project)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        ProjectMemberEntity pendingMember = ProjectMemberEntity.builder()
                .id(20L)
                .status(MemberStatus.PENDING)
                .build();

        when(projectInvitationRepository.findByToken("cancel_token")).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(pendingMember));

        invitationService.cancelJoinRequest("cancel_token");

        verify(projectMemberRepository, times(1)).delete(pendingMember);
    }

    @Test
    void testCancelJoinRequest_NotPending_ThrowsException() {
        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .token("cancel_token")
                .project(project)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .isActive(true)
                .build();

        ProjectMemberEntity activeMember = ProjectMemberEntity.builder()
                .id(20L)
                .status(MemberStatus.ACTIVE)
                .build();

        when(projectInvitationRepository.findByToken("cancel_token")).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(activeMember));

        assertThrows(IllegalStateException.class, () -> invitationService.cancelJoinRequest("cancel_token"));
        verify(projectMemberRepository, never()).delete(any());
    }
}
