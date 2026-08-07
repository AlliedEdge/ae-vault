-- V8__Create_folders_table.sql
-- Create folders table for hierarchical file organization

CREATE TABLE folders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    parent_folder_id UUID,
    
    folder_name VARCHAR(255) NOT NULL,
    folder_path TEXT NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    
    CONSTRAINT fk_folder_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_parent FOREIGN KEY (parent_folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT unique_folder_per_user UNIQUE(user_id, parent_folder_id, folder_name, deleted_at)
);

-- Create indexes for performance
CREATE INDEX idx_folders_user_id ON folders(user_id);
CREATE INDEX idx_folders_parent_folder_id ON folders(parent_folder_id);
CREATE INDEX idx_folders_folder_path ON folders USING btree (folder_path text_pattern_ops);
CREATE INDEX idx_folders_deleted_at ON folders(deleted_at);
CREATE INDEX idx_folders_created_at ON folders(created_at);

-- Add comments for documentation
COMMENT ON TABLE folders IS 'Stores user folder hierarchy for file organization';
COMMENT ON COLUMN folders.id IS 'Unique folder identifier';
COMMENT ON COLUMN folders.user_id IS 'Owner of the folder';
COMMENT ON COLUMN folders.parent_folder_id IS 'Parent folder for nested structure (NULL for root)';
COMMENT ON COLUMN folders.folder_name IS 'Display name of the folder';
COMMENT ON COLUMN folders.folder_path IS 'Full path from root (e.g., /Documents/Projects)';
COMMENT ON COLUMN folders.deleted_at IS 'Soft delete timestamp (NULL if active)';
