-- Add pre-generated icon thumbnail fields to users table
-- liquibase formatted sql

-- changeset jsnippets:v007-1
ALTER TABLE users ADD COLUMN icon_thumbnail BYTEA;
ALTER TABLE users ADD COLUMN icon_thumbnail_content_type TEXT;

COMMENT ON COLUMN users.icon_thumbnail IS 'Pre-generated thumbnail icon image (32x32 PNG)';
COMMENT ON COLUMN users.icon_thumbnail_content_type IS 'MIME type of the thumbnail icon (always image/png)';
