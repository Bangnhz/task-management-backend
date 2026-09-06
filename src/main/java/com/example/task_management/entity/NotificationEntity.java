package com.example.task_management.entity;

import com.example.task_management.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người NHẬN thông báo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserEntity recipient;

    // Người TẠO RA hành động (ví dụ: người vừa comment, người vừa assign task)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    // Nội dung tóm tắt hiển thị
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Loại thông báo (dùng để render icon hoặc filter)
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType type; // ASSIGNED, COMMENT, DUE_SOON, STATUS_CHANGED

    // ID của đối tượng liên quan (dùng để click vào điều hướng tới trang Task đó)
    @Column(name = "target_id")
    private Long targetId; // Ví dụ: taskId = 10

    // Đánh dấu đã đọc hay chưa
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}