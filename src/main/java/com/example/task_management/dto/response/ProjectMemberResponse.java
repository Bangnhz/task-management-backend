package com.example.task_management.dto.response;

import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponse {
    private Long id;
    private Long projectId;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private ProjectRole role;
    private MemberStatus status;
    private LocalDateTime joinedAt;
}
