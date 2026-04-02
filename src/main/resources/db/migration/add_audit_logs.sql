CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NULL,
    actor_username VARCHAR(100) NULL,
    actor_role VARCHAR(50) NULL,
    action VARCHAR(80) NOT NULL,
    object_type VARCHAR(80) NOT NULL,
    object_id BIGINT NULL,
    object_label VARCHAR(200) NULL,
    before_state LONGTEXT NULL,
    after_state LONGTEXT NULL,
    change_summary LONGTEXT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_object ON audit_logs (object_type, object_id);
