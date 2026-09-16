package com.example.task_management.service;

import com.example.task_management.converter.UserConverter;
import com.example.task_management.dto.request.LoginRequest;
import com.example.task_management.dto.request.RegisterRequest;
import com.example.task_management.dto.response.AuthResponse;
import com.example.task_management.dto.response.UserResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.security.JwtUtils;
import com.example.task_management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserEntity userEntity;
    private AuthResponse authResponse;
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .fullName("Test User")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        userEntity = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .fullName("Test User")
                .build();

        authResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .tokenType("Bearer")
                .userId(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        customUserDetails = new CustomUserDetails(
                1L, "test@example.com", "encodedPassword", "Test User", null, Collections.emptyList()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRegister_Success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userConverter.toEntity(registerRequest, "encodedPassword")).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(jwtUtils.generateTokenFromEmail("test@example.com", 1L)).thenReturn("mock-jwt-token");
        when(userConverter.toAuthResponse("mock-jwt-token", userEntity)).thenReturn(authResponse);

        AuthResponse result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("mock-jwt-token", result.getToken());
        verify(userRepository, times(1)).save(userEntity);
    }

    @Test
    void testRegister_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        assertTrue(exception.getMessage().contains("Email is already in use"));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void testLogin_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("mock-jwt-token");
        when(userConverter.toAuthResponse("mock-jwt-token", customUserDetails)).thenReturn(authResponse);

        AuthResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals(1L, result.getUserId());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testLogin_BadCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testGetCurrentUser_Success() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userConverter.toUserResponse(userEntity)).thenReturn(userResponse);

        UserResponse result = authService.getCurrentUser();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testGetCurrentUser_UserNotFound_ThrowsException() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getCurrentUser());
    }
}
