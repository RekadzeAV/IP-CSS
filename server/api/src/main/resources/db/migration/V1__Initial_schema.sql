-- Flyway Migration: Initial Schema
-- This migration creates the initial database schema
-- Compatible with both SQLite and PostgreSQL

-- Таблица камер
CREATE TABLE IF NOT EXISTS camera (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    url TEXT NOT NULL,
    username TEXT,
    password TEXT,
    model TEXT,
    status TEXT NOT NULL,
    resolution_width INTEGER,
    resolution_height INTEGER,
    fps INTEGER NOT NULL DEFAULT 25,
    bitrate INTEGER NOT NULL DEFAULT 4096,
    codec TEXT NOT NULL DEFAULT 'H.264',
    audio INTEGER NOT NULL DEFAULT 0,
    ptz_config TEXT,
    streams TEXT,
    settings TEXT,
    statistics TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    last_seen BIGINT
);

CREATE INDEX IF NOT EXISTS camera_status_index ON camera(status);
CREATE INDEX IF NOT EXISTS camera_created_at_index ON camera(created_at);

-- Таблица записей
CREATE TABLE IF NOT EXISTS recording (
    id TEXT NOT NULL PRIMARY KEY,
    camera_id TEXT NOT NULL,
    camera_name TEXT,
    start_time BIGINT NOT NULL,
    end_time BIGINT,
    duration INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    file_size INTEGER NOT NULL DEFAULT 0,
    format TEXT NOT NULL DEFAULT 'MP4',
    quality TEXT NOT NULL DEFAULT 'HIGH',
    status TEXT NOT NULL DEFAULT 'COMPLETED',
    thumbnail_url TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS recording_camera_id_index ON recording(camera_id);
CREATE INDEX IF NOT EXISTS recording_start_time_index ON recording(start_time);
CREATE INDEX IF NOT EXISTS recording_status_index ON recording(status);
CREATE INDEX IF NOT EXISTS recording_created_at_index ON recording(created_at);

-- Таблица событий
CREATE TABLE IF NOT EXISTS event (
    id TEXT NOT NULL PRIMARY KEY,
    camera_id TEXT NOT NULL,
    camera_name TEXT,
    type TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'INFO',
    timestamp BIGINT NOT NULL,
    description TEXT,
    metadata TEXT,
    acknowledged INTEGER NOT NULL DEFAULT 0,
    acknowledged_at BIGINT,
    acknowledged_by TEXT,
    thumbnail_url TEXT,
    video_url TEXT
);

CREATE INDEX IF NOT EXISTS event_camera_id_index ON event(camera_id);
CREATE INDEX IF NOT EXISTS event_type_index ON event(type);
CREATE INDEX IF NOT EXISTS event_severity_index ON event(severity);
CREATE INDEX IF NOT EXISTS event_timestamp_index ON event(timestamp);
CREATE INDEX IF NOT EXISTS event_acknowledged_index ON event(acknowledged);

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS "user" (
    id TEXT NOT NULL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    email TEXT,
    full_name TEXT,
    role TEXT NOT NULL,
    permissions TEXT,
    created_at BIGINT NOT NULL,
    last_login_at BIGINT,
    is_active INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS user_username_index ON "user"(username);
CREATE INDEX IF NOT EXISTS user_role_index ON "user"(role);
CREATE INDEX IF NOT EXISTS user_is_active_index ON "user"(is_active);

-- Таблица настроек
CREATE TABLE IF NOT EXISTS setting (
    id TEXT NOT NULL PRIMARY KEY,
    category TEXT NOT NULL,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'STRING',
    description TEXT,
    updated_at BIGINT NOT NULL,
    UNIQUE(category, key)
);

CREATE INDEX IF NOT EXISTS setting_category_index ON setting(category);
CREATE INDEX IF NOT EXISTS setting_key_index ON setting(key);
CREATE INDEX IF NOT EXISTS setting_category_key_index ON setting(category, key);

-- Таблица уведомлений
CREATE TABLE IF NOT EXISTS notification (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'NORMAL',
    camera_id TEXT,
    event_id TEXT,
    recording_id TEXT,
    channel_id TEXT,
    icon TEXT,
    sound INTEGER NOT NULL DEFAULT 1,
    vibration INTEGER NOT NULL DEFAULT 0,
    read INTEGER NOT NULL DEFAULT 0,
    read_at BIGINT,
    timestamp BIGINT NOT NULL,
    extras TEXT
);

CREATE INDEX IF NOT EXISTS notification_type_index ON notification(type);
CREATE INDEX IF NOT EXISTS notification_priority_index ON notification(priority);
CREATE INDEX IF NOT EXISTS notification_read_index ON notification(read);
CREATE INDEX IF NOT EXISTS notification_timestamp_index ON notification(timestamp);
CREATE INDEX IF NOT EXISTS notification_camera_id_index ON notification(camera_id);
CREATE INDEX IF NOT EXISTS notification_event_id_index ON notification(event_id);
