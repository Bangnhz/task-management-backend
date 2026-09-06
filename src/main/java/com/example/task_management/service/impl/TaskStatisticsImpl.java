package com.example.task_management.service.impl;

import com.example.task_management.dto.response.TaskStatisticsResponse;
import com.example.task_management.repository.StatisticsRepository;
import com.example.task_management.service.StatisticsService;
import org.springframework.stereotype.Service;

@Service
public class TaskStatisticsImpl implements StatisticsService {
    private StatisticsRepository statisticsRepository;

    @Override
    public TaskStatisticsResponse getTaskStatisticsByUserId(Long userId) {
        return statisticsRepository.getTaskStatistic(userId);
    }

}

