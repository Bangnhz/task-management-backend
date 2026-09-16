package com.example.task_management.service;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.ProjectMemberConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.ProjectMemberEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.repository.ProjectMemberRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.ProjectMemberServiceImpl;
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
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectMemberConverter projectMemberConverter;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private ActivityLogConverter activityLogConverter;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    private final Long currentUserId = 1L;
    private final Long projectId = 10L;
    private UserEntity currentUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        currentUser = UserEntity.builder().id(currentUserId).fullName("Current User").email("cur@example.com").build();
        userDetails = new CustomUserDetails(currentUserId, "cur@example.com", "pass", "Current User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetPendingMembers_SuccessByAdmin() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.ADMIN)
                .build();
        ProjectMemberEntity pendingMember = ProjectMemberEntity.builder().id(2L).status(MemberStatus.PENDING).build();
        ProjectMemberResponse response = ProjectMemberResponse.builder().id(2L).status(MemberStatus.PENDING).build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByProjectIdAndStatus(projectId, MemberStatus.PENDING)).thenReturn(List.of(pendingMember));
        when(projectMemberConverter.toProjectMemberResponse(pendingMember)).thenReturn(response);

        List<ProjectMemberResponse> result = projectMemberService.getPendingMembers(projectId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPendingMembers_ForbiddenForNormalMember() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));

        assertThrows(AccessDeniedException.class, () -> projectMemberService.getPendingMembers(projectId));
    }

    @Test
    void testUpdateMemberStatus_ApproveActive_Success() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(5L)
                .status(MemberStatus.PENDING)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(5L, projectId)).thenReturn(Optional.of(targetMember));
        when(projectMemberRepository.save(targetMember)).thenReturn(targetMember);
        when(activityLogConverter.toProjectMemberLogRequest(any(), any(), any(), any()))
                .thenReturn(ActivityLogCreateRequest.builder().build());
        when(projectMemberConverter.toProjectMemberResponse(targetMember))
                .thenReturn(ProjectMemberResponse.builder().id(5L).status(MemberStatus.ACTIVE).build());

        ProjectMemberResponse result = projectMemberService.updateMemberStatus(projectId, 5L, MemberStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(MemberStatus.ACTIVE, targetMember.getStatus());
        assertNotNull(targetMember.getJoinedAt());
        verify(activityLogService, times(1)).log(any());
    }

    @Test
    void testUpdateMemberStatus_Reject_Success() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(5L)
                .status(MemberStatus.PENDING)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(5L, projectId)).thenReturn(Optional.of(targetMember));
        when(projectMemberRepository.save(targetMember)).thenReturn(targetMember);
        when(projectMemberConverter.toProjectMemberResponse(targetMember))
                .thenReturn(ProjectMemberResponse.builder().id(5L).status(MemberStatus.REJECTED).build());

        ProjectMemberResponse result = projectMemberService.updateMemberStatus(projectId, 5L, MemberStatus.REJECTED);

        assertNotNull(result);
        assertEquals(MemberStatus.REJECTED, targetMember.getStatus());
        verify(activityLogService, never()).log(any());
    }

    @Test
    void testUpdateMemberStatus_TargetNotPending_ThrowsException() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(5L)
                .status(MemberStatus.ACTIVE)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(5L, projectId)).thenReturn(Optional.of(targetMember));

        assertThrows(IllegalStateException.class, () ->
                projectMemberService.updateMemberStatus(projectId, 5L, MemberStatus.ACTIVE));
    }

    @Test
    void testUpdateMemberStatus_InvalidStatus_ThrowsException() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(5L)
                .status(MemberStatus.PENDING)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(5L, projectId)).thenReturn(Optional.of(targetMember));

        assertThrows(IllegalArgumentException.class, () ->
                projectMemberService.updateMemberStatus(projectId, 5L, MemberStatus.PENDING));
    }

    @Test
    void testLeaveProject_Success() {
        ProjectMemberEntity member = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(member));
        when(projectMemberRepository.save(member)).thenReturn(member);

        projectMemberService.leaveProject(projectId);

        assertEquals(MemberStatus.LEFT, member.getStatus());
        verify(activityLogService, times(1)).log(any());
    }

    @Test
    void testLeaveProject_OwnerCannotLeave_ThrowsException() {
        ProjectMemberEntity member = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(member));

        assertThrows(IllegalStateException.class, () -> projectMemberService.leaveProject(projectId));
    }

    @Test
    void testKickMember_AdminKickingMember_Success() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.ADMIN)
                .build();

        UserEntity targetUser = UserEntity.builder().id(99L).build();
        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(10L)
                .user(targetUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(10L, projectId)).thenReturn(Optional.of(targetMember));
        when(projectMemberRepository.save(targetMember)).thenReturn(targetMember);
        when(projectMemberConverter.toProjectMemberResponse(targetMember))
                .thenReturn(ProjectMemberResponse.builder().id(10L).status(MemberStatus.KICKED).build());

        ProjectMemberResponse result = projectMemberService.kickMember(projectId, 10L);

        assertNotNull(result);
        assertEquals(MemberStatus.KICKED, targetMember.getStatus());
        verify(activityLogService, times(1)).log(any());
    }

    @Test
    void testKickMember_AdminCannotKickAdmin_ThrowsException() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.ADMIN)
                .build();

        UserEntity targetUser = UserEntity.builder().id(99L).build();
        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(10L)
                .user(targetUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.ADMIN)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(10L, projectId)).thenReturn(Optional.of(targetMember));

        assertThrows(RuntimeException.class, () -> projectMemberService.kickMember(projectId, 10L));
    }

    @Test
    void testKickMember_CannotKickSelf_ThrowsException() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(10L)
                .user(currentUser) // self
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(10L, projectId)).thenReturn(Optional.of(targetMember));

        assertThrows(IllegalStateException.class, () -> projectMemberService.kickMember(projectId, 10L));
    }

    @Test
    void testKickMember_CannotKickOwner_ThrowsException() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.ADMIN)
                .build();

        UserEntity ownerUser = UserEntity.builder().id(50L).build();
        ProjectMemberEntity targetMember = ProjectMemberEntity.builder()
                .id(10L)
                .user(ownerUser)
                .status(MemberStatus.ACTIVE)
                .role(ProjectRole.OWNER)
                .build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByIdAndProjectId(10L, projectId)).thenReturn(Optional.of(targetMember));

        assertThrows(IllegalStateException.class, () -> projectMemberService.kickMember(projectId, 10L));
    }

    @Test
    void testGetMembers_Success() {
        ProjectMemberEntity requester = ProjectMemberEntity.builder()
                .user(currentUser)
                .status(MemberStatus.ACTIVE)
                .build();

        ProjectMemberEntity member = ProjectMemberEntity.builder().id(1L).status(MemberStatus.ACTIVE).build();
        ProjectMemberResponse response = ProjectMemberResponse.builder().id(1L).build();

        when(projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)).thenReturn(Optional.of(requester));
        when(projectMemberRepository.findByProjectIdAndStatus(projectId, MemberStatus.ACTIVE)).thenReturn(List.of(member));
        when(projectMemberConverter.toProjectMemberResponse(member)).thenReturn(response);

        List<ProjectMemberResponse> results = projectMemberService.getMembers(projectId);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchMembersForMention_Success() {
        UserMentionResponse mention = new UserMentionResponse(1L, "User 1", "user1@example.com", "avatar.png");
        when(projectMemberRepository.searchMembersForMention(projectId, "user")).thenReturn(List.of(mention));

        List<UserMentionResponse> results = projectMemberService.searchMembersForMention(projectId, "  user  ");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("User 1", results.get(0).getFullName());
    }
}
