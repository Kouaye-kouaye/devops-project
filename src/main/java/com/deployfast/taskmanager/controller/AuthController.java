package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.dto.request.LoginRequest;
import com.deployfast.taskmanager.dto.request.RegisterRequest;
import com.deployfast.taskmanager.dto.response.ApiResponse;
import com.deployfast.taskmanager.dto.response.AuthResponse;
import com.deployfast.taskmanager.dto.response.UserResponse;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur d'authentification.
 * Routes : POST /api/v1/auth/register|login|logout|refresh + GET /api/v1/auth/me
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * POST /api/v1/auth/register
     * Inscription d'un nouvel utilisateur.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.ok("Compte créé avec succès.", response);
    }

    /**
     * POST /api/v1/auth/login
     * Connexion et retour du token JWT.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.ok("Connexion réussie.", response);
    }

    /**
     * POST /api/v1/auth/refresh
     * Rafraîchissement du token d'accès.
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ApiResponse.ok("Token rafraîchi.", response);
    }

    /**
     * GET /api/v1/auth/me
     * Retourne le profil de l'utilisateur authentifié.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow();
        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
        return ApiResponse.ok(response);
    }
}
