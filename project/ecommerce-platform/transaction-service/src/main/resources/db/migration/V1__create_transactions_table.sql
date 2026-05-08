-- V1__create_transactions_table.sql
CREATE TABLE IF NOT EXISTS transactions (
    id               VARCHAR(36)   NOT NULL,
    order_id         VARCHAR(36)   NOT NULL,
    payment_id       VARCHAR(36)   NOT NULL,
    request_id       VARCHAR(36)   NOT NULL,
    user_id          VARCHAR(36)   NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    status           VARCHAR(30)   NOT NULL DEFAULT 'INITIATED',
    failure_reason   VARCHAR(500),
    ledger_reference VARCHAR(100),
    reversed         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(36),
    updated_by       VARCHAR(36),
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_transactions            PRIMARY KEY (id),
    CONSTRAINT uq_transactions_order_id   UNIQUE (order_id),
    CONSTRAINT uq_transactions_payment_id UNIQUE (payment_id),
    CONSTRAINT chk_txn_status CHECK (status IN ('INITIATED','COMPLETED','FAILED','REVERSED')),
    CONSTRAINT chk_txn_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_txn_order_id   ON transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_txn_payment_id ON transactions(payment_id);
CREATE INDEX IF NOT EXISTS idx_txn_status     ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_txn_user_id    ON transactions(user_id);
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = CURRENT_TIMESTAMP; RETURN NEW; END; $$ LANGUAGE plpgsql;
CREATE TRIGGER trg_transactions_updated_at BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
