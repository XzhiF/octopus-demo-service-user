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