package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.entity.Task;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.repository.TaskRepository;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration - CRUD Tâches
 * Couvrent toutes les opérations REST avec contrôle d'accès.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TaskRepository taskRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private static final String BASE_URL = "/api/v1/tasks";

    private User owner;
    private User otherUser;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("owner@test.io");
        otherUser = createUser("other@test.io");
        ownerToken = generateToken(owner);
        otherToken = generateToken(otherUser);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Création d'une tâche valide → 201 + données retournées")
    void createTask_withValidData_returns201() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Mettre en place CI/CD",
                                "priority", "HIGH",
                                "dueDate", LocalDate.now().plusDays(7).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Mettre en place CI/CD")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.priority", is("HIGH")));
    }

    @Test
    @DisplayName("Création sans titre → 422 + erreur de validation")
    void createTask_withoutTitle_returns422() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("priority", "LOW"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Lister ses tâches avec pagination → 200 + page")
    void listTasks_returnsOwnedTasksPaginated() throws Exception {
        createTaskInDb("Tâche 1", owner);
        createTaskInDb("Tâche 2", owner);
        createTaskInDb("Tâche autre", otherUser);   // Ne doit pas apparaître

        mockMvc.perform(get(BASE_URL + "?page=0&size=10")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @DisplayName("Voir une tâche qui appartient à soi → 200")
    void showTask_ownedByCurrentUser_returns200() throws Exception {
        Task task = createTaskInDb("Ma tâche", owner);

        mockMvc.perform(get(BASE_URL + "/" + task.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(task.getId().intValue())));
    }

    @Test
    @DisplayName("Voir la tâche d'un autre utilisateur → 403 Forbidden")
    void showTask_notOwned_returns403() throws Exception {
        Task task = createTaskInDb("Tâche privée", otherUser);

        mockMvc.perform(get(BASE_URL + "/" + task.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tâche inexistante → 404 Not Found")
    void showTask_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Mettre à jour sa propre tâche → 200 + données modifiées")
    void updateTask_ownedByCurrentUser_returns200() throws Exception {
        Task task = createTaskInDb("Ancienne tâche", owner);

        mockMvc.perform(put(BASE_URL + "/" + task.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Tâche modifiée",
                                "status", "IN_PROGRESS"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("Tâche modifiée")))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("Modifier la tâche d'un autre → 403 Forbidden")
    void updateTask_notOwned_returns403() throws Exception {
        Task task = createTaskInDb("Tâche d'un autre", otherUser);

        mockMvc.perform(put(BASE_URL + "/" + task.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Hack"))))
                .andExpect(status().isForbidden());
    }

    // ─── COMPLETE ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Marquer une tâche comme terminée → 200 + status COMPLETED")
    void completeTask_returns200WithCompletedStatus() throws Exception {
        Task task = createTaskInDb("À terminer", owner);

        mockMvc.perform(patch(BASE_URL + "/" + task.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.completedAt", notNullValue()));
    }

    @Test
    @DisplayName("Marquer une tâche déjà terminée → 400 Bad Request")
    void completeTask_alreadyCompleted_returns400() throws Exception {
        Task task = createTaskInDb("Déjà terminée", owner);
        task.setStatus(Task.Status.COMPLETED);
        taskRepository.save(task);

        mockMvc.perform(patch(BASE_URL + "/" + task.getId() + "/complete")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Supprimer sa propre tâche → 204 No Content")
    void deleteTask_ownedByCurrentUser_returns204() throws Exception {
        Task task = createTaskInDb("À supprimer", owner);

        mockMvc.perform(delete(BASE_URL + "/" + task.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Supprimer la tâche d'un autre → 403 Forbidden")
    void deleteTask_notOwned_returns403() throws Exception {
        Task task = createTaskInDb("Tâche protégée", otherUser);

        mockMvc.perform(delete(BASE_URL + "/" + task.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Utilisateur Test")
                .email(email)
                .password(passwordEncoder.encode("Test@1234"))
                .role(User.Role.USER)
                .build());
    }

    private Task createTaskInDb(String title, User user) {
        return taskRepository.save(Task.builder()
                .user(user)
                .title(title)
                .status(Task.Status.PENDING)
                .priority(Task.Priority.MEDIUM)
                .build());
    }

    private String generateToken(User user) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return jwtTokenProvider.generateAccessToken(userDetails);
    }
}
