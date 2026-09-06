package com.example.task_management.dto.response;

import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InvitePreviewResponse {
    private Long projectId;
    private String projectTitle;
    private ProjectRole assignedRole;
    private Boolean requiresApproval;
    private MemberStatus memberStatus;
}
