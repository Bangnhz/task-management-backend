package com.example.task_management.repository;

import com.example.task_management.entity.ActivityLogEntity;
import com.example.task_management.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {
    List<ActivityLogEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ActivityLogEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(EntityType entityType, Long entityId);

    List<ActivityLogEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
