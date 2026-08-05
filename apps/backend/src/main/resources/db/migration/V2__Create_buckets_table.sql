-- Create buckets table
CREATE TABLE buckets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(63) NOT NULL,
    description VARCHAR(500),
    user_id BIGINT NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    storage_used BIGINT DEFAULT 0,
    file_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_buckets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_bucket_name_user UNIQUE (name, user_id)
);

-- Create indexes
CREATE INDEX idx_buckets_user_id ON buckets(user_id);
CREATE INDEX idx_buckets_status ON buckets(status);
CREATE INDEX idx_buckets_visibility ON buckets(visibility);
CREATE INDEX idx_buckets_created_at ON buckets(created_at);
