package com.deployfast.taskmanager.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "L'adresse email est invalide.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Le mot de passe doit contenir une majuscule, une minuscule, un chiffre et un caractère spécial.")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire.")
    private String passwordConfirmation;
}
