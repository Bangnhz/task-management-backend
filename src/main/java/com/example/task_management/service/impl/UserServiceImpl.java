package com.example.task_management.service.impl;

import com.example.task_management.converter.UserConverter;
import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.dto.response.UserResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.repository.ProjectMemberRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserConverter userConverter;

    @Override
    public UserResponse getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return userConverter.toUserResponse(user);
    }
}

