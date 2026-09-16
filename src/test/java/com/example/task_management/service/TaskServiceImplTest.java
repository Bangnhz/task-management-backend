package com.example.task_management.service;

import com.example.task_management.converter.ActivityLogConverter;
import com.example.task_management.converter.TaskConverter;
import com.example.task_management.dto.request.ActivityLogCreateRequest;
import com.example.task_management.dto.request.TaskCreateRequest;
import com.example.task_management.dto.request.TaskMoveRequest;
import com.example.task_management.dto.request.TaskUpdateRequest;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;
import com.example.task_management.entity.ProjectEntity;
import com.example.task_management.entity.TaskEntity;
import com.example.task_management.entity.TaskListEntity;
import com.example.task_management.entity.UserEntity;
import com.example.task_management.enums.ActivityAction;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.enums.TaskPriority;
import com.example.task_management.repository.TaskListRepository;
import com.example.task_management.repository.TaskRepository;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskListRepository taskListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskConverter taskConverter;

    @Mock
    private ActivityLogConverter activityLogConverter;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private final Long userId = 1L;
    private final Long taskId = 100L;
    private final Long listId = 10L;
    private UserEntity user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder().id(userId).fullName("Test User").email("test@example.com").build();
        userDetails = new CustomUserDetails(userId, "test@example.com", "pass", "Test User", null, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetTaskByUser_Success() {
        ProjectEntity project = ProjectEntity.builder().id(50L).title("Sample Project").build();
        TaskListEntity taskList = TaskListEntity.builder().id(listId).title("To Do").project(project).build();
        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .title("Sample Task")
                .priority(TaskPriority.HIGH)
                .startDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(3))
                .taskList(taskList)
                .build();

        when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(task));

        List<TaskSummaryResponse> results = taskService.getTaskByUser(userId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Sample Task", results.get(0).getTitle());
        assertEquals("To Do", results.get(0).getListTitle());
        assertEquals("Sample Project", results.get(0).getProjectTitle());
    }

    @Test
    void testMoveTask_ToDifferentList_SendsNotificationAndLog() {
        TaskListEntity oldList = TaskListEntity.builder().id(10L).title("To Do").build();
        TaskListEntity targetList = TaskListEntity.builder().id(20L).title("In Progress").build();
        UserEntity assignee = UserEntity.builder().id(2L).fullName("Assignee").build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .title("Task Move")
                .position(1000.0)
                .taskList(oldList)
                .assignee(assignee)
                .build();

        TaskMoveRequest request = TaskMoveRequest.builder()
                .targetListId(20L)
                .position(2500L)
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskListRepository.findById(20L)).thenReturn(Optional.of(targetList));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(activityLogConverter.toTaskLogRequest(eq(task), eq(ActivityAction.MOVED), eq(userId), any()))
                .thenReturn(ActivityLogCreateRequest.builder().build());

        taskService.moveTask(taskId, request);

        assertEquals(2500.0, task.getPosition());
        assertEquals(targetList, task.getTaskList());
        verify(taskRepository, times(1)).save(task);
        verify(activityLogService, times(1)).log(any());
        verify(notificationService, times(1)).sendNotification(
                eq(assignee),
                eq(user),
                anyString(),
                anyString(),
                eq(NotificationType.STATUS_CHANGED),
                eq(taskId)
        );
    }

    @Test
    void testMoveTask_WithinSameList_NoNotification() {
        TaskListEntity list = TaskListEntity.builder().id(10L).title("To Do").build();
        UserEntity assignee = UserEntity.builder().id(2L).fullName("Assignee").build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .title("Task Reorder")
                .position(1000.0)
                .taskList(list)
                .assignee(assignee)
                .build();

        TaskMoveRequest request = TaskMoveRequest.builder()
                .targetListId(10L)
                .position(500L)
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskListRepository.findById(10L)).thenReturn(Optional.of(list));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        taskService.moveTask(taskId, request);

        assertEquals(500.0, task.getPosition());
        verify(taskRepository, times(1)).save(task);
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any(), any());
        verify(activityLogService, never()).log(any());
    }

    @Test
    void testMoveTask_TaskNotFound_ThrowsException() {
        TaskMoveRequest request = TaskMoveRequest.builder().targetListId(20L).build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskService.moveTask(taskId, request));
    }

    @Test
    void testMoveTask_TargetListNotFound_ThrowsException() {
        TaskEntity task = TaskEntity.builder().id(taskId).build();
        TaskMoveRequest request = TaskMoveRequest.builder().targetListId(20L).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskListRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> taskService.moveTask(taskId, request));
    }

    @Test
    void testCreateTask_Success_WithDefaultPositionAndPriority() {
        TaskListEntity list = TaskListEntity.builder().id(listId).title("To Do").build();
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("New Task")
                .description("Description")
                .assigneeId(2L)
                .priority("URGENT")
                .build();

        UserEntity assignee = UserEntity.builder().id(2L).fullName("Assignee").build();

        when(taskListRepository.findById(listId)).thenReturn(Optional.of(list));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.findMaxPositionByTaskListId(listId)).thenReturn(null); // list is empty

        TaskEntity savedTask = TaskEntity.builder().id(taskId).title("New Task").build();
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(savedTask);
        when(taskConverter.toTaskCardResponse(savedTask)).thenReturn(TaskCardResponse.builder().id(taskId).build());

        TaskCardResponse result = taskService.createTask(listId, request);

        assertNotNull(result);
        assertEquals(taskId, result.getId());

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository, times(1)).save(captor.capture());
        TaskEntity captured = captor.getValue();
        assertEquals(1000.0, captured.getPosition()); // Initial position
        assertEquals(TaskPriority.URGENT, captured.getPriority());
        assertEquals("New Task", captured.getTitle());
        assertEquals(assignee, captured.getAssignee());
        assertEquals(user, captured.getCreatedBy());
        verify(activityLogService, times(1)).log(any());
    }

    @Test
    void testCreateTask_WithExistingMaxPosition_DefaultMediumPriority() {
        TaskListEntity list = TaskListEntity.builder().id(listId).title("To Do").build();
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Second Task")
                .priority(null)
                .build();

        when(taskListRepository.findById(listId)).thenReturn(Optional.of(list));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(taskRepository.findMaxPositionByTaskListId(listId)).thenReturn(2000.0);

        TaskEntity savedTask = TaskEntity.builder().id(taskId).build();
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(savedTask);
        when(taskConverter.toTaskCardResponse(savedTask)).thenReturn(TaskCardResponse.builder().id(taskId).build());

        taskService.createTask(listId, request);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskRepository, times(1)).save(captor.capture());
        TaskEntity captured = captor.getValue();
        assertEquals(3000.0, captured.getPosition()); // 2000 + 1000
        assertEquals(TaskPriority.MEDIUM, captured.getPriority());
    }

    @Test
    void testUpdateTask_PartialUpdatesAndAssigneeNotification() {
        UserEntity oldAssignee = UserEntity.builder().id(2L).fullName("Old Assignee").build();
        UserEntity newAssignee = UserEntity.builder().id(3L).fullName("New Assignee").build();
        TaskListEntity oldList = TaskListEntity.builder().id(10L).title("Old List").build();
        TaskListEntity newList = TaskListEntity.builder().id(20L).title("New List").build();

        TaskEntity task = TaskEntity.builder()
                .id(taskId)
                .title("Old Title")
                .description("Old Desc")
                .priority(TaskPriority.LOW)
                .assignee(oldAssignee)
                .taskList(oldList)
                .build();

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("New Title")
                .description("New Desc")
                .priority("HIGH")
                .assigneeId(3L)
                .listId(20L)
                .startDate(LocalDate.of(2026, 9, 1))
                .dueDate(LocalDate.of(2026, 9, 15))
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newAssignee));
        when(taskListRepository.findById(20L)).thenReturn(Optional.of(newList));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskConverter.toTaskCardResponse(any(TaskEntity.class)))
                .thenReturn(TaskCardResponse.builder().id(taskId).title("New Title").build());

        TaskCardResponse result = taskService.updateTask(taskId, request);

        assertNotNull(result);
        assertEquals("New Title", task.getTitle());
        assertEquals("New Desc", task.getDescription());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(newAssignee, task.getAssignee());
        assertEquals(newList, task.getTaskList());
        assertEquals(LocalDate.of(2026, 9, 1), task.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 15), task.getDueDate());

        // Verify notification sent to new assignee
        verify(notificationService, times(1)).sendNotification(
                eq(newAssignee),
                eq(user),
                anyString(),
                contains("New Title"),
                eq(NotificationType.ASSIGNED),
                eq(taskId)
        );

        // Verify activity log call
        verify(activityLogService, times(1)).log(any());
    }

    @Test
    void testGetTaskById_Success() {
        TaskEntity task = TaskEntity.builder().id(taskId).title("Test").build();
        TaskCardResponse response = TaskCardResponse.builder().id(taskId).title("Test").build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskConverter.toTaskCardResponse(task)).thenReturn(response);

        TaskCardResponse result = taskService.getTaskById(taskId);
        assertNotNull(result);
        assertEquals(taskId, result.getId());
    }

    @Test
    void testGetTaskById_NotFound_ThrowsException() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> taskService.getTaskById(taskId));
    }

    @Test
    void testDeleteTask_Success_WithActivityLog() {
        TaskEntity task = TaskEntity.builder().id(taskId).title("To Delete").build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(activityLogConverter.toTaskLogRequest(eq(task), eq(ActivityAction.DELETED), eq(userId), isNull()))
                .thenReturn(ActivityLogCreateRequest.builder().build());

        taskService.deleteTask(taskId);

        verify(activityLogService, times(1)).log(any());
        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void testDeleteTask_NotFound_ThrowsException() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> taskService.deleteTask(taskId));
        verify(taskRepository, never()).delete(any());
    }
}
