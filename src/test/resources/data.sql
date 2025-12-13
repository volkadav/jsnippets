-- Insert test user 'alice' with password 'password'
-- BCrypt hash for 'password'
INSERT INTO users (username, email, password_hash, created_at, timezone) 
VALUES ('alice', 'alice@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', CURRENT_TIMESTAMP, 'UTC');
