package com.example.task_management.repository;

import com.example.task_management.dto.response.TaskStatisticsResponse;
import com.example.task_management.entity.TaskEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StatisticsRepository {

    @Query("""
        SELECT new com.example.task_management.dto.response.TaskStatisticsResponse(
            COUNT(t.id),
            SUM(CASE 
                WHEN tl.isDone is false
                THEN 1L ELSE 0L 
            END),
            SUM(CASE 
                WHEN t.dueDate IS NOT NULL 
                 AND t.dueDate >= CURRENT_TIMESTAMP
                THEN 1L ELSE 0L 
            END),
            SUM(CASE 
                WHEN tl.isDone is true
                THEN 1L ELSE 0L 
            END)
        )
        FROM TaskEntity t
        JOIN t.taskList tl
        WHERE t.assignee.id = :userId
    """)
    TaskStatisticsResponse getTaskStatistic(@Param("userId") Long userId);
}
