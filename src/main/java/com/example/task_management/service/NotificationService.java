package com.example.task_management.service;

import com.example.task_management.dto.response.NotificationResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    void sendNotification(UserEntity recipient, UserEntity actor, String title, String content, NotificationType type, Long targetId);
    List<NotificationResponse> getMyNotifications();
    long getUnreadCount();
    void markAsRead(Long notificationId);
    void markAllAsRead();
}