package com.example.task_management.service;

import com.example.task_management.dto.request.TaskCreateRequest;
import com.example.task_management.dto.request.TaskMoveRequest;
import com.example.task_management.dto.request.TaskUpdateRequest;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;

import java.util.List;

public interface TaskService {
    public List<TaskSummaryResponse> getTaskByUser(Long userId);

    public void moveTask(Long taskId, TaskMoveRequest request);

    public List<TaskCardResponse> getTasksByProjectId(Long projectId);

    public TaskCardResponse createTask(Long taskListId, TaskCreateRequest request);

    public TaskCardResponse updateTask(Long taskId, TaskUpdateRequest request);

    public TaskCardResponse getTaskById(Long taskId);

    public void deleteTask(Long taskId);
}

