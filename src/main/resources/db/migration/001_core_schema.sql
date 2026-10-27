-- ExamLens core schema: accounts, exams, documents, extraction provenance, syllabus.
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(190) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
  theme VARCHAR(12) NOT NULL DEFAULT 'system',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP NULL,
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exams (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  subject_name VARCHAR(180) NOT NULL,
  exam_datetime DATETIME NOT NULL,
  timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
  target_marks INT NULL,
  total_marks INT NULL,
  notes TEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_exams_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX ix_exams_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_availability (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  day_of_week TINYINT NULL,
  specific_date DATE NULL,
  available_minutes INT NOT NULL,
  label VARCHAR(120) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_avail_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  INDEX ix_avail_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE documents (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  exam_id BIGINT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(120) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  file_size BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  document_type VARCHAR(40) NOT NULL,
  page_count INT NULL,
  extraction_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  extraction_method VARCHAR(40) NULL,
  extraction_warnings TEXT NULL,
  extracted_chars INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_docs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_docs_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE SET NULL,
  CONSTRAINT uq_docs_stored UNIQUE (stored_filename),
  INDEX ix_docs_user (user_id, created_at),
  INDEX ix_docs_exam (exam_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE document_blocks (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  block_key VARCHAR(40) NOT NULL,
  page_number INT NULL,
  heading VARCHAR(255) NULL,
  block_text MEDIUMTEXT NOT NULL,
  extraction_method VARCHAR(40) NOT NULL,
  confidence DECIMAL(4,3) NULL,
  uncertain BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_blocks_doc FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  CONSTRAINT uq_blocks_key UNIQUE (document_id, block_key),
  INDEX ix_blocks_doc (document_id, page_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE syllabus_versions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  source_document_id BIGINT NULL,
  ai_job_id BIGINT NULL,
  summary TEXT NULL,
  warnings TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP NULL,
  CONSTRAINT fk_sv_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_sv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_sv_doc FOREIGN KEY (source_document_id) REFERENCES documents(id) ON DELETE SET NULL,
  CONSTRAINT uq_sv_version UNIQUE (exam_id, version_number),
  INDEX ix_sv_exam (exam_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE topics (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  syllabus_version_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  parent_topic_id BIGINT NULL,
  unit_name VARCHAR(180) NULL,
  title VARCHAR(255) NOT NULL,
  subtopics TEXT NULL,
  learning_outcomes TEXT NULL,
  importance VARCHAR(10) NULL,
  difficulty VARCHAR(10) NULL,
  estimated_minutes INT NULL,
  source_pages VARCHAR(255) NULL,
  confidence VARCHAR(10) NULL,
  needs_review BOOLEAN NOT NULL DEFAULT FALSE,
  ai_reason TEXT NULL,
  display_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_topics_sv FOREIGN KEY (syllabus_version_id) REFERENCES syllabus_versions(id) ON DELETE CASCADE,
  CONSTRAINT fk_topics_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
  CONSTRAINT fk_topics_parent FOREIGN KEY (parent_topic_id) REFERENCES topics(id) ON DELETE SET NULL,
  INDEX ix_topics_sv (syllabus_version_id, display_order),
  INDEX ix_topics_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE syllabus_classifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  syllabus_version_id BIGINT NOT NULL,
  block_key VARCHAR(40) NULL,
  page_number INT NULL,
  original_text TEXT NOT NULL,
  classification VARCHAR(40) NOT NULL,
  confidence DECIMAL(4,3) NULL,
  normalized_topic VARCHAR(255) NULL,
  parent_unit VARCHAR(180) NULL,
  reason TEXT NULL,
  included BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_sc_sv FOREIGN KEY (syllabus_version_id) REFERENCES syllabus_versions(id) ON DELETE CASCADE,
  INDEX ix_sc_sv (syllabus_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_settings (
  user_id BIGINT NOT NULL PRIMARY KEY,
  default_study_minutes INT NOT NULL DEFAULT 120,
  preferred_depth VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
  retention_days INT NOT NULL DEFAULT 365,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
