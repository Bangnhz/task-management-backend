package com.example.task_management.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskMoveRequest {
    private Long targetListId;
    private Long position;
}
