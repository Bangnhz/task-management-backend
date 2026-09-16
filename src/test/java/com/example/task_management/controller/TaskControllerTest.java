package com.example.task_management.controller;

import com.example.task_management.dto.request.CommentRequest;
import com.example.task_management.dto.request.TaskMoveRequest;
import com.example.task_management.dto.request.TaskUpdateRequest;
import com.example.task_management.dto.response.CommentResponse;
import com.example.task_management.dto.response.TaskCardResponse;
import com.example.task_management.dto.response.TaskSummaryResponse;
import com.example.task_management.exception.GlobalExceptionHandler;
import com.example.task_management.security.CustomUserDetails;
import com.example.task_management.service.CommentService;
import com.example.task_management.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private TaskController taskController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        CustomUserDetails userDetails = new CustomUserDetails(
                1L, "user@example.com", "pass", "User", null, Collections.emptyList()
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetTasksByUserId_Success() throws Exception {
        TaskSummaryResponse summary = TaskSummaryResponse.builder().id(10L).title("My Task").build();
        when(taskService.getTaskByUser(1L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("My Task"));
    }

    @Test
    void testGetTaskById_Success() throws Exception {
        TaskCardResponse card = TaskCardResponse.builder().id(10L).title("Task Detail").build();
        when(taskService.getTaskById(10L)).thenReturn(card);

        mockMvc.perform(get("/api/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Task Detail"));
    }

    @Test
    void testUpdateTask_Success() throws Exception {
        TaskUpdateRequest request = TaskUpdateRequest.builder().title("Updated").build();
        TaskCardResponse response = TaskCardResponse.builder().id(10L).title("Updated").build();

        when(taskService.updateTask(eq(10L), any(TaskUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void testDeleteTask_Success() throws Exception {
        doNothing().when(taskService).deleteTask(10L);

        mockMvc.perform(delete("/api/tasks/10"))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(10L);
    }

    @Test
    void testMoveTask_Success() throws Exception {
        TaskMoveRequest request = TaskMoveRequest.builder().targetListId(5L).position(2000L).build();
        doNothing().when(taskService).moveTask(eq(10L), any(TaskMoveRequest.class));

        mockMvc.perform(patch("/api/tasks/10/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateComment_Success() throws Exception {
        CommentRequest request = CommentRequest.builder().content("Great job").build();
        CommentResponse response = CommentResponse.builder().id(50L).content("Great job").build();

        when(commentService.createComment(eq(10L), any(CommentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tasks/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.content").value("Great job"));
    }

    @Test
    void testGetComments_Success() throws Exception {
        CommentResponse response = CommentResponse.builder().id(50L).content("A comment").build();
        when(commentService.getComments(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tasks/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].content").value("A comment"));
    }
}
