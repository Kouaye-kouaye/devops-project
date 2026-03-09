package com.deployfast.taskmanager.dto.response;
import com.deployfast.taskmanager.entity.Task;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Task.Status status;
    private Task.Priority priority;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private UserResponse user;
    private CategoryResponse category;
    private UserResponse assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
