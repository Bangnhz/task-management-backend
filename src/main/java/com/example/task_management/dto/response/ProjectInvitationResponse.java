package com.example.task_management.dto.response;

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
public class ProjectInvitationResponse {
    private Long id;
    private String token;
    private LocalDateTime expiresAt;
    private ProjectRole role;
    private Boolean requiresApproval;
}
