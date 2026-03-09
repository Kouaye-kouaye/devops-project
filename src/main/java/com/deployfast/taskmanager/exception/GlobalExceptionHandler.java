package com.deployfast.taskmanager.exception;

import com.deployfast.taskmanager.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestion centralisée des exceptions.
 * Principe : un seul endroit pour tout le traitement d'erreur → Clean Code.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Erreurs de validation (@Valid) ───────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        log.debug("Erreur de validation : {}", fieldErrors);
        return ApiResponse.error("Les données fournies sont invalides.", fieldErrors);
    }

    // ─── Ressource introuvable ────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Ressource introuvable : {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }

    // ─── Conflit (doublon) ────────────────────────────────────────────────────
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(ResourceAlreadyExistsException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    // ─── Accès refusé ─────────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error("Vous n'avez pas la permission d'effectuer cette action.");
    }

    // ─── Non authentifié ──────────────────────────────────────────────────────
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationError(Exception ex) {
        return ApiResponse.error("Identifiants invalides.");
    }

    // ─── Règle métier violée ──────────────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("Règle métier violée : {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }

    // ─── Erreur interne (catch-all) ───────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleAllUnexpected(Exception ex, WebRequest request) {
        // On log l'erreur complète mais on n'expose pas les détails au client
        log.error("Erreur interne non gérée [{}] : {}", request.getDescription(false), ex.getMessage(), ex);
        return ApiResponse.error("Une erreur interne est survenue. Veuillez réessayer plus tard.");
    }
}
