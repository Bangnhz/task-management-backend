package com.example.task_management.scheduler;

import com.example.task_management.entity.TaskEntity;
import com.example.task_management.enums.NotificationType;
import com.example.task_management.repository.TaskRepository;
import com.example.task_management.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDueNotificationScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    // Chạy lúc 08:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional(readOnly = true)
    public void scanAndNotifyDueSoonTasks() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<TaskEntity> dueSoonTasks = taskRepository.findByDueDateAndTaskListIsDoneFalse(tomorrow);

        for (TaskEntity task : dueSoonTasks) {
            notificationService.sendNotification(
                    task.getAssignee(),
                    null, // Do hệ thống tự động gửi (actor = null)
                    "Nhắc nhở: Task sắp đến hạn",
                    "Task \"" + task.getTitle() + "\" sẽ hết hạn vào ngày mai (" + task.getDueDate() + ")",
                    NotificationType.DUE_SOON,
                    task.getId()
            );
        }
    }
}