package com.example.task_management.converter;

import com.example.task_management.dto.request.RegisterRequest;
import com.example.task_management.dto.response.AuthResponse;
import com.example.task_management.dto.response.UserResponse;
import com.example.task_management.dto.response.UserSummaryResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class UserConverterTest {

    private UserConverter userConverter;

    @BeforeEach
    void setUp() {
        userConverter = new UserConverter();
    }

    @Test
    void testToEntity_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .fullName("John Doe")
                .avatarUrl("avatar.png")
                .build();

        UserEntity entity = userConverter.toEntity(request, "hashed_pw");

        assertNotNull(entity);
        assertEquals("user@example.com", entity.getEmail());
        assertEquals("hashed_pw", entity.getPasswordHash());
        assertEquals("John Doe", entity.getFullName());
        assertEquals("avatar.png", entity.getAvatarUrl());
    }

    @Test
    void testToEntity_NullRequest_ReturnsNull() {
        assertNull(userConverter.toEntity(null, "pw"));
    }

    @Test
    void testToUserResponse_Success() {
        UserEntity entity = UserEntity.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .avatarUrl("avatar.png")
                .createdAt(LocalDateTime.now())
                .build();

        UserResponse response = userConverter.toUserResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
    }

    @Test
    void testToUserResponse_NullEntity_ReturnsNull() {
        assertNull(userConverter.toUserResponse(null));
    }

    @Test
    void testToUserSummaryResponse_Success() {
        UserEntity entity = UserEntity.builder()
                .id(2L)
                .fullName("Jane Doe")
                .avatarUrl("avatar2.png")
                .build();

        UserSummaryResponse response = userConverter.toUserSummaryResponse(entity);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Jane Doe", response.getFullName());
        assertEquals("avatar2.png", response.getAvatarUrl());
    }

    @Test
    void testToAuthResponse_FromEntity() {
        UserEntity entity = UserEntity.builder()
                .id(3L)
                .email("auth@example.com")
                .fullName("Auth User")
                .avatarUrl("avatar3.png")
                .build();

        AuthResponse authResponse = userConverter.toAuthResponse("jwt_token_123", entity);

        assertNotNull(authResponse);
        assertEquals("jwt_token_123", authResponse.getToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(3L, authResponse.getUserId());
        assertEquals("auth@example.com", authResponse.getEmail());
    }

    @Test
    void testToAuthResponse_FromUserDetails() {
        CustomUserDetails userDetails = new CustomUserDetails(
                4L, "details@example.com", "pw", "Details User", "avatar4.png", Collections.emptyList()
        );

        AuthResponse authResponse = userConverter.toAuthResponse("jwt_token_456", userDetails);

        assertNotNull(authResponse);
        assertEquals("jwt_token_456", authResponse.getToken());
        assertEquals(4L, authResponse.getUserId());
        assertEquals("details@example.com", authResponse.getEmail());
    }
}
