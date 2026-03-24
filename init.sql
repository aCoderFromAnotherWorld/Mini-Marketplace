-- Initialize roles and admin user
INSERT INTO roles (name) VALUES ('ADMIN'), ('SELLER'), ('BUYER');

INSERT INTO users (username, email, password, enabled) VALUES 
('admin', 'admin@example.com', '$2a$10$7lZglkVUtOJD37rKk9yN.u5mvrWzpQhcFgaA5oq9p5rG/.uhTP1di', true);  -- password: admin123

INSERT INTO user_roles (user_id, role_id) VALUES ((SELECT id FROM users WHERE username = 'admin'), (SELECT id FROM roles WHERE name = 'ADMIN'));
