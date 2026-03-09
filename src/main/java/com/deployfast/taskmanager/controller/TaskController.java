package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.dto.request.StoreTaskRequest;
import com.deployfast.taskmanager.dto.request.UpdateTaskRequest;
import com.deployfast.taskmanager.dto.response.ApiResponse;
import com.deployfast.taskmanager.dto.response.TaskResponse;
import com.deployfast.taskmanager.entity.Task;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Contrôleur REST pour les tâches.
 * Versioning URI : /api/v1/tasks
 *
 * GET    /api/v1/tasks              → liste paginée avec filtres
 * POST   /api/v1/tasks              → créer une tâche
 * GET    /api/v1/tasks/{id}         → détail d'une tâche
 * PUT    /api/v1/tasks/{id}         → mise à jour complète
 * PATCH  /api/v1/tasks/{id}         → mise à jour partielle
 * DELETE /api/v1/tasks/{id}         → supprimer
 * PATCH  /api/v1/tasks/{id}/complete → marquer comme terminée
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    // ─── LIST avec filtres et pagination ─────────────────────────────────────

    @GetMapping
    public ApiResponse<Page<TaskResponse>> index(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        User currentUser = loadUser(principal);
        Page<TaskResponse> tasks = taskService.getPaginatedTasks(
                currentUser, status, priority, search, dueBefore, page, size, sortBy, direction);
        return ApiResponse.ok(tasks);
    }

    // ─── SHOW ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> show(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(taskService.getById(id, loadUser(principal)));
    }

    // ─── STORE ────────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TaskResponse> store(@Valid @RequestBody StoreTaskRequest request,
                                            @AuthenticationPrincipal UserDetails principal) {
        TaskResponse task = taskService.create(loadUser(principal), request);
        return ApiResponse.ok("Tâche créée avec succès.", task);
    }

    // ─── UPDATE (PUT = remplacement complet) ──────────────────────────────────

    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateTaskRequest request,
                                             @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Tâche mise à jour.", taskService.update(id, loadUser(principal), request));
    }

    // ─── PATCH (mise à jour partielle) ────────────────────────────────────────

    @PatchMapping("/{id}")
    public ApiResponse<TaskResponse> patch(@PathVariable Long id,
                                            @RequestBody UpdateTaskRequest request,
                                            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Tâche mise à jour.", taskService.update(id, loadUser(principal), request));
    }

    // ─── COMPLETE ─────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/complete")
    public ApiResponse<TaskResponse> complete(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Tâche marquée comme terminée.", taskService.complete(id, loadUser(principal)));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void destroy(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        taskService.delete(id, loadUser(principal));
    }

    // ─── Helper : récupère l'entité User depuis le principal ─────────────────

    private User loadUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }
}
