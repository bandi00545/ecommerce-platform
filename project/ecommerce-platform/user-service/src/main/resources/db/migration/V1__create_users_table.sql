-- =============================================================================
-- V1__create_users_table.sql
-- Flyway migration: create the users table in user_db
--
-- Run automatically by Flyway on service startup.
-- Once applied, this file is NEVER modified (create V2 for changes).
--
-- Flyway tracks applied migrations in: flyway_schema_history table
-- =============================================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    -- Primary key: UUID stored as VARCHAR(36) for portability
    id                      VARCHAR(36)     NOT NULL,

    -- User identity fields
    full_name               VARCHAR(100)    NOT NULL,
    username                VARCHAR(50)     NOT NULL,
    email                   VARCHAR(255)    NOT NULL,

    -- Security: BCrypt hash, never plaintext
    password                VARCHAR(255)    NOT NULL,

    -- Profile
    phone                   VARCHAR(15),
    profile_picture_url     VARCHAR(500),
    address                 VARCHAR(500),

    -- Access control
    role                    VARCHAR(20)     NOT NULL DEFAULT 'USER',
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    failed_login_attempts   INTEGER         NOT NULL DEFAULT 0,

    -- Audit fields (from BaseEntity)
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),
    version                 BIGINT          NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

-- Performance indexes for common queries
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role     ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_enabled  ON users(enabled);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

-- Trigger: auto-update updated_at on every UPDATE statement
-- Eliminates need for Hibernate's @LastModifiedDate in some scenarios
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed initial admin user
-- Password: Admin@123 (BCrypt hash generated with strength 10)
INSERT INTO users (
    id, full_name, username, email, password,
    role, enabled, failed_login_attempts,
    created_at, updated_at, version
) VALUES (
    'admin-user-id-0000-0000-000000000001',
    'Platform Admin',
    'admin',
    'admin@ecommerce.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
) ON CONFLICT (email) DO NOTHING;

COMMENT ON TABLE users IS 'Registered user accounts for the e-commerce platform';
COMMENT ON COLUMN users.id IS 'UUID primary key';
COMMENT ON COLUMN users.password IS 'BCrypt hashed password - never store plaintext';
COMMENT ON COLUMN users.role IS 'RBAC role: USER or ADMIN';
COMMENT ON COLUMN users.enabled IS 'False = account soft-disabled, cannot login';
COMMENT ON COLUMN users.failed_login_attempts IS 'Counter reset to 0 on successful login';
