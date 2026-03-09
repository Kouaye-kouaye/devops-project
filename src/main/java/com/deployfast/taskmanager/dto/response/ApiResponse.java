package com.deployfast.taskmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Object errors;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }
    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder().success(false).message(message).build();
    }
    public static ApiResponse<Void> error(String message, Object errors) {
        return ApiResponse.<Void>builder().success(false).message(message).errors(errors).build();
    }
}
