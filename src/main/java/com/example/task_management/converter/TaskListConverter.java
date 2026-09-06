package com.example.task_management.converter;

import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskListResponse;
import com.example.task_management.entity.TaskListEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskListConverter {

    @Autowired
    private TaskConverter taskConverter;

    public TaskListResponse toTaskListResponse(TaskListEntity entity) {
        if (entity == null) {
            return null;
        }

        List<TaskCardResponse> tasks = entity.getTasks() != null
                ? entity.getTasks().stream()
                        .map(taskConverter::toTaskCardResponse)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return TaskListResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
                .title(entity.getTitle())
                .position(entity.getPosition())
                .isDone(entity.getIsDone())
                .tasks(tasks)
                .build();
    }
}

