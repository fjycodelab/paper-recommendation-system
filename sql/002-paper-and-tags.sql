-- Spec 002: 论文信息与研究方向标签管理
-- 先执行 sql/001-auth-and-foundation.sql，确保 users 表已存在。

CREATE DATABASE IF NOT EXISTS paper_recommendation
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE paper_recommendation;

CREATE TABLE IF NOT EXISTS research_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(100) NOT NULL,
  parent_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  level TINYINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_research_tags_code (code),
  UNIQUE KEY uk_research_tags_parent_name (parent_id, name),
  KEY idx_research_tags_parent (parent_id),
  KEY idx_research_tags_status (status),
  CONSTRAINT fk_research_tags_parent FOREIGN KEY (parent_id) REFERENCES research_tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS papers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(512) NULL,
  authors TEXT NULL,
  abstract_text TEXT NULL,
  publish_year INT NULL,
  source VARCHAR(32) NULL,
  source_paper_id VARCHAR(128) NULL,
  doi VARCHAR(191) NULL,
  source_url VARCHAR(1024) NULL,
  download_url VARCHAR(1024) NULL,
  keywords VARCHAR(1000) NULL,
  citation_count INT NULL,
  published_at DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  submitted_by BIGINT NULL,
  deleted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_papers_doi (doi),
  UNIQUE KEY uk_papers_source_id (source, source_paper_id),
  KEY idx_papers_title (title(191)),
  KEY idx_papers_year (publish_year),
  KEY idx_papers_source (source),
  KEY idx_papers_status (status),
  KEY idx_papers_submitted_by (submitted_by),
  CONSTRAINT fk_papers_submitted_by FOREIGN KEY (submitted_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paper_tags (
  paper_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (paper_id, tag_id),
  KEY idx_paper_tags_tag (tag_id),
  CONSTRAINT fk_paper_tags_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_paper_tags_tag FOREIGN KEY (tag_id) REFERENCES research_tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS paper_download_attempts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  requested_by BIGINT NULL,
  download_url VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NULL,
  file_size BIGINT NULL,
  local_file_path VARCHAR(1024) NULL,
  failure_reason VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_download_attempts_paper (paper_id),
  KEY idx_download_attempts_status (status),
  CONSTRAINT fk_download_attempts_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_download_attempts_user FOREIGN KEY (requested_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
VALUES
  ('ai', NULL, '人工智能', 1, 'ACTIVE', 10),
  ('data-science', NULL, '数据科学', 1, 'ACTIVE', 20),
  ('software-engineering', NULL, '软件工程', 1, 'ACTIVE', 30),
  ('computer-systems', NULL, '计算机系统', 1, 'ACTIVE', 40),
  ('hci', NULL, '人机交互', 1, 'ACTIVE', 50),
  ('research-methods', NULL, '科研方法', 1, 'ACTIVE', 60)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  name = VALUES(name),
  level = VALUES(level),
  status = VALUES(status),
  sort_order = VALUES(sort_order),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'ai-machine-learning', id, '机器学习', 2, 'ACTIVE', 10 FROM research_tags WHERE code = 'ai'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'ai-deep-learning', id, '深度学习', 2, 'ACTIVE', 20 FROM research_tags WHERE code = 'ai'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'ai-nlp', id, '自然语言处理', 2, 'ACTIVE', 30 FROM research_tags WHERE code = 'ai'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'ai-recommender-systems', id, '推荐系统', 2, 'ACTIVE', 40 FROM research_tags WHERE code = 'ai'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'data-information-retrieval', id, '信息检索', 2, 'ACTIVE', 10 FROM research_tags WHERE code = 'data-science'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'data-knowledge-graph', id, '知识图谱', 2, 'ACTIVE', 20 FROM research_tags WHERE code = 'data-science'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'data-data-mining', id, '数据挖掘', 2, 'ACTIVE', 30 FROM research_tags WHERE code = 'data-science'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'data-visualization', id, '数据可视化', 2, 'ACTIVE', 40 FROM research_tags WHERE code = 'data-science'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'se-requirements', id, '需求工程', 2, 'ACTIVE', 10 FROM research_tags WHERE code = 'software-engineering'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'se-testing', id, '软件测试', 2, 'ACTIVE', 20 FROM research_tags WHERE code = 'software-engineering'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'se-architecture', id, '软件架构', 2, 'ACTIVE', 30 FROM research_tags WHERE code = 'software-engineering'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'se-devops', id, 'DevOps', 2, 'ACTIVE', 40 FROM research_tags WHERE code = 'software-engineering'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'systems-distributed', id, '分布式系统', 2, 'ACTIVE', 10 FROM research_tags WHERE code = 'computer-systems'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'systems-database', id, '数据库系统', 2, 'ACTIVE', 20 FROM research_tags WHERE code = 'computer-systems'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'systems-cloud', id, '云计算', 2, 'ACTIVE', 30 FROM research_tags WHERE code = 'computer-systems'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'systems-security', id, '系统安全', 2, 'ACTIVE', 40 FROM research_tags WHERE code = 'computer-systems'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'hci-user-experience', id, '用户体验', 2, 'ACTIVE', 10 FROM research_tags WHERE code = 'hci'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'hci-visual-analytics', id, '可视分析', 2, 'ACTIVE', 20 FROM research_tags WHERE code = 'hci'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'hci-education-tech', id, '教育技术', 2, 'ACTIVE', 30 FROM research_tags WHERE code = 'hci'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'methods-bibliometrics', id, '文献计量', 2, 'ACTIVE', 10 FROM research_tags WHERE code = 'research-methods'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'methods-research-behavior', id, '科研行为分析', 2, 'ACTIVE', 20 FROM research_tags WHERE code = 'research-methods'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

INSERT INTO research_tags (code, parent_id, name, level, status, sort_order)
SELECT 'methods-evaluation', id, '模型评估', 2, 'ACTIVE', 30 FROM research_tags WHERE code = 'research-methods'
ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), level = VALUES(level), status = VALUES(status), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;
