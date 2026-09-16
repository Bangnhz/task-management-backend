package com.example.task_management.service;

import com.example.task_management.converter.NotificationConverter;
import com.example.task_management.dto.response.NotificationResponse;
import com.example.task_management.entity.NotificationEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.repository.NotificationRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationConverter notificationConverter;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private final Long userId = 1L;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(userId, "u@example.com", "pass", "User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSendNotification_Success() {
        UserEntity recipient = UserEntity.builder().id(2L).fullName("Recipient").build();
        UserEntity actor = UserEntity.builder().id(1L).fullName("Actor").build();

        notificationService.sendNotification(
                recipient, actor, "Title", "Content", NotificationType.ASSIGNED, 100L
        );

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        NotificationEntity saved = captor.getValue();
        assertEquals(recipient, saved.getRecipient());
        assertEquals(actor, saved.getActor());
        assertEquals("Title", saved.getTitle());
        assertEquals("Content", saved.getContent());
        assertEquals(NotificationType.ASSIGNED, saved.getType());
        assertEquals(100L, saved.getTargetId());
        assertFalse(saved.getIsRead());
    }

    @Test
    void testSendNotification_RecipientIsNull_DoesNothing() {
        UserEntity actor = UserEntity.builder().id(1L).build();
        notificationService.sendNotification(null, actor, "Title", "Content", NotificationType.COMMENT, 100L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testSendNotification_ActorIsRecipient_DoesNothing() {
        UserEntity user = UserEntity.builder().id(1L).build();
        notificationService.sendNotification(user, user, "Title", "Content", NotificationType.COMMENT, 100L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testGetMyNotifications_Success() {
        NotificationEntity entity = NotificationEntity.builder().id(10L).title("Note").build();
        NotificationResponse response = NotificationResponse.builder().id(10L).title("Note").build();

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(entity));
        when(notificationConverter.toResponse(entity)).thenReturn(response);

        List<NotificationResponse> results = notificationService.getMyNotifications();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Note", results.get(0).getTitle());
    }

    @Test
    void testGetUnreadCount_Success() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount();
        assertEquals(5L, count);
    }

    @Test
    void testMarkAsRead_Success() {
        UserEntity recipient = UserEntity.builder().id(userId).build();
        NotificationEntity entity = NotificationEntity.builder().id(10L).recipient(recipient).isRead(false).build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(entity));

        notificationService.markAsRead(10L);

        assertTrue(entity.getIsRead());
        verify(notificationRepository, times(1)).save(entity);
    }

    @Test
    void testMarkAsRead_Forbidden_ThrowsException() {
        UserEntity otherRecipient = UserEntity.builder().id(99L).build();
        NotificationEntity entity = NotificationEntity.builder().id(10L).recipient(otherRecipient).isRead(false).build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(10L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAllAsRead_Success() {
        notificationService.markAllAsRead();
        verify(notificationRepository, times(1)).markAllAsReadByRecipientId(userId);
    }
}
