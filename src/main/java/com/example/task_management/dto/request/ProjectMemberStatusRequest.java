package com.example.task_management.dto.request;

import com.example.task_management.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectMemberStatusRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private MemberStatus status;
}
