package com.example.task_management.converter;

import com.example.task_management.dto.response.ProjectCardResponse;
import com.example.task_management.dto.response.ProjectResponse;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.TaskEntity;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.enums.ProjectVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectConverterTest {

    private ProjectConverter projectConverter;

    @BeforeEach
    void setUp() {
        projectConverter = new ProjectConverter();
    }

    @Test
    @DisplayName("toProjectResponse: Should convert valid ProjectEntity to ProjectResponse accurately")
    void testToProjectResponse_Success() {
        LocalDateTime now = LocalDateTime.now();
        ProjectEntity entity = ProjectEntity.builder()
                .id(10L)
                .title("Marketing Campaign")
                .visibility(ProjectVisibility.WORKSPACE)
                .createdAt(now)
                .build();

        ProjectResponse response = projectConverter.toProjectResponse(entity);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Marketing Campaign", response.getTitle());
        assertEquals(ProjectVisibility.WORKSPACE, response.getVisibility());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    @DisplayName("toProjectResponse: Should return null when input entity is null")
    void testToProjectResponse_NullEntity_ReturnsNull() {
        ProjectResponse response = projectConverter.toProjectResponse(null);
        assertNull(response);
    }

    @Test
    @DisplayName("toProjectCardResponse: Should accurately sum totalTasks across multiple task lists")
    void testToProjectCardResponse_Success_WithMultipleTaskListsAndTasks() {
        LocalDateTime now = LocalDateTime.now();

        TaskListEntity list1 = TaskListEntity.builder()
                .id(1L)
                .title("To Do")
                .tasks(List.of(
                        TaskEntity.builder().id(101L).build(),
                        TaskEntity.builder().id(102L).build(),
                        TaskEntity.builder().id(103L).build()
                ))
                .build();

        TaskListEntity list2 = TaskListEntity.builder()
                .id(2L)
                .title("In Progress")
                .tasks(List.of(
                        TaskEntity.builder().id(104L).build(),
                        TaskEntity.builder().id(105L).build()
                ))
                .build();

        TaskListEntity list3 = TaskListEntity.builder()
                .id(3L)
                .title("Done")
                .tasks(Collections.emptyList())
                .build();

        ProjectEntity entity = ProjectEntity.builder()
                .id(20L)
                .title("Development Sprint")
                .visibility(ProjectVisibility.PRIVATE)
                .updatedAt(now)
                .taskLists(List.of(list1, list2, list3))
                .build();

        ProjectCardResponse cardResponse = projectConverter.toProjectCardResponse(entity);

        assertNotNull(cardResponse);
        assertEquals(20L, cardResponse.getId());
        assertEquals("Development Sprint", cardResponse.getName());
        assertEquals(ProjectVisibility.PRIVATE, cardResponse.getVisibility());
        assertEquals(5L, cardResponse.getTotalTasks());
        assertEquals(now, cardResponse.getUpdatedAt());
    }

    @Test
    @DisplayName("toProjectCardResponse: Should set totalTasks = 0 when taskLists is null")
    void testToProjectCardResponse_NullTaskLists_ReturnsZeroTasks() {
        ProjectEntity entity = ProjectEntity.builder()
                .id(30L)
                .title("Empty Project")
                .visibility(ProjectVisibility.WORKSPACE)
                .taskLists(null)
                .build();

        ProjectCardResponse cardResponse = projectConverter.toProjectCardResponse(entity);

        assertNotNull(cardResponse);
        assertEquals(30L, cardResponse.getId());
        assertEquals("Empty Project", cardResponse.getName());
        assertEquals(0L, cardResponse.getTotalTasks());
    }

    @Test
    @DisplayName("toProjectCardResponse: Should set totalTasks = 0 when taskLists is empty")
    void testToProjectCardResponse_EmptyTaskLists_ReturnsZeroTasks() {
        ProjectEntity entity = ProjectEntity.builder()
                .id(40L)
                .title("New Project")
                .visibility(ProjectVisibility.WORKSPACE)
                .taskLists(Collections.emptyList())
                .build();

        ProjectCardResponse cardResponse = projectConverter.toProjectCardResponse(entity);

        assertNotNull(cardResponse);
        assertEquals(40L, cardResponse.getId());
        assertEquals(0L, cardResponse.getTotalTasks());
    }

    @Test
    @DisplayName("toProjectCardResponse: Should safely handle TaskListEntity with null tasks collection")
    void testToProjectCardResponse_TaskListWithNullTasks_ReturnsZeroTasks() {
        TaskListEntity listWithNullTasks = TaskListEntity.builder()
                .id(1L)
                .title("Backlog")
                .tasks(null)
                .build();

        ProjectEntity entity = ProjectEntity.builder()
                .id(50L)
                .title("Project with Null Tasks List")
                .visibility(ProjectVisibility.PUBLIC)
                .taskLists(List.of(listWithNullTasks))
                .build();

        ProjectCardResponse cardResponse = projectConverter.toProjectCardResponse(entity);

        assertNotNull(cardResponse);
        assertEquals(50L, cardResponse.getId());
        assertEquals(0L, cardResponse.getTotalTasks());
    }
}
