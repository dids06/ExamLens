-- =============================================================================
-- ExamLens Database Schema
-- Version: 1.0 (Phase 1 / P0 MVP Foundation)
-- Target RDBMS: MySQL 8.4 LTS (compatible with MySQL 8.0+)
-- Storage Engine: InnoDB
-- Character Set: utf8mb4 / utf8mb4_unicode_ci
--
-- Target Database: examlens
-- Execution Policy:
-- - Intended for first-time database initialization on a fresh environment.
-- - Safe for repeated execution via CREATE TABLE IF NOT EXISTS (will not drop or overwrite existing tables).
-- - NOTE: CREATE TABLE IF NOT EXISTS does NOT perform schema migrations or alter columns
--   on existing tables. Subsequent schema modifications must be applied via explicit ALTER TABLE statements.
-- =============================================================================

-- Ensure target database exists
CREATE DATABASE IF NOT EXISTS `examlens`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `examlens`;

-- -----------------------------------------------------------------------------
-- 1. Table: users
-- Purpose: Student accounts with unique email and secure password hash.
-- Shared Contract: User(id, name, email, passwordHash, createdAt)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uq_users_email` UNIQUE (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Table: exams
-- Purpose: Exam workspaces created by students.
-- Shared Contract: Exam(id, userId, subjectName, examDateTime, dailyStudyMinutes, targetMarks, status)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exams` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `subject_name` VARCHAR(150) NOT NULL,
    `exam_date_time` DATETIME NOT NULL,
    `daily_study_minutes` INT NOT NULL,
    `target_marks` DECIMAL(5, 2) NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_exams_user` FOREIGN KEY (`user_id`)
        REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_exams_daily_minutes` CHECK (`daily_study_minutes` > 0),
    CONSTRAINT `chk_exams_target_marks` CHECK (`target_marks` IS NULL OR `target_marks` >= 0),
    INDEX `idx_exams_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Table: topics
-- Purpose: Syllabus topics and optional hierarchical subtopics for an exam.
-- Shared Contract: Topic(id, examId, parentTopicId, name, estimatedMinutes, priority, status)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `topics` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL,
    `parent_topic_id` BIGINT NULL,
    `name` VARCHAR(200) NOT NULL,
    `estimated_minutes` INT NOT NULL DEFAULT 60,
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    `status` VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_topics_exam` FOREIGN KEY (`exam_id`)
        REFERENCES `exams` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_topics_parent` FOREIGN KEY (`parent_topic_id`)
        REFERENCES `topics` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_topics_estimated_minutes` CHECK (`estimated_minutes` > 0),
    INDEX `idx_topics_exam_id` (`exam_id`),
    INDEX `idx_topics_parent_id` (`parent_topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Table: study_plans
-- Purpose: Versioned study plans belonging to an exam workspace.
-- Shared Contract: StudyPlan(id, examId, version, createdAt, changeReason)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `study_plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL,
    `version` INT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `change_reason` VARCHAR(255) NOT NULL DEFAULT 'Initial baseline plan',
    CONSTRAINT `fk_study_plans_exam` FOREIGN KEY (`exam_id`)
        REFERENCES `exams` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_study_plans_version` CHECK (`version` >= 1),
    CONSTRAINT `uq_study_plans_exam_version` UNIQUE (`exam_id`, `version`),
    INDEX `idx_study_plans_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Table: study_tasks
-- Purpose: Scheduled tasks within a study plan linked to specific topics.
-- Shared Contract: StudyTask(id, planId, topicId, plannedStart, plannedEnd, plannedMinutes, actualMinutes, status)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `study_tasks` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `plan_id` BIGINT NOT NULL,
    `topic_id` BIGINT NOT NULL,
    `planned_start` DATETIME NOT NULL,
    `planned_end` DATETIME NOT NULL,
    `planned_minutes` INT NOT NULL,
    `actual_minutes` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_study_tasks_plan` FOREIGN KEY (`plan_id`)
        REFERENCES `study_plans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_study_tasks_topic` FOREIGN KEY (`topic_id`)
        REFERENCES `topics` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_study_tasks_planned_minutes` CHECK (`planned_minutes` > 0),
    CONSTRAINT `chk_study_tasks_actual_minutes` CHECK (`actual_minutes` >= 0),
    CONSTRAINT `chk_study_tasks_dates` CHECK (`planned_end` >= `planned_start`),
    INDEX `idx_study_tasks_plan_id` (`plan_id`),
    INDEX `idx_study_tasks_topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Table: study_sessions
-- Purpose: Actual logged study sessions and elapsed study time for a task.
-- Shared Contract: study_sessions (task_id, started_at, ended_at, duration_minutes)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `study_sessions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `started_at` DATETIME NOT NULL,
    `ended_at` DATETIME NULL,
    `duration_minutes` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_study_sessions_task` FOREIGN KEY (`task_id`)
        REFERENCES `study_tasks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_study_sessions_duration` CHECK (`duration_minutes` >= 0),
    CONSTRAINT `chk_study_sessions_dates` CHECK (`ended_at` IS NULL OR `ended_at` >= `started_at`),
    INDEX `idx_study_sessions_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
