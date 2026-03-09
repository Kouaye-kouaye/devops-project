package com.deployfast.taskmanager.service;

import com.deployfast.taskmanager.dto.response.CategoryResponse;
import com.deployfast.taskmanager.entity.Category;
import com.deployfast.taskmanager.entity.User;
import com.deployfast.taskmanager.exception.ResourceAlreadyExistsException;
import com.deployfast.taskmanager.exception.ResourceNotFoundException;
import com.deployfast.taskmanager.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(User user) {
        return categoryRepository.findByUser(user)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id, User user) {
        return mapToResponse(findOrThrow(id, user));
    }

    @Transactional
    public CategoryResponse create(User user, String name, String color) {
        if (categoryRepository.existsByNameAndUser(name, user)) {
            throw new ResourceAlreadyExistsException("Une catégorie avec ce nom existe déjà.");
        }
        Category category = Category.builder()
                .user(user).name(name)
                .color(color != null ? color : "#3B82F6")
                .build();
        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, User user, String name, String color) {
        Category category = findOrThrow(id, user);
        if (name != null) category.setName(name);
        if (color != null) category.setColor(color);
        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id, User user) {
        Category category = findOrThrow(id, user);
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
    }

    private CategoryResponse mapToResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId()).name(c.getName())
                .color(c.getColor()).createdAt(c.getCreatedAt())
                .build();
    }
}
