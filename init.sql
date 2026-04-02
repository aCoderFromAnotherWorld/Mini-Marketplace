-- Legacy/manual SQL seed file.
-- NOTE: Docker uses init-db.sql, not this file.
-- Kept in sync with current ROLE_* naming to avoid accidental mismatches.

INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_SELLER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_BUYER') ON CONFLICT (name) DO NOTHING;

-- password: admin123 (bcrypt)
INSERT INTO users (username, email, password, enabled)
VALUES ('admin', 'admin@minimarket.com', '$2a$12$8pL4/E5VspMYPGdd4nXRNOH7TkCOEhLlD54wGxIiftNsrVoNWsreu', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS products (
    id                 BIGSERIAL      PRIMARY KEY,
    seller_id          BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name               VARCHAR(120)   NOT NULL,
    description        VARCHAR(1000),
    price              NUMERIC(12,2)  NOT NULL,
    stock              INTEGER        NOT NULL DEFAULT 0,
    image_filename     VARCHAR(255),
    image_content_type VARCHAR(100),
    image_data         BYTEA,
    asset_filename     VARCHAR(255),
    asset_content_type VARCHAR(100),
    asset_data         BYTEA,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sales (
    id           BIGSERIAL     PRIMARY KEY,
    seller_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity     INTEGER       NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    sold_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);
