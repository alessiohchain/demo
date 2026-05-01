-- V1: customer table for the demo registration / login flow
CREATE TABLE customer (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(100)    NOT NULL,
    display_name    VARCHAR(100)    NOT NULL,

    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_email ON customer (email);
