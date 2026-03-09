package com.deployfast.taskmanager.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format email invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    private String password;
}
