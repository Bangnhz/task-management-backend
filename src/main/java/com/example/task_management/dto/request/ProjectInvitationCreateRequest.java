package com.example.task_management.dto.request;

import com.example.task_management.enums.ProjectRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectInvitationCreateRequest {

    @Min(value = 1, message = "Thời hạn lời mời tối thiểu là 1 ngày")
    @Builder.Default
    private Integer expireDays = 7;

    @NotNull(message = "Vai trò được gán không được để trống")
    @Builder.Default
    private ProjectRole role = ProjectRole.MEMBER;

    @Builder.Default
    private Boolean requiresApproval = false;
}
