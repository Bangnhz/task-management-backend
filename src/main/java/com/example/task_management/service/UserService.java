package com.example.task_management.service;

import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.dto.response.UserResponse;
import java.util.List;

public interface UserService {
    public UserResponse getUserByEmail(String email);
}

