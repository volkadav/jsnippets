-- Add bio field to users table
-- liquibase formatted sql

-- changeset jsnippets:v005-1
ALTER TABLE users ADD COLUMN bio TEXT;

COMMENT ON COLUMN users.bio IS 'Optional free-form bio text for user profiles';

