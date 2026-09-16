package com.example.task_management.service.impl;

import com.example.task_management.converter.WorkspaceConverter;
import com.example.task_management.dto.request.WorkspaceCreateRequest;
import com.example.task_management.dto.response.WorkspaceResponse;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.entity.WorkspaceEntity;
import com.example.task_management.entity.WorkspaceMemberEntity;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.repository.WorkspaceMemberRepository;
import com.example.task_management.repository.WorkspaceRepository;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceConverter workspaceConverter;

    @Override
    public List<WorkspaceResponse> getAllWorkspaces() {
        List<WorkspaceEntity> workspaces = workspaceRepository.findAll();
        return workspaces.stream()
                .map(workspaceConverter::toWorkspaceResponse)
                .toList();
    }

    @Override
    public List<WorkspaceResponse> getWorkspacesByUserId(Long userId) {
        List<WorkspaceEntity> workspaces = workspaceRepository.findByUserId(userId);
        return workspaces.stream()
                .map(workspaceConverter::toWorkspaceResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
        Long userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            userId = 1L;
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WorkspaceEntity workspace = WorkspaceEntity.builder()
                .name(request.getName())
                .owner(user)
                .build();

        WorkspaceEntity savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMemberEntity member = WorkspaceMemberEntity.builder()
                .workspace(savedWorkspace)
                .user(user)
                .role(WorkspaceRole.OWNER)
                .build();

        workspaceMemberRepository.save(member);

        return workspaceConverter.toWorkspaceResponse(savedWorkspace);
    }
}


