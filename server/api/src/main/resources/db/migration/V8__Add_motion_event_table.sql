-- Motion detection events
-- Создаётся для хранения событий motion-детекции в PostgreSQL
CREATE TABLE IF NOT EXISTS motion_event (
    id VARCHAR(36) PRIMARY KEY,
    camera_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL DEFAULT 'MOTION_DETECTED',
    confidence REAL NOT NULL DEFAULT 1.0,
    region_of_interest TEXT,
    snapshot_path TEXT,
    metadata TEXT,
    created_at BIGINT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_motion_event_camera_id ON motion_event(camera_id);
CREATE INDEX IF NOT EXISTS idx_motion_event_created_at ON motion_event(created_at);