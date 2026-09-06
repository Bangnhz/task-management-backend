package com.example.task_management.converter;

import com.example.task_management.dto.request.RegisterRequest;
import com.example.task_management.dto.response.AuthResponse;
import com.example.task_management.dto.response.UserResponse;
import com.example.task_management.dto.response.UserSummaryResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.security.CustomUserDetails;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public UserEntity toEntity(RegisterRequest request, String encodedPassword) {
        if (request == null) return null;
        return UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(encodedPassword)
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .build();
    }

    public UserResponse toUserResponse(UserEntity entity) {
        if (entity == null) return null;
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .avatarUrl(entity.getAvatarUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UserSummaryResponse toUserSummaryResponse(UserEntity entity) {
        if (entity == null) return null;
        return UserSummaryResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .avatarUrl(entity.getAvatarUrl())
                .build();
    }

    public AuthResponse toAuthResponse(String token, UserEntity entity) {
        if (entity == null) return null;
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .avatarUrl(entity.getAvatarUrl())
                .build();
    }

    public AuthResponse toAuthResponse(String token, CustomUserDetails userDetails) {
        if (userDetails == null) return null;
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userDetails.getId())
                .email(userDetails.getEmail())
                .fullName(userDetails.getFullName())
                .avatarUrl(userDetails.getAvatarUrl())
                .build();
    }
}

