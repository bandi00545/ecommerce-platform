-- V1__create_payments_table.sql
CREATE TABLE IF NOT EXISTS payments (
    id                VARCHAR(36)   NOT NULL,
    order_id          VARCHAR(36)   NOT NULL,
    request_id        VARCHAR(36)   NOT NULL,
    user_id           VARCHAR(36)   NOT NULL,
    amount            NUMERIC(12,2) NOT NULL,
    status            VARCHAR(30)   NOT NULL DEFAULT 'INITIATED',
    failure_reason    VARCHAR(500),
    gateway_reference VARCHAR(100),
    refunded          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(36),
    updated_by        VARCHAR(36),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_payments            PRIMARY KEY (id),
    CONSTRAINT uq_payments_order_id   UNIQUE (order_id),
    CONSTRAINT uq_payments_request_id UNIQUE (request_id),
    CONSTRAINT chk_payments_status CHECK (
        status IN ('INITIATED','PROCESSING','SUCCESS','FAILED','REFUNDED')
    ),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_payments_order_id   ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_request_id ON payments(request_id);
CREATE INDEX IF NOT EXISTS idx_payments_status     ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_user_id    ON payments(user_id);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = CURRENT_TIMESTAMP; RETURN NEW; END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON payments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
