-- ──────────────────────────────────────────────────────────────────────
--  minimarketplace — Database Schema
--  Runs once when the postgres container is first created.
--  Hibernate ddl-auto=update keeps tables in sync during development.
-- ──────────────────────────────────────────────────────────────────────

-- Roles lookup
CREATE TABLE IF NOT EXISTS roles (
    id   BIGSERIAL   PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE        -- ROLE_ADMIN | ROLE_SELLER | ROLE_BUYER
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,       -- BCrypt hash
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Many-to-many join: users ↔ roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seller upgrade requests (BUYER → SELLER, reviewed by ADMIN)
CREATE TABLE IF NOT EXISTS seller_requests (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING|APPROVED|REJECTED
    note         VARCHAR(500),
    requested_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    reviewed_at  TIMESTAMP
);

-- Seller products
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

-- Sales records for seller analytics
CREATE TABLE IF NOT EXISTS sales (
    id           BIGSERIAL     PRIMARY KEY,
    seller_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity     INTEGER       NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    sold_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Indexes ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_users_username         ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email            ON users(email);
CREATE INDEX IF NOT EXISTS idx_seller_requests_status ON seller_requests(status);
CREATE INDEX IF NOT EXISTS idx_seller_requests_user   ON seller_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_products_seller         ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_sales_seller            ON sales(seller_id);
CREATE INDEX IF NOT EXISTS idx_sales_product           ON sales(product_id);
CREATE INDEX IF NOT EXISTS idx_sales_sold_at           ON sales(sold_at);

-- ── Seed roles (idempotent) ───────────────────────────────────────────
INSERT INTO roles (name) VALUES ('ROLE_ADMIN')  ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_SELLER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_BUYER')  ON CONFLICT (name) DO NOTHING;

-- ── Seed admin user (idempotent) ─────────────────────────────────────
-- Password = BCrypt(cost=12) of "admin123"
-- DataInitializer.java also seeds this on every boot — belt-and-braces.
-- !! CHANGE THIS PASSWORD before any production deployment !!
INSERT INTO users (username, email, password, enabled)
VALUES (
    'admin',
    'admin@minimarket.com',
    '$2a$12$8pL4/E5VspMYPGdd4nXRNOH7TkCOEhLlD54wGxIiftNsrVoNWsreu',
    TRUE
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
