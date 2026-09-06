package com.example.task_management.service;

import com.example.task_management.dto.request.ProjectInvitationCreateRequest;
import com.example.task_management.dto.response.InvitePreviewResponse;
import com.example.task_management.dto.response.ProjectInvitationResponse;
import com.example.task_management.dto.response.ProjectMemberResponse;

public interface ProjectInvitationService {
    public ProjectInvitationResponse createInvitation(Long projectId, ProjectInvitationCreateRequest request);
    public InvitePreviewResponse getInvitePreview(String token);
    public ProjectInvitationResponse getActiveInvitationByUser(Long projectId);
    public ProjectMemberResponse acceptInvitation(String token);
    public void cancelJoinRequest(String token);
}

