package com.example.task_management.security;

import com.example.task_management.enums.MemberStatus;
import com.example.task_management.repository.ProjectMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("projectSecurity")
public class ProjectSecurity {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    public boolean isMember(Long projectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        System.out.println(">>> CHECK QUYEN: projectId = " + projectId + " | currentUserId = " + currentUserId);

        boolean hasAccess = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .map(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElse(false);

        System.out.println(">>> KET QUA ACCESS: " + hasAccess);
        return hasAccess;
    }
}
