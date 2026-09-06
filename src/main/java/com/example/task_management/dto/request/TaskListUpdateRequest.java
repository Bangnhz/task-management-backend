package com.example.task_management.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskListUpdateRequest {

    @Size(max = 100, message = "Tiêu đề không được vượt quá 100 ký tự")
    private String title;
    private Boolean isDone;
}