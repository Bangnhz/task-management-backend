package com.example.task_management.controller;

import com.example.task_management.dto.response.NotificationResponse;
import com.example.task_management.exception.GlobalExceptionHandler;
import com.example.task_management.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetMyNotifications_Success() throws Exception {
        NotificationResponse resp = NotificationResponse.builder().id(1L).title("Test Notice").build();
        when(notificationService.getMyNotifications()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Notice"));
    }

    @Test
    void testGetUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(7L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(7));
    }

    @Test
    void testMarkAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAsRead(10L);

        mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).markAsRead(10L);
    }

    @Test
    void testMarkAllAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).markAllAsRead();
    }
}
