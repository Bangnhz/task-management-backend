package com.example.task_management.service;

import com.example.task_management.dto.response.TaskStatisticsResponse;

public interface StatisticsService {
    TaskStatisticsResponse getTaskStatisticsByUserId(Long userId);
}

