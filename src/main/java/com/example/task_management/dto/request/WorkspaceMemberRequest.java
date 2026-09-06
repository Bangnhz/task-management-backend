package com.example.task_management.dto.request;

import com.example.task_management.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMemberRequest {

    @NotNull
    private Long userId;
    private WorkspaceRole role;
}
