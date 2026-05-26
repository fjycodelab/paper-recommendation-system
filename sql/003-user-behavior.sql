-- Spec 003: 用户收藏评分与行为记录
-- 先执行 sql/001-auth-and-foundation.sql 和 sql/002-paper-and-tags.sql。

CREATE DATABASE IF NOT EXISTS paper_recommendation
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE paper_recommendation;

CREATE TABLE IF NOT EXISTS paper_favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_paper_favorites_user_paper (user_id, paper_id),
  KEY idx_paper_favorites_user_status (user_id, status),
  KEY idx_paper_favorites_paper_status (paper_id, status),
  CONSTRAINT fk_paper_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_paper_favorites_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paper_ratings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_paper_ratings_user_paper (user_id, paper_id),
  KEY idx_paper_ratings_paper (paper_id),
  CONSTRAINT fk_paper_ratings_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_paper_ratings_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT chk_paper_ratings_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paper_behavior_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  keyword VARCHAR(255) NULL,
  author VARCHAR(255) NULL,
  publish_year INT NULL,
  tag_id BIGINT NULL,
  metadata VARCHAR(1000) NULL,
  occurred_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_behavior_events_event_id (event_id),
  KEY idx_behavior_events_user_time (user_id, occurred_at),
  KEY idx_behavior_events_paper_type (paper_id, event_type),
  KEY idx_behavior_events_type_time (event_type, occurred_at),
  CONSTRAINT fk_behavior_events_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_behavior_events_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_behavior_events_tag FOREIGN KEY (tag_id) REFERENCES research_tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
