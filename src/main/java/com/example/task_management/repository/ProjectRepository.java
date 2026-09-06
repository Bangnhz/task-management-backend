package com.example.task_management.repository;

import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findByWorkspaceId(Long workspaceId);


    @Query("""
        SELECT new com.example.task_management.dto.response.ProjectCardResponse(
            p.id,
            p.title,
            COUNT(t.id),
            p.visibility,
            p.updatedAt
            
        )
        FROM ProjectEntity p
        LEFT JOIN p.taskLists tl
        LEFT JOIN tl.tasks t
        WHERE p.workspace.id = :workspaceId
        GROUP BY p.id, p.title
    """)
    List<ProjectCardResponse> getProjectSummariesByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query(value= """
    SELECT new com.example.task_management.dto.response.ProjectCardResponse(
        p.id,
        p.title,
        COUNT(t.id),
        p.visibility,
        p.updatedAt
    )
    FROM ProjectEntity p
    LEFT JOIN p.taskLists tl
    LEFT JOIN tl.tasks t
    WHERE EXISTS (
        SELECT 1
        FROM WorkspaceMemberEntity wm
        WHERE wm.workspace.id = p.workspace.id
        AND wm.user.id = :userId
    )
    GROUP BY p.id, p.title
""",    countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM ProjectEntity p
        WHERE EXISTS (
            SELECT 1
            FROM WorkspaceMemberEntity wm
            WHERE wm.workspace.id = p.workspace.id
            AND wm.user.id = :userId
        )
    """
    )
    Page<ProjectCardResponse> getProjectSummariesByUserId(@Param("userId") Long userId, Pageable pageable);
}

