package com.example.task_management.security;

import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.ProjectMemberEntity;
import com.example.task_management.entity.WorkspaceMemberEntity;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectVisibility;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.ProjectMemberRepository;
import com.example.task_management.repository.ProjectRepository;
import com.example.task_management.repository.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("projectSecurity")
public class ProjectSecurity {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    public boolean canAccessProject(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || projectId == null) {
            return false;
        }

        ProjectEntity project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        // 1. Thành viên trực tiếp trong ProjectMember (bao gồm Owner) có status == ACTIVE
        Optional<ProjectMemberEntity> projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId);
        if (projectMember.isPresent() && projectMember.get().getStatus() == MemberStatus.ACTIVE) {
            return true;
        }

        // 2. Dự án có visibility == PUBLIC
        if (ProjectVisibility.PUBLIC.equals(project.getVisibility())) {
            return true;
        }

        // 3. Dự án có visibility == WORKSPACE VÀ User là thành viên thuộc Workspace đó với status == ACTIVE và role != GUEST
        if (ProjectVisibility.WORKSPACE.equals(project.getVisibility()) && project.getWorkspace() != null) {
            Optional<WorkspaceMemberEntity> workspaceMember =
                    workspaceMemberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), currentUserId);

            if (workspaceMember.isPresent()
                    && workspaceMember.get().getStatus() == MemberStatus.ACTIVE
                    && workspaceMember.get().getRole() != WorkspaceRole.GUEST) {
                return true;
            }
        }

        return false;
    }

    public boolean isMember(Long projectId) {
        return canAccessProject(projectId);
    }
}
