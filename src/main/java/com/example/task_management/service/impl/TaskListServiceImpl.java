package com.example.task_management.service.impl;

import com.example.task_management.converter.TaskListConverter;
import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.dto.request.TaskListUpdateRequest;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.entity.*;
import com.example.task_management.enums.MemberStatus;
import com.example.task_management.enums.ProjectVisibility;
import com.example.task_management.enums.WorkspaceRole;
import com.example.task_management.repository.*;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.TaskListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class TaskListServiceImpl implements TaskListService {

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private TaskListConverter taskListConverter;

    @Override
    @Transactional(readOnly = true)
    public List<TaskListResponse> getTaskListsByProjectId(Long projectId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Kiểm tra sự tồn tại của Project trước
        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // 2. Kiểm tra thành viên trực tiếp trong ProjectMember (bao gồm Owner) có status == ACTIVE
        Optional<ProjectMemberEntity> projectMember =
                projectMemberRepository.findByProjectIdAndUserId(projectId, userId);

        if (projectMember.isPresent() && projectMember.get().getStatus() == MemberStatus.ACTIVE) {
            return fetchAndConvertTaskLists(projectId);
        }

        // 3. Kiểm tra nếu là Public
        if (ProjectVisibility.PUBLIC.equals(projectEntity.getVisibility())) {
            return fetchAndConvertTaskLists(projectId);
        }

        // 4. Kiểm tra nếu là Workspace Visibility VÀ User là thành viên thuộc Workspace đó với status == ACTIVE và role != GUEST
        if (ProjectVisibility.WORKSPACE.equals(projectEntity.getVisibility()) && projectEntity.getWorkspace() != null) {
            Optional<WorkspaceMemberEntity> workspaceMember =
                    workspaceMemberRepository.findByWorkspaceIdAndUserId(projectEntity.getWorkspace().getId(), userId);

            if (workspaceMember.isPresent()
                    && workspaceMember.get().getStatus() == MemberStatus.ACTIVE
                    && workspaceMember.get().getRole() != WorkspaceRole.GUEST) {
                return fetchAndConvertTaskLists(projectId);
            }
        }

        // 5. Chặn ngay lập tức nếu không thỏa mãn bất kỳ điều kiện nào (Guard Clause)
        throw new AccessDeniedException("Bạn không có quyền truy cập vào danh sách công việc của dự án này");
    }

    private List<TaskListResponse> fetchAndConvertTaskLists(Long projectId) {
        List<TaskListEntity> taskLists = taskListRepository.findByProjectIdOrderByPositionAsc(projectId);
        return taskLists.stream()
                .map(taskListConverter::toTaskListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskListResponse createTaskList(Long projectId, TaskListCreateRequest request) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không tồn tại với ID: " + projectId));

        Double maxPos = taskListRepository.findMaxPositionByProjectId(projectId);
        Double position = (maxPos != null) ? maxPos + 1000.0 : 1000.0;

        boolean isDone = Boolean.TRUE.equals(request.getIsDone());
        if (isDone) {
            taskListRepository.findByProjectIdAndIsDoneTrue(projectId)
                    .ifPresent(currentDoneList -> {
                        currentDoneList.setIsDone(false);
                        taskListRepository.save(currentDoneList);
                    });
        }

        TaskListEntity taskListEntity = TaskListEntity.builder()
                .project(project)
                .title(request.getTitle())
                .position(position)
                .isDone(isDone)
                .build();

        TaskListEntity savedTaskList = taskListRepository.save(taskListEntity);
        return taskListConverter.toTaskListResponse(savedTaskList);
    }

    @Override
    @Transactional
    public TaskListResponse updateTaskList(Long id, TaskListUpdateRequest request) {
        TaskListEntity taskListEntity = taskListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh sách công việc không tồn tại với ID: " + id));

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            taskListEntity.setTitle(request.getTitle());
        }

        if (request.getIsDone() != null) {
            if (Boolean.TRUE.equals(request.getIsDone())) {
                taskListRepository.findByProjectIdAndIsDoneTrueAndIdNot(taskListEntity.getProject().getId(), id)
                        .ifPresent(currentDoneList -> {
                            currentDoneList.setIsDone(false);
                            taskListRepository.save(currentDoneList);
                        });
            }
            taskListEntity.setIsDone(request.getIsDone());
        }

        TaskListEntity updatedTaskList = taskListRepository.save(taskListEntity);
        return taskListConverter.toTaskListResponse(updatedTaskList);
    }
}

