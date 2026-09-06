package com.example.task_management.service;

import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.dto.response.ProjectMemberResponse;
import com.example.task_management.enums.MemberStatus;
import java.util.List;

public interface ProjectMemberService {
    public List<ProjectMemberResponse> getPendingMembers(Long projectId);
    public ProjectMemberResponse updateMemberStatus(Long projectId, Long memberId, MemberStatus status);
    public void leaveProject(Long projectId);
    public ProjectMemberResponse kickMember(Long projectId, Long memberId);
    public List<ProjectMemberResponse> getMembers(Long projectId);
    public List<UserMentionResponse> searchMembersForMention(Long projectId, String query);
}

