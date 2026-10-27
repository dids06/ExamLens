-- Practice papers, attempts, evaluation, revision resources, readiness, conversations.
CREATE TABLE practice_papers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  instructions TEXT NULL,
  total_marks INT NOT NULL,
  duration_minutes INT NULL,
  difficulty VARCHAR(12) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ppr_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_ppr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_ppr_exam (exam_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE paper_questions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  paper_id BIGINT NOT NULL,
  client_key VARCHAR(40) NOT NULL,
  question_type VARCHAR(24) NOT NULL,
  topic VARCHAR(255) NULL,
  topic_id BIGINT NULL,
  prompt TEXT NOT NULL,
  options_json TEXT NULL,
  correct_option VARCHAR(8) NULL,
  marks INT NOT NULL,
  difficulty VARCHAR(12) NULL,
  model_answer MEDIUMTEXT NULL,
  rubric_json TEXT NULL,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_pq_paper2 FOREIGN KEY (paper_id) REFERENCES practice_papers(id) ON DELETE CASCADE,
  CONSTRAINT fk_pq_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL,
  CONSTRAINT uq_pq_client UNIQUE (paper_id, client_key),
  INDEX ix_pq_paper (paper_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE paper_attempts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  paper_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submitted_at TIMESTAMP NULL,
  timed BOOLEAN NOT NULL DEFAULT FALSE,
  duration_minutes INT NULL,
  total_awarded DECIMAL(8,2) NULL,
  total_possible INT NULL,
  ai_job_id BIGINT NULL,
  CONSTRAINT fk_pa2_paper FOREIGN KEY (paper_id) REFERENCES practice_papers(id) ON DELETE CASCADE,
  CONSTRAINT fk_pa2_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_pa2_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE attempt_answers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer_text MEDIUMTEXT NULL,
  selected_option VARCHAR(8) NULL,
  awarded_marks DECIMAL(8,2) NULL,
  grading_method VARCHAR(20) NULL,
  feedback TEXT NULL,
  criteria_json TEXT NULL,
  confidence VARCHAR(10) NULL,
  requires_manual_review BOOLEAN NOT NULL DEFAULT FALSE,
  ai_job_id BIGINT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_aa_attempt FOREIGN KEY (attempt_id) REFERENCES paper_attempts(id) ON DELETE CASCADE,
  CONSTRAINT fk_aa_question FOREIGN KEY (question_id) REFERENCES paper_questions(id) ON DELETE CASCADE,
  CONSTRAINT uq_aa UNIQUE (attempt_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE revision_resources (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  topic_title VARCHAR(255) NOT NULL,
  topic_id BIGINT NULL,
  key_points TEXT NULL,
  flashcards_json MEDIUMTEXT NULL,
  recall_json MEDIUMTEXT NULL,
  source_references TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rr_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_rr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_rr_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL,
  INDEX ix_rr_exam (exam_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE readiness_assessments (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  weak_topics_json MEDIUMTEXT NULL,
  strengths_json TEXT NULL,
  coverage_gaps TEXT NULL,
  readiness_summary TEXT NULL,
  data_limitations TEXT NULL,
  completion_percent DECIMAL(5,2) NULL,
  planned_minutes INT NULL,
  actual_minutes INT NULL,
  practice_average DECIMAL(5,2) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ra_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_ra_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_ra_exam (exam_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE concept_explanations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  ai_job_id BIGINT NULL,
  topic_title VARCHAR(255) NULL,
  question TEXT NOT NULL,
  direct_answer TEXT NULL,
  explanation MEDIUMTEXT NULL,
  examples_json TEXT NULL,
  check_json TEXT NULL,
  source_references TEXT NULL,
  limitations TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ce_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_ce_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_ce_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE conversations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  exam_id BIGINT NULL,
  title VARCHAR(160) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_conv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_conv_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE SET NULL,
  INDEX ix_conv_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE conversation_messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(12) NOT NULL,
  content TEXT NOT NULL,
  intent VARCHAR(40) NULL,
  ai_job_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cm_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
  INDEX ix_cm_conv (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_rate_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  action_key VARCHAR(60) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_are_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_are_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
