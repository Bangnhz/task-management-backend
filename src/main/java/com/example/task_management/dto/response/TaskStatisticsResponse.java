package com.example.task_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatisticsResponse {

    private Long myTasks;
    private Long inProgress;
    private Long dueSoon;
    private Long completed;
}
