package com.deployfast.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Point d'entrée de l'application Task Manager.
 * Architecture : Spring Boot 3 + Spring Security + JWT + JPA
 */
@SpringBootApplication
@EnableJpaAuditing
public class TaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
