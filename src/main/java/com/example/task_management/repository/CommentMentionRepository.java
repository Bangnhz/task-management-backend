package com.example.task_management.repository;

import com.example.task_management.entity.CommentMentionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity, Long> {
}
