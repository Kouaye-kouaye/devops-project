package com.deployfast.taskmanager.controller;

import com.deployfast.taskmanager.dto.response.ApiResponse;
import com.deployfast.taskmanager.dto.response.CategoryResponse;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.repository.UserRepository;
import com.deployfast.taskmanager.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> index(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(categoryService.getAll(loadUser(principal)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> show(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(categoryService.getById(id, loadUser(principal)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> store(@Valid @RequestBody CategoryRequest request,
                                                @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Catégorie créée.",
                categoryService.create(loadUser(principal), request.getName(), request.getColor()));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id,
                                                 @RequestBody CategoryRequest request,
                                                 @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Catégorie mise à jour.",
                categoryService.update(id, loadUser(principal), request.getName(), request.getColor()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void destroy(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        categoryService.delete(id, loadUser(principal));
    }

    private User loadUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    // ─── DTO interne ─────────────────────────────────────────────────────────
    @Data
    static class CategoryRequest {
        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        @Size(max = 100)
        private String name;
        private String color;
    }
}
