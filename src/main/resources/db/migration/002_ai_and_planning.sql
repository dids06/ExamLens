-- AI job audit, plans, tasks, sessions, guidance, material index, PYQ analysis.
CREATE TABLE ai_jobs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  exam_id BIGINT NULL,
  action_key VARCHAR(60) NOT NULL,
  prompt_version VARCHAR(20) NOT NULL,
  provider VARCHAR(30) NOT NULL,
  model VARCHAR(80) NULL,
  status VARCHAR(20) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  correction_attempts INT NOT NULL DEFAULT 0,
  duration_ms BIGINT NULL,
  input_chars INT NULL,
  error_code VARCHAR(60) NULL,
  error_message VARCHAR(500) NULL,
  raw_response MEDIUMTEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  CONSTRAINT fk_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_jobs_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE SET NULL,
  INDEX ix_jobs_user (user_id, created_at),
  INDEX ix_jobs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE study_plans (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  active_version_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_plans_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_plans_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT uq_plans_exam UNIQUE (exam_id),
  INDEX ix_plans_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plan_versions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  kind VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  ai_job_id BIGINT NULL,
  summary TEXT NULL,
  feasibility VARCHAR(20) NULL,
  feasibility_note TEXT NULL,
  assumptions TEXT NULL,
  warnings TEXT NULL,
  risks TEXT NULL,
  tradeoffs TEXT NULL,
  unscheduled_work TEXT NULL,
  total_estimated_minutes INT NULL,
  available_capacity_minutes INT NULL,
  change_summary TEXT NULL,
  change_explanation TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pv_plan FOREIGN KEY (plan_id) REFERENCES study_plans(id) ON DELETE CASCADE,
  CONSTRAINT uq_pv_version UNIQUE (plan_id, version_number),
  INDEX ix_pv_plan (plan_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE study_tasks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_version_id BIGINT NOT NULL,
  task_key VARCHAR(60) NOT NULL,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  topic_id BIGINT NULL,
  task_type VARCHAR(30) NOT NULL,
  title VARCHAR(255) NOT NULL,
  objective TEXT NULL,
  study_method VARCHAR(180) NULL,
  estimated_minutes INT NOT NULL,
  scheduled_date DATE NOT NULL,
  task_order INT NOT NULL DEFAULT 0,
  priority VARCHAR(10) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  ai_rationale TEXT NULL,
  revision_of_task_key VARCHAR(60) NULL,
  change_type VARCHAR(20) NULL,
  completed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tasks_pv FOREIGN KEY (plan_version_id) REFERENCES plan_versions(id) ON DELETE CASCADE,
  CONSTRAINT fk_tasks_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_tasks_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL,
  CONSTRAINT uq_tasks_key UNIQUE (plan_version_id, task_key),
  INDEX ix_tasks_pv (plan_version_id, scheduled_date, task_order),
  INDEX ix_tasks_user (user_id, status, scheduled_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE task_prerequisites (
  task_id BIGINT NOT NULL,
  prerequisite_task_key VARCHAR(60) NOT NULL,
  PRIMARY KEY (task_id, prerequisite_task_key),
  CONSTRAINT fk_tp_task FOREIGN KEY (task_id) REFERENCES study_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE study_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  task_id BIGINT NULL,
  planned_minutes INT NULL,
  actual_minutes INT NOT NULL,
  status VARCHAR(24) NOT NULL,
  difficulty_feedback VARCHAR(20) NULL,
  student_notes TEXT NULL,
  session_date DATE NOT NULL,
  ai_job_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ss_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_ss_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_ss_task FOREIGN KEY (task_id) REFERENCES study_tasks(id) ON DELETE SET NULL,
  INDEX ix_ss_user (user_id, session_date),
  INDEX ix_ss_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE teacher_guidance (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  document_id BIGINT NULL,
  ai_job_id BIGINT NULL,
  hints TEXT NULL,
  important_topics TEXT NULL,
  instructions TEXT NULL,
  exam_format TEXT NULL,
  uncertainties TEXT NULL,
  confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tg_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_tg_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_tg_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL,
  INDEX ix_tg_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE material_indexes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  sections_json MEDIUMTEXT NULL,
  unmapped_topics TEXT NULL,
  warnings TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mi_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_mi_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_mi_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  CONSTRAINT uq_mi_doc UNIQUE (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE source_comparisons (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  recommendations_json MEDIUMTEXT NULL,
  uncovered_topics TEXT NULL,
  warnings TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_scmp_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_scmp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_scmp_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE previous_papers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  paper_label VARCHAR(120) NOT NULL,
  exam_year VARCHAR(12) NULL,
  total_marks INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pp_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_pp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_pp_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  INDEX ix_pp_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pyq_questions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  paper_id BIGINT NOT NULL,
  question_key VARCHAR(40) NOT NULL,
  question_text TEXT NOT NULL,
  marks INT NULL,
  page_number INT NULL,
  matched_topic VARCHAR(255) NULL,
  CONSTRAINT fk_pq_paper FOREIGN KEY (paper_id) REFERENCES previous_papers(id) ON DELETE CASCADE,
  CONSTRAINT uq_pq_key UNIQUE (paper_id, question_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pyq_analyses (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  topic_frequency_json MEDIUMTEXT NULL,
  recurring_patterns TEXT NULL,
  unmatched_questions TEXT NULL,
  limitations TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pa_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_pa_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_pa_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plan_change_explanations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  plan_version_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  summary TEXT NULL,
  changes_json MEDIUMTEXT NULL,
  unchanged_priorities TEXT NULL,
  action_items TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pce_pv FOREIGN KEY (plan_version_id) REFERENCES plan_versions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
