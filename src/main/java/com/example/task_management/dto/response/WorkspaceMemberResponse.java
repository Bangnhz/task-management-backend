package com.example.task_management.dto.response;

import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private WorkspaceRole role;
    private MemberStatus status;
    private LocalDateTime joinedAt;
}
