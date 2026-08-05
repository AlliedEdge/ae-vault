-- Create file_metadata table
CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    checksum VARCHAR(64),
    bucket_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    storage_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_file_metadata_bucket FOREIGN KEY (bucket_id) REFERENCES buckets(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_metadata_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_file_metadata_bucket_id ON file_metadata(bucket_id);
CREATE INDEX idx_file_metadata_user_id ON file_metadata(user_id);
CREATE INDEX idx_file_metadata_status ON file_metadata(status);
CREATE INDEX idx_file_metadata_file_name ON file_metadata(file_name);
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at);
