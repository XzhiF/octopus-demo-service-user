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
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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