CREATE DATABASE IF NOT EXISTS octopus_demo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE octopus_demo;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(64)  NOT NULL,
  nickname      VARCHAR(50)  NULL,
  email         VARCHAR(255) NOT NULL,
  age           INT          NULL,
  password_hash VARCHAR(255) NOT NULL,
  status        TINYINT      NOT NULL DEFAULT 1,
  phone         VARCHAR(32)  NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- CREATE TABLE IF NOT EXISTS 不会给已存在的 users 补列，这里按需 ALTER 兼容旧库。
SET @add_users_status := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'status') = 0,
  'ALTER TABLE users ADD COLUMN status TINYINT NOT NULL DEFAULT 1',
  'DO 0');
PREPARE add_users_status FROM @add_users_status;
EXECUTE add_users_status;
DEALLOCATE PREPARE add_users_status;

SET @add_users_phone := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'phone') = 0,
  'ALTER TABLE users ADD COLUMN phone VARCHAR(32) NULL',
  'DO 0');
PREPARE add_users_phone FROM @add_users_phone;
EXECUTE add_users_phone;
DEALLOCATE PREPARE add_users_phone;

CREATE TABLE IF NOT EXISTS addresses (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  user_id        BIGINT       NOT NULL,
  receiver_name  VARCHAR(50)  NOT NULL,
  receiver_phone VARCHAR(20)  NOT NULL,
  province       VARCHAR(50)  NOT NULL,
  city           VARCHAR(50)  NOT NULL,
  district       VARCHAR(50)  NOT NULL,
  detail_address VARCHAR(255) NOT NULL,
  postal_code    VARCHAR(20)  NULL,
  is_default     TINYINT(1)   NOT NULL DEFAULT 0,
  created_at     DATETIME(6)  NOT NULL,
  updated_at     DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_addresses_user_id (user_id),
  KEY idx_addresses_user_default (user_id, is_default),
  CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;