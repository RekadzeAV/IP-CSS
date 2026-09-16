-- Flyway Migration: Server auth tables for ServerUserRepository (PostgreSQL)
-- Tables: user_password_hash, refresh_token, user_totp
-- Used when DATABASE_URL is set for persistent auth storage

-- Хеши паролей пользователей (отдельная таблица для безопасности)
CREATE TABLE IF NOT EXISTS user_password_hash (
    user_id TEXT NOT NULL PRIMARY KEY,
    password_hash TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT fk_user_password_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS user_password_hash_user_id_index ON user_password_hash(user_id);

-- Refresh-токены для JWT (с возможным сроком истечения)
CREATE TABLE IF NOT EXISTS refresh_token (
    token TEXT NOT NULL PRIMARY KEY,
    user_id TEXT NOT NULL,
    expires_at BIGINT,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS refresh_token_user_id_index ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS refresh_token_expires_at_index ON refresh_token(expires_at);

-- TOTP 2FA: секрет и флаг включения
CREATE TABLE IF NOT EXISTS user_totp (
    user_id TEXT NOT NULL PRIMARY KEY,
    secret TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL,
    CONSTRAINT fk_user_totp_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS user_totp_user_id_index ON user_totp(user_id);
