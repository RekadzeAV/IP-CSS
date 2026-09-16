-- Flyway Migration: Add Performance Indexes
-- This migration adds additional indexes for query optimization

-- Composite indexes for common query patterns

-- Camera queries by status and creation date
CREATE INDEX IF NOT EXISTS camera_status_created_at_index ON camera(status, created_at DESC);

-- Recording queries by camera and time range
CREATE INDEX IF NOT EXISTS recording_camera_time_index ON recording(camera_id, start_time DESC);

-- Event queries by camera, type, and timestamp
CREATE INDEX IF NOT EXISTS event_camera_type_timestamp_index ON event(camera_id, type, timestamp DESC);

-- Event queries by acknowledged status and timestamp
CREATE INDEX IF NOT EXISTS event_acknowledged_timestamp_index ON event(acknowledged, timestamp DESC);

-- Notification queries by read status and timestamp
CREATE INDEX IF NOT EXISTS notification_read_timestamp_index ON notification(read, timestamp DESC);

-- Notification queries by type and priority
CREATE INDEX IF NOT EXISTS notification_type_priority_index ON notification(type, priority);

-- User queries by role and active status
CREATE INDEX IF NOT EXISTS user_role_active_index ON "user"(role, is_active);

-- Setting queries by category and key (already exists as UNIQUE, but adding explicit index for clarity)
-- CREATE INDEX IF NOT EXISTS setting_category_key_index ON setting(category, key); -- Already exists

-- Recording queries by status and creation date
CREATE INDEX IF NOT EXISTS recording_status_created_index ON recording(status, created_at DESC);

-- Event queries by severity and timestamp
CREATE INDEX IF NOT EXISTS event_severity_timestamp_index ON event(severity, timestamp DESC);
