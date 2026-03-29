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
