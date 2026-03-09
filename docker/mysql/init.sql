-- ══════════════════════════════════════════════════════════════════════════
-- DeployFast Task Manager — Script d'initialisation MySQL
-- ══════════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS taskmanager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE taskmanager;

-- Table users
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    role            ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table categories
CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    color           VARCHAR(7)      NOT NULL DEFAULT '#3B82F6',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_categories_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table tasks
CREATE TABLE IF NOT EXISTS tasks (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    category_id     BIGINT UNSIGNED NULL,
    assigned_to     BIGINT UNSIGNED NULL,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    status          ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    priority        ENUM('LOW','MEDIUM','HIGH','URGENT')                  NOT NULL DEFAULT 'MEDIUM',
    due_date        DATE            NULL,
    completed_at    TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_to) REFERENCES users(id)      ON DELETE SET NULL,
    INDEX idx_tasks_user      (user_id),
    INDEX idx_tasks_status    (status),
    INDEX idx_tasks_priority  (priority),
    INDEX idx_tasks_due_date  (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Utilisateur admin par défaut (mot de passe: Admin@2024)
INSERT IGNORE INTO users (name, email, password, role)
VALUES ('Administrateur', 'admin@deployfast.io',
        '$2a$12$LcVv5D4C0rS9b0fjBkDEb.SyuWZRjLk1/iDM4nVv9qkHiB8yRsGmu', 'ADMIN');
