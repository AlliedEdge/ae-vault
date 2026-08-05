-- Add last_login_at column to users table
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;

-- Create index for efficient queries on last login
CREATE INDEX idx_users_last_login_at ON users(last_login_at);

-- Add comment for documentation
COMMENT ON COLUMN users.last_login_at IS 'Timestamp of the user''s last successful login';
