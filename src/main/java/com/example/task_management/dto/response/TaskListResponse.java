package com.example.task_management.dto.response;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskListResponse {
    private Long id;
    private Long projectId;
    private String title;
    private Double position;
    private Boolean isDone;

    @Builder.Default
    private List<TaskCardResponse> tasks = new ArrayList<>();
}
