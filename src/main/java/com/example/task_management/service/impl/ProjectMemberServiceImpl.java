package com.example.task_management.service.impl;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.ProjectMemberConverter;
import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.entity.ProjectMemberEntity;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.repository.ProjectMemberRepository;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ActivityLogService;
import com.example.task_management.service.ProjectMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ProjectMemberServiceImpl implements ProjectMemberService {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    @Autowired
    private ProjectMemberConverter projectMemberConverter;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private ActivityLogConverter activityLogConverter;

    @Override
    public List<ProjectMemberResponse> getPendingMembers(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberEntity requester = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên dự án"));
        if (requester.getStatus() != MemberStatus.ACTIVE ||
                (requester.getRole() != ProjectRole.ADMIN && requester.getRole() != ProjectRole.OWNER)) {
            throw new AccessDeniedException("Bạn không có quyền xem danh sách chờ duyệt");
        }
        return projectMemberRepository.findByProjectIdAndStatus(projectId, MemberStatus.PENDING)
                .stream()
                .map(projectMemberConverter::toProjectMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectMemberResponse updateMemberStatus(Long projectId, Long memberId, MemberStatus status) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberEntity requester = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên dự án"));

        if (requester.getStatus() != MemberStatus.ACTIVE ||
                (requester.getRole() != ProjectRole.ADMIN && requester.getRole() != ProjectRole.OWNER)) {
            throw new RuntimeException("Bạn không có quyền thay đổi trạng thái thành viên");
        }

        ProjectMemberEntity targetMember = projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu tham gia"));

        if (targetMember.getStatus() != MemberStatus.PENDING) {
            throw new IllegalStateException("Chỉ thay đổi trạng thái thành viên đang chờ duyệt");
        }
        if (status != MemberStatus.ACTIVE && status != MemberStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ chuyển trạng thái sang ACTIVE hoặc REJECTED");
        }

        targetMember.setStatus(status);
        if (status == MemberStatus.ACTIVE) {
            targetMember.setJoinedAt(LocalDateTime.now());
        }

        ProjectMemberEntity updated = projectMemberRepository.save(targetMember);

        // Ghi log khi duyệt thành viên vào dự án
        if (status == MemberStatus.ACTIVE) {
            ActivityLogCreateRequest logRequest = activityLogConverter.toProjectMemberLogRequest(
                    updated,
                    ActivityAction.JOINED,
                    currentUserId,
                    Map.of("approvedBy", requester.getUser().getFullName())
            );
            activityLogService.log(logRequest);
        }

        return projectMemberConverter.toProjectMemberResponse(updated);
    }

    @Override
    @Transactional
    public void leaveProject(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberEntity member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc dự án này"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("Bạn không trong trạng thái hoạt động của dự án");
        }
        if (member.getRole() == ProjectRole.OWNER) {
            throw new IllegalStateException("Chủ sở hữu (Owner) không thể rời. Hãy chuyển quyền Owner trước.");
        }

        member.setStatus(MemberStatus.LEFT);
        ProjectMemberEntity updated = projectMemberRepository.save(member);

        // Ghi log rời dự án
        ActivityLogCreateRequest logRequest = activityLogConverter.toProjectMemberLogRequest(
                updated,
                ActivityAction.LEFT,
                currentUserId,
                null
        );
        activityLogService.log(logRequest);
    }

    @Override
    @Transactional
    public ProjectMemberResponse kickMember(Long projectId, Long memberId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberEntity requester = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Bạn không thuộc dự án này"));

        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản không hoạt động trong dự án này");
        }

        ProjectMemberEntity targetMember = projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên cần xóa"));

        if (targetMember.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ xóa thành viên đang hoạt động");
        }
        if (targetMember.getUser().getId().equals(currentUserId)) {
            throw new IllegalStateException("Không thể kick chính mình, hãy dùng tính năng rời dự án");
        }
        if (targetMember.getRole() == ProjectRole.OWNER) {
            throw new IllegalStateException("Không thể kick Chủ dự án (Owner)");
        }
        if (requester.getRole() == ProjectRole.ADMIN) {
            if (targetMember.getRole() == ProjectRole.ADMIN) {
                throw new RuntimeException("Admin không thể kick một Admin khác");
            }
        } else if (requester.getRole() != ProjectRole.OWNER) {
            throw new RuntimeException("Bạn không có quyền kick thành viên khác");
        }

        targetMember.setStatus(MemberStatus.KICKED);
        ProjectMemberEntity updated = projectMemberRepository.save(targetMember);

        // Ghi log xóa thành viên khỏi dự án
        ActivityLogCreateRequest logRequest = activityLogConverter.toProjectMemberLogRequest(
                updated,
                ActivityAction.KICKED,
                currentUserId,
                Map.of("kickedBy", requester.getUser().getFullName())
        );
        activityLogService.log(logRequest);

        return projectMemberConverter.toProjectMemberResponse(updated);
    }

    @Override
    public List<ProjectMemberResponse> getMembers(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberEntity requester = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên dự án"));
        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Bạn không phải thành viên dự án");
        }
        return projectMemberRepository.findByProjectIdAndStatus(projectId, MemberStatus.ACTIVE)
                .stream()
                .map(projectMemberConverter::toProjectMemberResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserMentionResponse> searchMembersForMention(Long projectId, String query) {
        String cleanQuery = (query != null) ? query.trim() : "";
        return projectMemberRepository.searchMembersForMention(projectId, cleanQuery);
    }
}