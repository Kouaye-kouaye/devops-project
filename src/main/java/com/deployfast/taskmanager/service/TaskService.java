package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.request.StoreTaskRequest;
import com.deployfast.taskmanager.dto.request.UpdateTaskRequest;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.dto.response.UserResponse;
import com.deployfast.taskmanager.dto.response.CategoryResponse;
import com.deployfast.taskmanager.entity.Category;
import com.deployfast.taskmanager.entity.Task;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.exception.BusinessException;
import com.deployfast.taskmanager.exception.ResourceNotFoundException;
import com.deployfast.taskmanager.repository.CategoryRepository;
import com.deployfast.taskmanager.repository.TaskRepository;
import com.deployfast.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service principal de gestion des tâches.
 * Respecte SRP : uniquement la logique métier des tâches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // ─── Liste paginée avec filtres ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TaskResponse> getPaginatedTasks(User currentUser,
                                                 Task.Status status,
                                                 Task.Priority priority,
                                                 String search,
                                                 LocalDate dueBefore,
                                                 int page, int size,
                                                 String sortBy, String direction) {
        // Les admins voient toutes les tâches, les users ne voient que les leurs
        Long userId = currentUser.isAdmin() ? null : currentUser.getId();

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return taskRepository
                .findWithFilters(userId, status, priority, search, dueBefore, pageable)
                .map(this::mapToResponse);
    }

    // ─── Récupérer une tâche ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);
        assertCanView(currentUser, task);
        return mapToResponse(task);
    }

    // ─── Créer une tâche ──────────────────────────────────────────────────────

    @Transactional
    public TaskResponse create(User currentUser, StoreTaskRequest request) {
        Category category = resolveCategory(request.getCategoryId(), currentUser);
        User assignedUser = resolveAssignedUser(request.getAssignedTo());

        Task task = Task.builder()
                .user(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : Task.Status.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : Task.Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .category(category)
                .assignedTo(assignedUser)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Tâche créée [id={}] par l'utilisateur [id={}]", saved.getId(), currentUser.getId());
        return mapToResponse(saved);
    }

    // ─── Mettre à jour une tâche ──────────────────────────────────────────────

    @Transactional
    public TaskResponse update(Long id, User currentUser, UpdateTaskRequest request) {
        Task task = findTaskOrThrow(id);
        assertCanModify(currentUser, task);

        applyUpdates(task, request, currentUser);

        Task saved = taskRepository.save(task);
        log.info("Tâche mise à jour [id={}]", saved.getId());
        return mapToResponse(saved);
    }

    // ─── Compléter une tâche ──────────────────────────────────────────────────

    @Transactional
    public TaskResponse complete(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);
        assertCanModify(currentUser, task);

        if (task.isAlreadyCompleted()) {
            throw new BusinessException("La tâche est déjà marquée comme terminée.");
        }

        task.setStatus(Task.Status.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        return mapToResponse(taskRepository.save(task));
    }

    // ─── Supprimer une tâche ──────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, User currentUser) {
        Task task = findTaskOrThrow(id);
        assertCanModify(currentUser, task);
        taskRepository.delete(task);
        log.info("Tâche supprimée [id={}] par [id={}]", id, currentUser.getId());
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche", id));
    }

    private void assertCanView(User user, Task task) {
        boolean canView = user.isAdmin()
                || task.isOwnedBy(user)
                || (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(user.getId()));
        if (!canView) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter cette tâche.");
        }
    }

    private void assertCanModify(User user, Task task) {
        if (!user.isAdmin() && !task.isOwnedBy(user)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette tâche.");
        }
    }

    private Category resolveCategory(Long categoryId, User user) {
        if (categoryId == null) return null;
        return categoryRepository.findByIdAndUser(categoryId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", categoryId));
    }

    private User resolveAssignedUser(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur assigné", userId));
    }

    private void applyUpdates(Task task, UpdateTaskRequest request, User user) {
        if (request.getTitle() != null)       task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null)      task.setStatus(request.getStatus());
        if (request.getPriority() != null)    task.setPriority(request.getPriority());
        if (request.getDueDate() != null)     task.setDueDate(request.getDueDate());
        if (request.getCategoryId() != null)  task.setCategory(resolveCategory(request.getCategoryId(), user));
        if (request.getAssignedTo() != null)  task.setAssignedTo(resolveAssignedUser(request.getAssignedTo()));
    }

    // ─── Mapping vers DTO ─────────────────────────────────────────────────────

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .user(mapUser(task.getUser()))
                .category(task.getCategory() != null ? mapCategory(task.getCategory()) : null)
                .assignedTo(task.getAssignedTo() != null ? mapUser(task.getAssignedTo()) : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private UserResponse mapUser(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private CategoryResponse mapCategory(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .build();
    }
}
