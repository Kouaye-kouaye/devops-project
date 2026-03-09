package com.deployfast.taskmanager.repository;

import com.deployfast.taskmanager.entity.Category;
import com.deployfast.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    boolean existsByNameAndUser(String name, User user);
}
