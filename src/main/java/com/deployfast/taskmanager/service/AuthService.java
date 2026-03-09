package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.request.LoginRequest;
import com.deployfast.taskmanager.dto.request.RegisterRequest;
import com.deployfast.taskmanager.dto.response.AuthResponse;
import com.deployfast.taskmanager.dto.response.UserResponse;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.exception.BusinessException;
import com.deployfast.taskmanager.exception.ResourceAlreadyExistsException;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification :
 * - Inscription avec validation du mot de passe
 * - Connexion avec génération de tokens JWT
 * - Rafraîchissement du token
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${application.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ─── Inscription ──────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validatePasswordsMatch(request.getPassword(), request.getPasswordConfirmation());
        ensureEmailNotTaken(request.getEmail());

        User user = createUser(request);
        log.info("Nouvel utilisateur inscrit : {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ─── Connexion ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Spring Security lève BadCredentialsException si invalide
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        log.info("Connexion réussie pour : {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ─── Rafraîchissement du token ────────────────────────────────────────────

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtTokenProvider.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtTokenProvider.isTokenValid(refreshToken, userDetails)) {
            throw new BusinessException("Token de rafraîchissement invalide ou expiré.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        return buildAuthResponse(user);
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private void validatePasswordsMatch(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new BusinessException("Les mots de passe ne correspondent pas.");
        }
    }

    private void ensureEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException(
                    "Un compte existe déjà avec l'adresse email : " + email);
        }
    }

    private User createUser(RegisterRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpirationMs / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
