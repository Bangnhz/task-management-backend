package com.example.task_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(
        name = "task_lists",
        indexes = {
                @Index(name = "idx_task_lists_project_position", columnList = "project_id, position")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @OneToMany(mappedBy = "taskList")
    private List<TaskEntity> tasks;
    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Double position;

    @Column(name = "is_done", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isDone = false;
}