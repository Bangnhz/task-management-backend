package com.example.task_management.service.impl;

import com.example.task_management.converter.UserConverter;
import com.example.task_management.dto.request.LoginRequest;
import com.example.task_management.dto.request.RegisterRequest;
import com.example.task_management.dto.response.AuthResponse;
import com.example.task_management.dto.response.UserResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.security.JwtUtils;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserConverter userConverter;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        UserEntity user = userConverter.toEntity(request, passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtUtils.generateTokenFromEmail(user.getEmail(), user.getId());
        return userConverter.toAuthResponse(token, user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userConverter.toAuthResponse(jwt, userDetails);
    }

    @Override
    public UserResponse getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return userConverter.toUserResponse(user);
    }
}

