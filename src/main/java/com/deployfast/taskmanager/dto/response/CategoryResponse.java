package com.deployfast.taskmanager.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
}
