package com.example.task_management.controller;

import com.example.task_management.dto.request.TaskCreateRequest;
import com.example.task_management.dto.request.TaskListUpdateRequest;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.service.TaskListService;
import com.example.task_management.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task-lists")
@CrossOrigin(origins = "*")
public class TaskListController {

    @Autowired
    private TaskListService taskListService;

    @Autowired
    private TaskService taskService;

    @PutMapping("/{id}")
    public ResponseEntity<TaskListResponse> updateTaskList(
            @PathVariable("id") Long id,
            @Valid @RequestBody TaskListUpdateRequest request) {
        TaskListResponse updatedTaskList = taskListService.updateTaskList(id, request);
        return ResponseEntity.ok(updatedTaskList);
    }

    @PostMapping("/{taskListId}/tasks")
    public ResponseEntity<TaskCardResponse> createTask(
            @PathVariable("taskListId") Long taskListId,
            @Valid @RequestBody TaskCreateRequest request) {
        TaskCardResponse createdTask = taskService.createTask(taskListId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

}

