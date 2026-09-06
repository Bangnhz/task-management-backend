package com.example.task_management.repository;

import com.example.task_management.entity.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {

    @Query("SELECT DISTINCT w FROM WorkspaceEntity w JOIN w.members m WHERE m.user.id = :userId")
    List<WorkspaceEntity> findByUserId(@Param("userId") Long userId);
}

