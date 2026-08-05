-- V6__Create_refresh_tokens_table.sql
-- Create refresh_tokens table for secure token management with BCrypt hashing

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(60) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    last_used_at TIMESTAMP,
    
    CONSTRAINT fk_refresh_token_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_revoked ON refresh_tokens(revoked);
CREATE INDEX idx_user_device ON refresh_tokens(user_id, device_info);

-- Add comments for documentation
COMMENT ON TABLE refresh_tokens IS 'Stores BCrypt-hashed refresh tokens for JWT authentication';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'BCrypt hash of the JWT refresh token (60 characters)';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Reference to the user who owns this token';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (typically 7 days from creation)';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Whether the token has been manually revoked (logout, security event)';
COMMENT ON COLUMN refresh_tokens.device_info IS 'Device information for multi-device support';
COMMENT ON COLUMN refresh_tokens.ip_address IS 'IP address from which the token was created';
COMMENT ON COLUMN refresh_tokens.user_agent IS 'User agent string for device tracking';
COMMENT ON COLUMN refresh_tokens.last_used_at IS 'Timestamp of last token usage for security monitoring';
