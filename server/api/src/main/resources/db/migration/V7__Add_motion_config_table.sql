-- Motion detection configuration per camera
-- Создаётся для хранения настроек motion-детекции в PostgreSQL
CREATE TABLE IF NOT EXISTS motion_config (
    id VARCHAR(36) PRIMARY KEY,
    camera_id VARCHAR(36) NOT NULL,
    sensitivity INTEGER NOT NULL DEFAULT 50,
    region_of_interest TEXT,
    motion_threshold REAL NOT NULL DEFAULT 0.1,
    enable_motion_detection BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_motion_config_camera_id ON motion_config(camera_id);