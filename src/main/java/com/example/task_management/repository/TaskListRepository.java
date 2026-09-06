package com.example.task_management.repository;

import com.example.task_management.entity.TaskListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskListRepository extends JpaRepository<TaskListEntity, Long> {
    List<TaskListEntity> findByProjectIdOrderByPositionAsc(Long projectId);

    @Query("SELECT MAX(tl.position) FROM TaskListEntity tl WHERE tl.project.id = :projectId")
    Double findMaxPositionByProjectId(@Param("projectId") Long projectId);

    Optional<TaskListEntity> findByProjectIdAndIsDoneTrueAndIdNot(Long projectId, Long taskId);

    Optional<TaskListEntity> findByProjectIdAndIsDoneTrue(Long projectId);
}


