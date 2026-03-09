package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.request.StoreTaskRequest;
import com.deployfast.taskmanager.dto.request.UpdateTaskRequest;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.entity.Task;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.exception.BusinessException;
import com.deployfast.taskmanager.exception.ResourceNotFoundException;
import com.deployfast.taskmanager.repository.CategoryRepository;
import com.deployfast.taskmanager.repository.TaskRepository;
import com.deployfast.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de TaskService.
 * Utilise Mockito pour isoler le service de la couche persistance.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;

    @InjectMocks TaskService taskService;

    private User owner;
    private User otherUser;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("owner@test.io").role(User.Role.USER).name("Owner").build();
        otherUser = User.builder().id(2L).email("other@test.io").role(User.Role.USER).name("Other").build();
        sampleTask = Task.builder()
                .id(10L).title("Tâche de test")
                .user(owner)
                .status(Task.Status.PENDING)
                .priority(Task.Priority.MEDIUM)
                .build();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() — assigne le user courant et valeurs par défaut")
    void create_assignsCurrentUserAndDefaults() {
        StoreTaskRequest request = new StoreTaskRequest();
        request.setTitle("Nouvelle tâche");

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        TaskResponse response = taskService.create(owner, request);

        assertThat(response.getTitle()).isEqualTo("Nouvelle tâche");
        assertThat(response.getStatus()).isEqualTo(Task.Status.PENDING);
        assertThat(response.getPriority()).isEqualTo(Task.Priority.MEDIUM);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById() — retourne la tâche si elle appartient au user")
    void getById_ownedByUser_returnsTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        TaskResponse response = taskService.getById(10L, owner);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Tâche de test");
    }

    @Test
    @DisplayName("getById() — lève ResourceNotFoundException si introuvable")
    void getById_notFound_throwsNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L, owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getById() — lève AccessDeniedException si tâche d'un autre utilisateur")
    void getById_notOwned_throwsAccessDeniedException() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        assertThatThrownBy(() -> taskService.getById(10L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── COMPLETE ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("complete() — change le statut à COMPLETED et fixe completedAt")
    void complete_pendingTask_setsCompletedStatus() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskResponse response = taskService.complete(10L, owner);

        assertThat(sampleTask.getStatus()).isEqualTo(Task.Status.COMPLETED);
        assertThat(sampleTask.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("complete() — lève BusinessException si déjà terminée")
    void complete_alreadyCompleted_throwsBusinessException() {
        sampleTask.setStatus(Task.Status.COMPLETED);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        assertThatThrownBy(() -> taskService.complete(10L, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà marquée");
    }

    @Test
    @DisplayName("complete() — lève AccessDeniedException si l'utilisateur n'est pas propriétaire")
    void complete_notOwner_throwsAccessDeniedException() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        assertThatThrownBy(() -> taskService.complete(10L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() — supprime la tâche du propriétaire")
    void delete_byOwner_deletesTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        doNothing().when(taskRepository).delete(sampleTask);

        assertThatCode(() -> taskService.delete(10L, owner)).doesNotThrowAnyException();
        verify(taskRepository).delete(sampleTask);
    }

    @Test
    @DisplayName("delete() — admin peut supprimer n'importe quelle tâche")
    void delete_byAdmin_deletesTask() {
        User admin = User.builder().id(99L).email("admin@test.io").role(User.Role.ADMIN).name("Admin").build();
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        doNothing().when(taskRepository).delete(sampleTask);

        assertThatCode(() -> taskService.delete(10L, admin)).doesNotThrowAnyException();
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() — met à jour uniquement les champs fournis (PATCH-like)")
    void update_partialData_onlyUpdatesProvidedFields() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Titre modifié");
        // priority et status ne sont pas modifiés

        TaskResponse response = taskService.update(10L, owner, request);

        assertThat(response.getTitle()).isEqualTo("Titre modifié");
        assertThat(response.getPriority()).isEqualTo(Task.Priority.MEDIUM); // inchangé
    }
}
