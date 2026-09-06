package com.example.task_management.repository;

import com.example.task_management.entity.ProjectInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitationEntity, Long> {
    Optional<ProjectInvitationEntity> findFirstByProject_IdAndCreatedBy_IdAndIsActiveTrueAndExpiresAtAfter(Long projectId, Long createdById, LocalDateTime time);
    Optional<ProjectInvitationEntity> findByToken(String token);
    Optional<ProjectInvitationEntity> findByCreatedBy_IdAndProject_IdAndIsActiveTrueAndExpiresAtAfter(Long userId, Long projectId, LocalDateTime time);
}
