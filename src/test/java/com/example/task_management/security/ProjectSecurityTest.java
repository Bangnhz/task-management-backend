package com.example.task_management.security;

import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.ProjectMemberEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.entity.WorkspaceMemberEntity;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectVisibility;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.ProjectMemberRepository;
import com.example.task_management.repository.ProjectRepository;
import com.example.task_management.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSecurityTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private ProjectSecurity projectSecurity;

    private final Long userId = 1L;
    private final Long projectId = 10L;
    private final Long workspaceId = 100L;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(
                userId, "test@example.com", "pass", "Test User", null, Collections.emptyList()
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
    void testCanAccessProject_NullProjectId_ReturnsFalse() {
        assertFalse(projectSecurity.canAccessProject(null));
    }

    @Test
    void testCanAccessProject_Unauthenticated_ThrowsException() {
        SecurityContextHolder.clearContext();
        assertThrows(IllegalStateException.class, () -> projectSecurity.canAccessProject(projectId));
    }

    @Test
    void testCanAccessProject_ProjectNotFound_ReturnsFalse() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertFalse(projectSecurity.canAccessProject(projectId));
    }

    @Test
    void testCanAccessProject_DirectActiveProjectMember_ReturnsTrue() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).visibility(ProjectVisibility.PRIVATE).build();
        ProjectMemberEntity member = ProjectMemberEntity.builder().status(MemberStatus.ACTIVE).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        assertTrue(projectSecurity.canAccessProject(projectId));
        assertTrue(projectSecurity.isMember(projectId));
    }

    @Test
    void testCanAccessProject_PublicProject_ReturnsTrue() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).visibility(ProjectVisibility.PUBLIC).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertTrue(projectSecurity.canAccessProject(projectId));
    }

    @Test
    void testCanAccessProject_WorkspaceProject_ActiveNonGuestMember_ReturnsTrue() {
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

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(wm));

        assertTrue(projectSecurity.canAccessProject(projectId));
    }

    @Test
    void testCanAccessProject_WorkspaceProject_GuestMember_ReturnsFalse() {
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

        assertFalse(projectSecurity.canAccessProject(projectId));
    }

    @Test
    void testCanAccessProject_WorkspaceProject_InactiveMember_ReturnsFalse() {
        WorkspaceEntity workspace = WorkspaceEntity.builder().id(workspaceId).build();
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .visibility(ProjectVisibility.WORKSPACE)
                .workspace(workspace)
                .build();

        WorkspaceMemberEntity wm = WorkspaceMemberEntity.builder()
                .status(MemberStatus.PENDING)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(wm));

        assertFalse(projectSecurity.canAccessProject(projectId));
    }

    @Test
    void testCanAccessProject_PrivateProject_NotMember_ReturnsFalse() {
        ProjectEntity project = ProjectEntity.builder().id(projectId).visibility(ProjectVisibility.PRIVATE).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertFalse(projectSecurity.canAccessProject(projectId));
    }
}
