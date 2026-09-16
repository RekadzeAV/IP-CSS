-- Security alerts table
-- Создаётся для хранения алертов безопасности в PostgreSQL
-- Заменяет InMemorySecurityAlertRepository
CREATE TABLE IF NOT EXISTS security_alert (
    id VARCHAR(36) PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    source VARCHAR(100),
    message TEXT NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    user_id VARCHAR(36),
    created_at BIGINT NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by VARCHAR(36),
    acknowledged_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_security_alert_type ON security_alert(alert_type);
CREATE INDEX IF NOT EXISTS idx_security_alert_severity ON security_alert(severity);
CREATE INDEX IF NOT EXISTS idx_security_alert_created_at ON security_alert(created_at);
CREATE INDEX IF NOT EXISTS idx_security_alert_acknowledged ON security_alert(acknowledged);