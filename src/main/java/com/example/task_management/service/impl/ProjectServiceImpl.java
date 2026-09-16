package com.example.task_management.service.impl;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.ProjectConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.request.ProjectCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import com.example.task_management.entity.*;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectRole;
import com.example.task_management.repository.*;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ActivityLogService;
import com.example.task_management.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectConverter projectConverter;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    @Autowired
    private TaskListRepository taskListRepository;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private ActivityLogConverter activityLogConverter;

    @Override
    public Page<ProjectCardResponse> getProjectSummariesByUserId(Long userId, Pageable pageable) {
        if(userId == null){
            throw new RuntimeException("UserId is null");
        }
        return projectRepository.getProjectSummariesByUserId(userId,pageable);
    }

    @Override
    public List<ProjectResponse> getProjectsByWorkspaceId(Long workspaceId) {
        List<ProjectEntity> projects = projectRepository.findByWorkspaceId(workspaceId);
        return projects.stream()
                .map(projectConverter::toProjectResponse)
                .toList();
    }

    @Override
    public List<ProjectCardResponse> getProjectSummariesByWorkspaceId(Long workspaceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return projectRepository.getProjectSummariesByWorkspaceId(workspaceId, userId);
    }

    @Override
    @Transactional
    public ProjectCardResponse createProject(Long workspaceId, ProjectCreateRequest projectCreateRequest) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            userId = 1L;
        }

        WorkspaceEntity workspaceEntity = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));

        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setTitle(projectCreateRequest.getTitle());
        projectEntity.setVisibility(projectCreateRequest.getVisibility());
        projectEntity.setWorkspace(workspaceEntity);

        ProjectEntity saved = projectRepository.save(projectEntity);

        List<String> defaultListTitles = List.of("Backlog", "To Do", "In Progress", "Done");
        List<TaskListEntity> taskListEntities = new ArrayList<>();
        double initialPosition = 65536.0;
        for (int i = 0; i < defaultListTitles.size(); i++) {
            TaskListEntity taskList = new TaskListEntity();
            taskList.setTitle(defaultListTitles.get(i));
            taskList.setPosition(initialPosition * (i + 1));
            taskList.setProject(saved);
            taskList.setTasks(new ArrayList<>());
            taskListEntities.add(taskList);
        }
        taskListRepository.saveAll(taskListEntities);

        ProjectMemberEntity memberEntity = new ProjectMemberEntity();
        memberEntity.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found!")));
        memberEntity.setProject(saved);
        memberEntity.setRole(ProjectRole.OWNER);
        memberEntity.setStatus(MemberStatus.ACTIVE);
        projectMemberRepository.save(memberEntity);

        // Ghi log tạo project mới
        ActivityLogCreateRequest logRequest = activityLogConverter.toProjectLogRequest(
                saved,
                ActivityAction.CREATED,
                userId
        );
        activityLogService.log(logRequest);

        return projectConverter.toProjectCardResponse(saved);
    }

    @Override
    public ProjectResponse getProjectById(Long projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        return projectConverter.toProjectResponse(project);
    }
}