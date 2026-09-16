package com.example.task_management.controller;

import com.example.task_management.dto.request.TaskListCreateRequest;
import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.ProjectService;
import com.example.task_management.service.TaskListService;
import com.example.task_management.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskListService taskListService;

    @GetMapping("/me")
    public ResponseEntity<Page<ProjectCardResponse>> getMyProjects(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "updatedAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction
    ) {
        Long userId = SecurityUtils.getCurrentUserId();

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProjectCardResponse> projects = projectService.getProjectSummariesByUserId(userId, pageable);
        return ResponseEntity.ok(projects);
    }

//    @GetMapping
//    public ResponseEntity<List<ProjectCardResponse>> getProjects() {
//        Long userId = SecurityUtils.getCurrentUserId();
//        List<ProjectCardResponse> projects = projectService.getProjectSummariesByUserId(userId);
//        return ResponseEntity.ok(projects);
//    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@projectSecurity.canAccessProject(#projectId)")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable("projectId") Long projectId) {
        ProjectResponse project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/{projectId}/tasks")
    @PreAuthorize("@projectSecurity.canAccessProject(#projectId)")
    public ResponseEntity<List<TaskCardResponse>> getTasksByProjectId(@PathVariable("projectId") Long projectId) {
        List<TaskCardResponse> tasks = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }

    // Project detail
    @GetMapping("/{projectId}/task-lists")
    @PreAuthorize("@projectSecurity.canAccessProject(#projectId)")
    public ResponseEntity<List<TaskListResponse>> getTaskListsByProjectId(@PathVariable("projectId") Long projectId) {
        System.out.println("ProjectId create" + projectId);
        List<TaskListResponse> taskLists = taskListService.getTaskListsByProjectId(projectId);
        return ResponseEntity.ok(taskLists);
    }

    @PostMapping("/{projectId}/task-lists")
    public ResponseEntity<TaskListResponse> createTaskList(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody TaskListCreateRequest request) {
        TaskListResponse createdTaskList = taskListService.createTaskList(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTaskList);
    }
}

