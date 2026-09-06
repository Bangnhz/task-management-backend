package com.example.task_management.service.impl;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.CommentConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.request.CommentMentionRequest;
import com.example.task_management.dto.request.CommentRequest;
import com.example.task_management.dto.response.CommentResponse;
import com.example.task_management.entity.*;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.repository.*;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ActivityLogService;
import com.example.task_management.service.CommentService;
import com.example.task_management.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CommentMentionRepository commentMentionRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private CommentConverter commentConverter;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private ActivityLogConverter activityLogConverter;
    @Autowired
    private NotificationService notificationService;
    @Override
    @Transactional
    public CommentResponse createComment(Long taskId, CommentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        TaskEntity taskEntity = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        CommentEntity commentEntity = new CommentEntity();
        commentEntity.setAuthor(userEntity);
        commentEntity.setTask(taskEntity);
        commentEntity.setContent(request.getContent());

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            for (Long attachmentId : request.getAttachmentIds()) {
                AttachmentEntity attachmentEntity = attachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new RuntimeException("Attachment not found"));
                if (!attachmentEntity.getUploadedBy().getId().equals(userId)) {
                    throw new RuntimeException("You cannot use this attachment");
                }
                commentEntity.getAttachments().add(attachmentEntity);
            }
        }

        if (request.getMentions() != null && !request.getMentions().isEmpty()) {
            for (CommentMentionRequest mention : request.getMentions()) {
                UserEntity mentionedUser = userRepository.findById(mention.getUserId())
                        .orElseThrow(() -> new RuntimeException("Mentioned user not found"));
                CommentMentionEntity commentMentionEntity = CommentMentionEntity.builder()
                        .comment(commentEntity)
                        .user(mentionedUser)
                        .startIndex(mention.getStartIndex())
                        .length(mention.getLength())
                        .build();
                commentEntity.getCommentMentions().add(commentMentionEntity);
                notificationService.sendNotification(
                        mentionedUser,
                        userEntity,
                        "Bạn được nhắc đến trong bình luận",
                        userEntity.getFullName() + " đã nhắc đến bạn trong task: " + taskEntity.getTitle(),
                        NotificationType.COMMENT,
                        taskEntity.getId()
                );
            }
        }

        CommentEntity saved = commentRepository.save(commentEntity);

        // Ghi nhận Activity Log khi tạo comment
        ActivityLogCreateRequest logRequest = activityLogConverter.toCommentLogRequest(saved, userId);
        activityLogService.log(logRequest);

        return commentConverter.toCommentResponse(saved);
    }

    @Override
    public List<CommentResponse> getComments(Long taskId) {
        List<CommentEntity> commentEntities = commentRepository.findByTaskId(taskId);
        return commentEntities.stream()
                .map(commentConverter::toCommentResponse)
                .toList();
    }
}