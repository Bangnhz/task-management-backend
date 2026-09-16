package com.example.task_management.converter;

import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;
import com.example.task_management.dto.response.UserSummaryResponse;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.TaskEntity;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.TaskPriority;
import com.example.task_management.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskConverterTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private TaskConverter taskConverter;

    @Test
    void testToTaskSummaryResponse_Success() {
        ProjectEntity project = ProjectEntity.builder().title("Project Alpha").build();
        TaskListEntity taskList = TaskListEntity.builder().title("In Progress").project(project).build();
        TaskEntity task = TaskEntity.builder()
                .id(1L)
                .title("Complete Docs")
                .priority(TaskPriority.HIGH)
                .startDate(LocalDate.of(2026, 9, 1))
                .dueDate(LocalDate.of(2026, 9, 10))
                .taskList(taskList)
                .build();

        TaskSummaryResponse response = taskConverter.toTaskSummaryResponse(task);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Complete Docs", response.getTitle());
        assertEquals(TaskPriority.HIGH, response.getPriority());
        assertEquals("In Progress", response.getListTitle());
        assertEquals("Project Alpha", response.getProjectTitle());
    }

    @Test
    void testToTaskSummaryResponse_Null_ReturnsNull() {
        assertNull(taskConverter.toTaskSummaryResponse(null));
    }

    @Test
    void testToTaskCardResponse_Success() {
        UserEntity user = UserEntity.builder().id(5L).fullName("Bob").build();
        TaskListEntity taskList = TaskListEntity.builder().title("Done").build();
        TaskEntity task = TaskEntity.builder()
                .id(2L)
                .title("Fix Bug")
                .priority(TaskPriority.URGENT)
                .taskList(taskList)
                .assignee(user)
                .createdAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 9, 2, 12, 0))
                .build();

        when(commentRepository.countByTaskId(2L)).thenReturn(3);
        when(userConverter.toUserSummaryResponse(user))
                .thenReturn(UserSummaryResponse.builder().id(5L).fullName("Bob").build());

        TaskCardResponse response = taskConverter.toTaskCardResponse(task);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Fix Bug", response.getTitle());
        assertEquals("URGENT", response.getPriority());
        assertEquals("Done", response.getStatus());
        assertEquals(3, response.getCommentCount());
        assertNotNull(response.getAssignee());
        assertEquals("Bob", response.getAssignee().getFullName());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void testToTaskCardResponse_Null_ReturnsNull() {
        assertNull(taskConverter.toTaskCardResponse(null));
    }
}
