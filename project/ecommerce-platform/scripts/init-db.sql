-- =============================================================================
-- init-db.sql
-- Run this ONCE in psql / pgAdmin BEFORE starting the services.
-- Creates the `ecommerce` database. Each microservice creates its own schema
-- (user_service, product_service, order_service, payment_service,
-- transaction_service, audit_service) automatically via Flyway.
-- =============================================================================

-- Connect as super-user (postgres) and run:
--   psql -U postgres -h localhost -f scripts/init-db.sql

CREATE DATABASE ecommerce;

-- Verify connectivity:
-- \c ecommerce
-- \dn
