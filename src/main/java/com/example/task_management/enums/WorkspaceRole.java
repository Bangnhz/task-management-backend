package com.example.task_management.enums;

public enum WorkspaceRole {
    OWNER,   // Người lập ra Workspace (toàn quyền, xóa workspace)
    ADMIN,   // Quản trị viên (mời người vào workspace, tạo dự án)
    MEMBER,  // Nhân viên nội bộ (xem được các dự án chung của workspace)
    GUEST    // Khách ngoài (chỉ xem được dự án họ được mời đích danh)
}