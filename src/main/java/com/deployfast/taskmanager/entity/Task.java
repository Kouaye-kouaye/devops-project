package com.deployfast.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité centrale du domaine : représente une tâche.
 * Toute logique d'état (ex: complétion) est gérée dans TaskService.
 */
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_user", columnList = "user_id"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_priority", columnList = "priority"),
        @Index(name = "idx_tasks_due_date", columnList = "due_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    // ─── Domain helper ────────────────────────────────────────────────────────

    public boolean isOwnedBy(User owner) {
        return this.user != null && this.user.getId().equals(owner.getId());
    }

    public boolean isAlreadyCompleted() {
        return Status.COMPLETED.equals(this.status);
    }
}
