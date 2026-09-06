package com.example.task_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCreateRequest {

    @NotNull(message = "ID danh sách (cột) không được để trống")
    private Long listId;

    @NotBlank(message = "Tiêu đề task không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;
    private String description;

    @Builder.Default
    private String priority = "MEDIUM";

    private LocalDate startDate;
    private LocalDate dueDate;
    private Long assigneeId;
}
