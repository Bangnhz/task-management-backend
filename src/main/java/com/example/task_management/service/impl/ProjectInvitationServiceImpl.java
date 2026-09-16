package com.example.task_management.service.impl;

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
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ProjectInvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ProjectInvitationServiceImpl implements ProjectInvitationService {

    @Autowired
    private ProjectInvitationRepository projectInvitationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private ProjectInvitationConverter projectInvitationConverter;
    @Autowired
    private ProjectMemberConverter projectMemberConverter;

    @Override
    @Transactional
    public ProjectInvitationResponse createInvitation(Long projectId, ProjectInvitationCreateRequest request) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));

        Long userId = SecurityUtils.getCurrentUserId();
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        LocalDateTime now = LocalDateTime.now();

        projectInvitationRepository.findFirstByProject_IdAndCreatedBy_IdAndIsActiveTrueAndExpiresAtAfter(projectId,userId,now).ifPresent(oldInv -> oldInv.setIsActive(false));

        int days = (request != null && request.getExpireDays() != null) ? request.getExpireDays() : 7;
        ProjectRole role = (request != null && request.getRole() != null) ? request.getRole() : ProjectRole.VIEWER;
        boolean requiresApproval = (request != null && request.getRequiresApproval() != null) && request.getRequiresApproval();

        ProjectInvitationEntity invitation = ProjectInvitationEntity.builder()
                .project(project)
                .token(SecurityUtils.generateToken())
                .role(role)
                .requiresApproval(requiresApproval)
                .expiresAt(now.plusDays(days))
                .isActive(true)
                .createdBy(userEntity)
                .build();

        ProjectInvitationEntity saved = projectInvitationRepository.save(invitation);
        return projectInvitationConverter.toProjectInvitationResponse(saved);
    }

    @Override
    public InvitePreviewResponse getInvitePreview(String token) {
        ProjectInvitationEntity invitation = projectInvitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link mời không tồn tại"));

        if (!invitation.isValid()) {
            throw new IllegalStateException("Link mời đã hết hạn hoặc bị vô hiệu hóa");
        }

        Long projectId = invitation.getProject().getId();
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<ProjectMemberEntity> memberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);

        MemberStatus status = memberOpt.map(ProjectMemberEntity::getStatus).orElse(null);

        return InvitePreviewResponse.builder()
                .projectId(projectId)
                .projectTitle(invitation.getProject().getTitle())
                .assignedRole(invitation.getRole())
                .requiresApproval(invitation.getRequiresApproval())
                .memberStatus(status)
                .build();
    }

    @Override
    public ProjectInvitationResponse getActiveInvitationByUser(Long projectId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("Không tìm thấy dự án với ID: " + projectId);
        }

        Optional<ProjectInvitationEntity> invitationEntity = projectInvitationRepository.findByCreatedBy_IdAndProject_IdAndIsActiveTrueAndExpiresAtAfter(userId,projectId,LocalDateTime.now());
        return invitationEntity.map(projectInvitationConverter::toProjectInvitationResponse).orElse(null);
    }

    @Override
    @Transactional
    public ProjectMemberResponse acceptInvitation(String token) {
        ProjectInvitationEntity invitation = projectInvitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Liên kết mời không tồn tại"));

        if (!invitation.isValid()) {
            throw new IllegalStateException("Liên kết mời đã hết hạn hoặc bị vô hiệu hóa");
        }

        Long projectId = invitation.getProject().getId();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Optional<ProjectMemberEntity> existingMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId);
        if (existingMember.isPresent()) {
            return projectMemberConverter.toProjectMemberResponse(existingMember.get());
        }

        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản: " + currentUserId));

        boolean needsApproval = Boolean.TRUE.equals(invitation.getRequiresApproval());
        MemberStatus initialStatus = needsApproval ? MemberStatus.PENDING : MemberStatus.ACTIVE;

        ProjectMemberEntity newMember = ProjectMemberEntity.builder()
                .project(invitation.getProject())
                .user(currentUser)
                .role(invitation.getRole())
                .invitation(invitation)
                .status(initialStatus)
                .joinedAt(LocalDateTime.now())
                .build();
        ProjectMemberEntity savedMember = projectMemberRepository.save(newMember);

        WorkspaceEntity workspace = invitation.getProject().getWorkspace();
        boolean isWorkspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), currentUserId).isPresent();

        if (!isWorkspaceMember) {
            WorkspaceMemberEntity workspaceMember = WorkspaceMemberEntity.builder()
                    .workspace(workspace)
                    .user(currentUser)
                    .role(WorkspaceRole.GUEST)
                    .build();
            workspaceMemberRepository.save(workspaceMember);
        }
        return projectMemberConverter.toProjectMemberResponse(savedMember);
    }

    @Override
    public void cancelJoinRequest(String token) {
        ProjectInvitationEntity invitation = projectInvitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Liên kết mời không tồn tại"));

        if (!invitation.isValid()) {
            throw new IllegalStateException("Liên kết mời đã hết hạn hoặc bị vô hiệu hóa");
        }
        Long projectId = invitation.getProject().getId();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberEntity member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId).orElseThrow(()-> new RuntimeException("Can not find join request"));
        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy yêu cầu khi đang ở trạng thái chờ duyệt");
        }
        projectMemberRepository.delete(member);
    }
}

