package com.example.task_management.service;

import com.example.task_management.converter.UserConverter;
import com.example.task_management.dto.response.UserResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testGetUserByEmail_Success() {
        String email = "john@example.com";
        UserEntity user = UserEntity.builder().id(1L).email(email).fullName("John Doe").build();
        UserResponse response = UserResponse.builder().id(1L).email(email).fullName("John Doe").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userConverter.toUserResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void testGetUserByEmail_NotFound_ThrowsException() {
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserByEmail(email));
    }
}
