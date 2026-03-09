package com.deployfast.taskmanager.dto.request;

import com.deployfast.taskmanager.entity.Task;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StoreTaskRequest {

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(min = 3, max = 255, message = "Le titre doit contenir entre 3 et 255 caractères.")
    private String title;

    @Size(max = 5000, message = "La description ne peut dépasser 5000 caractères.")
    private String description;

    private Task.Status status;

    private Task.Priority priority;

    @Future(message = "La date d'échéance doit être dans le futur.")
    private LocalDate dueDate;

    private Long categoryId;

    private Long assignedTo;
}
