-- Insert test user 'alice' with password 'password'
-- BCrypt hash for 'password'
INSERT INTO users (username, email, password_hash, created_at, timezone) 
VALUES ('alice', 'alice@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', CURRENT_TIMESTAMP, 'UTC');

-- Insert additional test users for follow functionality tests
INSERT INTO users (username, email, password_hash, created_at, timezone)
VALUES ('bob', 'bob@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', CURRENT_TIMESTAMP, 'UTC');

INSERT INTO users (username, email, password_hash, created_at, timezone)
VALUES ('charlie', 'charlie@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', CURRENT_TIMESTAMP, 'UTC');

-- Test snippet for alice so the home page's recent-snippets section renders
INSERT INTO snippets (contents, poster_id, created_at, edited_at)
SELECT 'first test snippet', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'alice';

-- Give charlie more snippets than the home-page pageSize (20) so the
-- "see more" link threshold is exceeded.
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 1', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 2', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 3', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 4', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 5', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 6', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 7', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 8', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 9', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 10', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 11', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 12', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 13', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 14', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 15', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 16', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 17', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 18', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 19', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 20', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 21', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 22', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 23', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 24', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';
INSERT INTO snippets (contents, poster_id, created_at, edited_at) SELECT 'charlie snippet 25', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE username = 'charlie';

