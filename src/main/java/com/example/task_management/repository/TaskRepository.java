package com.example.task_management.repository;

import com.example.task_management.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByAssigneeId(Long userId);
    List<TaskEntity> findByTaskListProjectId(Long projectId);
    List<TaskEntity> findByDueDateAndTaskListIsDoneFalse(LocalDate dueDate);
    @Query("SELECT MAX(t.position) FROM TaskEntity t WHERE t.taskList.id = :taskListId")
    Double findMaxPositionByTaskListId(@Param("taskListId") Long taskListId);
}

