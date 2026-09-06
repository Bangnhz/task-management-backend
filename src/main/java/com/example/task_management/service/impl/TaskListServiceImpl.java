package com.example.task_management.service.impl;

import com.example.task_management.converter.TaskListConverter;
import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.dto.request.TaskListUpdateRequest;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.repository.ProjectRepository;
import com.example.task_management.repository.TaskListRepository;
import com.example.task_management.service.TaskListService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private TaskListConverter taskListConverter;

    @Override
    @Transactional(readOnly = true)
    public List<TaskListResponse> getTaskListsByProjectId(Long projectId) {
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

