-- Spec 001: 登录认证与项目基础骨架
-- 用于本机 MVP 初始化；生产环境必须更换初始管理员密码。

CREATE DATABASE IF NOT EXISTS paper_recommendation
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE paper_recommendation;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_role (role),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- BCrypt hash for initial password: 123456
INSERT INTO users (username, password_hash, role, status)
VALUES (
  'fjy',
  '$2b$10$w2HR5vfxsncS.y/G9FoKS.qju1r/FVuBqNZ1jbLMzlBJq4hQ75aJu',
  'ADMIN',
  'ACTIVE'
)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  role = 'ADMIN',
  status = 'ACTIVE',
  updated_at = CURRENT_TIMESTAMP;
