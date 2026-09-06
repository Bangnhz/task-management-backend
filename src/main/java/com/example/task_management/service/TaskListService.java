package com.example.task_management.service;

import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.dto.request.TaskListUpdateRequest;
import com.example.task_management.dto.response.TaskListResponse;

import java.util.List;

public interface TaskListService {
    List<TaskListResponse> getTaskListsByProjectId(Long projectId);

    TaskListResponse createTaskList(Long projectId, TaskListCreateRequest request);

    TaskListResponse updateTaskList(Long id, TaskListUpdateRequest request);
}


