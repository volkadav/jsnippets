-- Add icon fields to users table
-- liquibase formatted sql

-- changeset jsnippets:v006-1
ALTER TABLE users ADD COLUMN icon BYTEA;
ALTER TABLE users ADD COLUMN icon_content_type TEXT;

COMMENT ON COLUMN users.icon IS 'Optional user profile icon image, max 32KB';
COMMENT ON COLUMN users.icon_content_type IS 'MIME type of the icon (e.g., image/png, image/jpeg)';

