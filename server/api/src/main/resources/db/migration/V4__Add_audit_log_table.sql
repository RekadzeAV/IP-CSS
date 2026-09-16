-- Flyway Migration: durable security audit log storage
-- Table: audit_log

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type TEXT NOT NULL,
    severity TEXT NOT NULL,
    user_id TEXT,
    username TEXT,
    ip_address TEXT,
    user_agent TEXT,
    details_json TEXT NOT NULL DEFAULT '{}',
    event_timestamp BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS audit_log_event_timestamp_index ON audit_log(event_timestamp DESC);
CREATE INDEX IF NOT EXISTS audit_log_event_type_index ON audit_log(event_type);
CREATE INDEX IF NOT EXISTS audit_log_user_id_index ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS audit_log_severity_index ON audit_log(severity);

