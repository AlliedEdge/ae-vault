-- V9__Create_file_metadata_table.sql
-- Create file_metadata table for storing file information

CREATE TABLE file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    folder_id UUID,
    
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_extension VARCHAR(20),
    
    sha256_hash VARCHAR(64) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    
    download_count INT DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    
    CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Create indexes for performance
CREATE INDEX idx_file_metadata_user_id ON file_metadata(user_id);
CREATE INDEX idx_file_metadata_folder_id ON file_metadata(folder_id);
CREATE INDEX idx_file_metadata_sha256_hash ON file_metadata(sha256_hash);
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at DESC);
CREATE INDEX idx_file_metadata_deleted_at ON file_metadata(deleted_at);
CREATE INDEX idx_file_metadata_mime_type ON file_metadata(mime_type);
CREATE INDEX idx_file_metadata_file_name ON file_metadata USING btree (file_name text_pattern_ops);

-- Add comments for documentation
COMMENT ON TABLE file_metadata IS 'Stores metadata for all uploaded files';
COMMENT ON COLUMN file_metadata.id IS 'Unique file identifier';
COMMENT ON COLUMN file_metadata.user_id IS 'Owner of the file';
COMMENT ON COLUMN file_metadata.folder_id IS 'Parent folder (NULL for root)';
COMMENT ON COLUMN file_metadata.file_name IS 'Current file name (can be renamed)';
COMMENT ON COLUMN file_metadata.original_file_name IS 'Original uploaded file name';
COMMENT ON COLUMN file_metadata.file_size IS 'File size in bytes';
COMMENT ON COLUMN file_metadata.mime_type IS 'File MIME type';
COMMENT ON COLUMN file_metadata.file_extension IS 'File extension (e.g., .pdf, .jpg)';
COMMENT ON COLUMN file_metadata.sha256_hash IS 'SHA-256 hash for deduplication and integrity';
COMMENT ON COLUMN file_metadata.storage_key IS 'Storage path/key for file location';
COMMENT ON COLUMN file_metadata.download_count IS 'Number of times file was downloaded';
COMMENT ON COLUMN file_metadata.deleted_at IS 'Soft delete timestamp (NULL if active)';
