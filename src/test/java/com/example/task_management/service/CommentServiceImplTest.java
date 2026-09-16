package com.example.task_management.service;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.CommentConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.request.CommentMentionRequest;
import com.example.task_management.dto.request.CommentRequest;
import com.example.task_management.dto.response.CommentResponse;
import com.example.task_management.entity.AttachmentEntity;
import com.example.task_management.entity.CommentEntity;
import com.example.task_management.entity.TaskEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.repository.AttachmentRepository;
import com.example.task_management.repository.CommentMentionRepository;
import com.example.task_management.repository.CommentRepository;
import com.example.task_management.repository.TaskRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentMentionRepository commentMentionRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private CommentConverter commentConverter;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private ActivityLogConverter activityLogConverter;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private final Long userId = 1L;
    private final Long taskId = 10L;
    private UserEntity user;
    private TaskEntity task;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(userId).fullName("Author User").email("author@example.com").build();
        task = TaskEntity.builder().id(taskId).title("Task Title").build();

        CustomUserDetails userDetails = new CustomUserDetails(userId, "author@example.com", "pass", "Author User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateComment_Success_WithMentionsAndAttachments() {
        UserEntity mentionedUser = UserEntity.builder().id(2L).fullName("Mentioned User").build();
        AttachmentEntity attachment = AttachmentEntity.builder().id(5L).uploadedBy(user).build();

        CommentMentionRequest mentionReq = CommentMentionRequest.builder()
                .userId(2L)
                .startIndex(0)
                .length(10)
                .build();

        CommentRequest request = CommentRequest.builder()
                .content("@Mentioned Hello!")
                .attachmentIds(List.of(5L))
                .mentions(List.of(mentionReq))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentionedUser));

        CommentEntity savedComment = new CommentEntity();
        savedComment.setId(100L);
        savedComment.setContent(request.getContent());
        when(commentRepository.save(any(CommentEntity.class))).thenReturn(savedComment);
        when(activityLogConverter.toCommentLogRequest(savedComment, userId))
                .thenReturn(ActivityLogCreateRequest.builder().build());
        when(commentConverter.toCommentResponse(savedComment))
                .thenReturn(CommentResponse.builder().id(100L).content(request.getContent()).build());

        CommentResponse response = commentService.createComment(taskId, request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("@Mentioned Hello!", response.getContent());

        // Verify mention notification was sent
        verify(notificationService, times(1)).sendNotification(
                eq(mentionedUser),
                eq(user),
                anyString(),
                contains("Task Title"),
                eq(NotificationType.COMMENT),
                eq(taskId)
        );

        // Verify activity log was called
        verify(activityLogService, times(1)).log(any());
    }

    @Test
    void testCreateComment_AttachmentNotOwned_ThrowsException() {
        UserEntity otherUser = UserEntity.builder().id(99L).build();
        AttachmentEntity attachment = AttachmentEntity.builder().id(5L).uploadedBy(otherUser).build();

        CommentRequest request = CommentRequest.builder()
                .content("Hello!")
                .attachmentIds(List.of(5L))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(attachmentRepository.findById(5L)).thenReturn(Optional.of(attachment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.createComment(taskId, request));
        assertTrue(ex.getMessage().contains("You cannot use this attachment"));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void testGetComments_Success() {
        CommentEntity comment = new CommentEntity();
        comment.setId(1L);
        comment.setContent("Nice!");

        when(commentRepository.findByTaskId(taskId)).thenReturn(List.of(comment));
        when(commentConverter.toCommentResponse(comment))
                .thenReturn(CommentResponse.builder().id(1L).content("Nice!").build());

        List<CommentResponse> results = commentService.getComments(taskId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Nice!", results.get(0).getContent());
    }
}
