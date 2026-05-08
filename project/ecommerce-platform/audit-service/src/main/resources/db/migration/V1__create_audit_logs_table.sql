-- =============================================================================
-- V1__create_audit_logs_table.sql  — Audit Service
-- =============================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id              VARCHAR(36)  NOT NULL,
    request_id      VARCHAR(36),
    user_id         VARCHAR(36),
    service_name    VARCHAR(30)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    description     TEXT,
    error_message   VARCHAR(500),
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(36),
    event_timestamp TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    inserted_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT chk_audit_status CHECK (status IN ('SUCCESS','FAILURE','PARTIAL')),
    CONSTRAINT chk_audit_service CHECK (
        service_name IN ('USER_SERVICE','PRODUCT_SERVICE','ORDER_SERVICE',
                         'PAYMENT_SERVICE','TRANSACTION_SERVICE','AUDIT_SERVICE','API_GATEWAY')
    )
);

-- Performance indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_audit_request_id       ON audit_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_user_id          ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_service          ON audit_logs(service_name);
CREATE INDEX IF NOT EXISTS idx_audit_action           ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_status           ON audit_logs(status);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp        ON audit_logs(event_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity           ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_service_status   ON audit_logs(service_name, status, event_timestamp);
-- Partial index: fast FAILURE queries (most common alert query)
CREATE INDEX IF NOT EXISTS idx_audit_failures
    ON audit_logs(service_name, event_timestamp DESC)
    WHERE status = 'FAILURE';

COMMENT ON TABLE  audit_logs                    IS 'Immutable audit trail from all microservices';
COMMENT ON COLUMN audit_logs.request_id         IS 'Distributed trace ID — links events across services';
COMMENT ON COLUMN audit_logs.event_timestamp    IS 'When the event occurred (from producing service)';
COMMENT ON COLUMN audit_logs.inserted_at        IS 'When this row was written to audit_db';
