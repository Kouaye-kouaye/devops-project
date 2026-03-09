package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration - Authentification
 * Couvrent l'inscription, la connexion et les erreurs associées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String BASE_URL = "/api/v1/auth";
    private static final String VALID_EMAIL = "test@deployfast.io";
    private static final String VALID_PASSWORD = "Test@1234";

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // ─── INSCRIPTION ──────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Inscription réussie avec données valides → 201 Created + token")
    void register_withValidData_returns201AndToken() throws Exception {
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Jean Dupont",
                                "email", VALID_EMAIL,
                                "password", VALID_PASSWORD,
                                "passwordConfirmation", VALID_PASSWORD
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is(VALID_EMAIL)));
    }

    @Test
    @Order(2)
    @DisplayName("Inscription échoue si email déjà utilisé → 409 Conflict")
    void register_withDuplicateEmail_returns409() throws Exception {
        createTestUser(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Autre Nom",
                                "email", VALID_EMAIL,
                                "password", VALID_PASSWORD,
                                "passwordConfirmation", VALID_PASSWORD
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @Order(3)
    @DisplayName("Inscription échoue si mot de passe ne respecte pas la politique → 422")
    void register_withWeakPassword_returns422() throws Exception {
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Jean",
                                "email", "jean@test.io",
                                "password", "motdepasse",     // Pas de majuscule/chiffre/spécial
                                "passwordConfirmation", "motdepasse"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @Order(4)
    @DisplayName("Inscription échoue si mots de passe ne correspondent pas → 400")
    void register_withMismatchedPasswords_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Jean",
                                "email", "jean@test.io",
                                "password", VALID_PASSWORD,
                                "passwordConfirmation", "Autre@5678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── CONNEXION ────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Connexion réussie avec identifiants valides → 200 + accessToken")
    void login_withValidCredentials_returns200AndToken() throws Exception {
        createTestUser(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", VALID_EMAIL,
                                "password", VALID_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")));
    }

    @Test
    @Order(6)
    @DisplayName("Connexion échoue avec mauvais mot de passe → 401")
    void login_withBadPassword_returns401() throws Exception {
        createTestUser(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", VALID_EMAIL,
                                "password", "MauvaisMotDePasse@99"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── PROTECTION DES ROUTES ────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Accès aux tâches sans token → 401 Unauthorized")
    void accessTasks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User createTestUser(String email, String rawPassword) {
        return userRepository.save(User.builder()
                .name("Test User")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(User.Role.USER)
                .build());
    }
}
