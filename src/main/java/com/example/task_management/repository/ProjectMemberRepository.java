package com.example.task_management.repository;

import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.entity.ProjectMemberEntity;
import com.example.task_management.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, Long> {
    Optional<ProjectMemberEntity> findByProjectIdAndUserId(Long projectId, Long currentUserId);
    Optional<ProjectMemberEntity> findByIdAndProjectId(Long memberId, Long projectId);
    List<ProjectMemberEntity> findByProjectIdAndStatus(Long projectId, MemberStatus status);

    @Query("SELECT DISTINCT new com.example.task_management.dto.response.UserMentionResponse(" +
            "u.id, u.fullName, u.email, u.avatarUrl) " +
            "FROM ProjectMemberEntity pm " +
            "JOIN pm.user u " +
            "WHERE pm.project.id = :projectId " +
            "AND pm.status = com.example.task_management.enums.MemberStatus.ACTIVE " +
            "AND (:query IS NULL OR :query = '' OR " +
            "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<UserMentionResponse> searchMembersForMention(@Param("projectId") Long projectId, @Param("query") String query);
}
