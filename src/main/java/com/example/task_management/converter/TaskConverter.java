package com.example.task_management.converter;

import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;
import com.example.task_management.entity.TaskEntity;
import com.example.task_management.repository.CommentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class TaskConverter {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserConverter userConverter;

    public TaskSummaryResponse toTaskSummaryResponse(TaskEntity task) {
        if (task == null) return null;
        return TaskSummaryResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .priority(task.getPriority())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .listTitle(task.getTaskList() != null ? task.getTaskList().getTitle() : null)
                .projectTitle(task.getTaskList() != null && task.getTaskList().getProject() != null ? task.getTaskList().getProject().getTitle() : null)
                .build();
    }

    public TaskCardResponse toTaskCardResponse(TaskEntity task) {
        if (task == null)
            return null;

        int commentCount = commentRepository.countByTaskId(task.getId());

        return TaskCardResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .status(task.getTaskList() != null ? task.getTaskList().getTitle() : null)
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .assignee(userConverter.toUserSummaryResponse(task.getAssignee()))
                .commentCount(commentCount)
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                        : null)
                .updatedAt(task.getUpdatedAt() != null ? task.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()
                        : null)
                .build();
    }
}

