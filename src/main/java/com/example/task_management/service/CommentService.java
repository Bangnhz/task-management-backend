package com.example.task_management.service;

import com.example.task_management.dto.request.CommentRequest;
import com.example.task_management.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {
    CommentResponse createComment(Long taskId, CommentRequest request);
    List<CommentResponse> getComments(Long taskId);
}

