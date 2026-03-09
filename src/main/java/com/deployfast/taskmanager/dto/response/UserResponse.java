package com.deployfast.taskmanager.dto.response;
import com.deployfast.taskmanager.entity.User;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private User.Role role;
    private LocalDateTime createdAt;
}
