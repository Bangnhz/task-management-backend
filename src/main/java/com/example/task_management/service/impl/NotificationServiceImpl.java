package com.example.task_management.service.impl;

import com.example.task_management.converter.NotificationConverter;
import com.example.task_management.dto.response.NotificationResponse;
import com.example.task_management.entity.NotificationEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.repository.NotificationRepository;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationConverter notificationConverter;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(
            UserEntity recipient,
            UserEntity actor,
            String title,
            String content,
            NotificationType type,
            Long targetId
    ) {
        if (recipient == null || (actor != null && recipient.getId().equals(actor.getId()))) {
            return;
        }
        NotificationEntity notification = NotificationEntity.builder()
                .recipient(recipient)
                .actor(actor)
                .title(title)
                .content(content)
                .type(type)
                .targetId(targetId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NotificationEntity> notificationEntities = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        return notificationEntities.stream().map(notificationConverter::toResponse).toList();
    }

    @Override
    public long getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        NotificationEntity notificationEntity = notificationRepository.findById(notificationId).orElseThrow(() -> new RuntimeException("Notification not found"));
        if(!notificationEntity.getRecipient().getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new RuntimeException("You are not allowed to mark this as read");
        }
        notificationEntity.setIsRead(true);
        notificationRepository.save(notificationEntity);
    }

    @Override
    public void markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.markAllAsReadByRecipientId(userId);
    }
}
