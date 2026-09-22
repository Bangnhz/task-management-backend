package com.example.task_management.controller;

import com.example.task_management.dto.request.CommentRequest;
import com.example.task_management.dto.request.TaskMoveRequest;
import com.example.task_management.dto.request.TaskUpdateRequest;
import com.example.task_management.dto.response.CommentResponse;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.CommentService;
import com.example.task_management.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;
    @Autowired
    private CommentService commentService;

    @GetMapping({"", "/my"})
    public ResponseEntity<List<TaskSummaryResponse>> getTasksByUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TaskSummaryResponse> tasks = taskService.getTaskByUser(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskCardResponse> getTaskById(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskCardResponse> updateTask(@PathVariable("taskId") Long taskId,
            @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable("taskId") Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/move")
    public ResponseEntity<Void> moveTask(@PathVariable("taskId") Long taskId,
            @RequestBody TaskMoveRequest request) {
        taskService.moveTask(taskId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable("taskId") Long taskId,
            @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(taskId, request));
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> getComment(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId));
    }
}