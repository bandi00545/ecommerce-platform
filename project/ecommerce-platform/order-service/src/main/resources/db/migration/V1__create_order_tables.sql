-- =============================================================================
-- V1__create_order_tables.sql - Order Service database migration
-- Creates: orders, order_items, outbox_events, saga_logs
-- =============================================================================

-- -------------------------------------------------------------------------
-- ORDERS
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id               VARCHAR(36)    NOT NULL,
    user_id          VARCHAR(36)    NOT NULL,
    request_id       VARCHAR(36)    NOT NULL,
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(12,2)  NOT NULL,
    shipping_address VARCHAR(500)   NOT NULL,
    notes            VARCHAR(500),
    payment_id       VARCHAR(36),
    transaction_id   VARCHAR(36),
    failure_reason   VARCHAR(500),

    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36),
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_request_id UNIQUE (request_id),
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING','CONFIRMED','PROCESSING','PAYMENT_FAILED',
                   'TRANSACTION_FAILED','COMPLETED','CANCELLED','COMPENSATED')
    ),
    CONSTRAINT chk_orders_total CHECK (total_amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id    ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_request_id ON orders(request_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);

-- -------------------------------------------------------------------------
-- ORDER ITEMS
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id           VARCHAR(36)    NOT NULL,
    order_id     VARCHAR(36)    NOT NULL,
    product_id   VARCHAR(36)    NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    product_sku  VARCHAR(50)    NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   NUMERIC(12,2)  NOT NULL,

    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36),
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_order_items             PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order       FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price > 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- -------------------------------------------------------------------------
-- OUTBOX EVENTS (Transactional Outbox Pattern)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox_events (
    id             VARCHAR(36)   NOT NULL,
    aggregate_type VARCHAR(50)   NOT NULL,
    aggregate_id   VARCHAR(36)   NOT NULL,
    topic          VARCHAR(100)  NOT NULL,
    payload        TEXT          NOT NULL,
    event_type     VARCHAR(100)  NOT NULL,
    message_key    VARCHAR(36),
    processed      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at   TIMESTAMP,
    retry_count    INTEGER       NOT NULL DEFAULT 0,
    last_error     VARCHAR(500),

    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_processed    ON outbox_events(processed, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_id ON outbox_events(aggregate_id);

-- -------------------------------------------------------------------------
-- SAGA LOGS (Distributed transaction step tracking)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS saga_logs (
    id            VARCHAR(36)  NOT NULL,
    order_id      VARCHAR(36)  NOT NULL,
    request_id    VARCHAR(36)  NOT NULL,
    step_name     VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    step_data     TEXT,
    error_message VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  TIMESTAMP,

    CONSTRAINT pk_saga_logs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_saga_order_id ON saga_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_saga_step     ON saga_logs(step_name);

-- -------------------------------------------------------------------------
-- Auto-update triggers
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_order_items_updated_at
    BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE orders IS 'Customer orders - aggregate root';
COMMENT ON TABLE order_items IS 'Line items within an order';
COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable Kafka publishing';
COMMENT ON TABLE saga_logs IS 'Step-by-step log of Order Saga execution for debugging';
