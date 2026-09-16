-- Flyway Migration: tamper-evident integrity chain for audit log
-- Adds hash chaining columns for append-only verification.

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS previous_hash TEXT,
    ADD COLUMN IF NOT EXISTS integrity_hash TEXT;

CREATE INDEX IF NOT EXISTS audit_log_integrity_hash_index ON audit_log(integrity_hash);
