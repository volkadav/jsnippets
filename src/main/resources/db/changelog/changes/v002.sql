-- Add timezone column to users table
ALTER TABLE users ADD COLUMN timezone VARCHAR(50) DEFAULT 'UTC';

-- Update existing users to have UTC timezone
UPDATE users SET timezone = 'UTC' WHERE timezone IS NULL;