package com.example.task_management.repository;

import com.example.task_management.dto.response.UserMentionResponse;
import com.example.task_management.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<UserEntity> findById(Long id);

    @Query("SELECT new com.example.task_management.dto.response.UserMentionResponse(" +
            "u.id, u.fullName, u.email, u.avatarUrl) " +
            "FROM UserEntity u " +
            "WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<UserMentionResponse> searchForMention(@Param("query") String query);
}

