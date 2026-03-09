package com.deployfast.taskmanager.repository;

import com.deployfast.taskmanager.entity.Task;
import com.deployfast.taskmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>,
        JpaSpecificationExecutor<Task> {

    Page<Task> findByUser(User user, Pageable pageable);

    /**
     * Recherche multi-critères pour les utilisateurs standard.
     * Les admins voient tout (pas de filtre user).
     */
    @Query("""
            SELECT t FROM Task t
            WHERE (:userId IS NULL OR t.user.id = :userId)
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:dueBefore IS NULL OR t.dueDate <= :dueBefore)
            """)
    Page<Task> findWithFilters(
            @Param("userId") Long userId,
            @Param("status") Task.Status status,
            @Param("priority") Task.Priority priority,
            @Param("search") String search,
            @Param("dueBefore") LocalDate dueBefore,
            Pageable pageable
    );

    Optional<Task> findByIdAndUser(Long id, User user);
}
