package com.example.task_management.controller;

import com.example.task_management.dto.response.TaskStatisticsResponse;
import com.example.task_management.security.SecurityUtils;
import com.example.task_management.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/tasks")
    public ResponseEntity<TaskStatisticsResponse> getTaskStatisticsByUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            userId=1L;
        }
        return ResponseEntity.ok(statisticsService.getTaskStatisticsByUserId(userId));
    }
}

