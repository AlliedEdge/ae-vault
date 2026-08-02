# Low-Level Design: Database Schema

## Overview
Complete PostgreSQL database schema for Ziboto v1, including tables, relationships, indexes, and constraints.

## Entity Relationship Diagram

```
┌─────────────────┐         ┌─────────────────┐
│     USERS       │         │    FOLDERS      │
├─────────────────┤         ├─────────────────┤
│ id (PK)         │────┐    │ id (PK)         │
│ email           │    │    │ user_id (FK)    │───┐
│ password_hash   │    │    │ parent_id (FK)  │───┘
│ first_name      │    │    │ folder_name     │
│ last_name       │    │    │ folder_path     │
│ storage_quota   │    │    │ created_at      │
│ storage_used    │    │    │ updated_at      │
│ is_active       │    │    │ deleted_at      │
│ created_at      │    │    └─────────────────┘
│ updated_at      │    │             │
│ last_login_at   │    │             │
└─────────────────┘    │             │
         │             │             │
         │             │             │
         │             └─────────────┼─────────────┐
         │                           │             │
         │                           │             │
         │                    ┌──────▼──────────┐  │
         │                    │ FILE_METADATA   │  │
         │                    ├─────────────────┤  │
         └───────────────────>│ id (PK)         │  │
                              │ user_id (FK)    │  │
                              │ folder_id (FK)  │<─┘
                              │ file_name       │
                              │ file_size       │
                              │ mime_type       │
                              │ sha256_hash     │
                              │ s3_bucket       │
                              │ s3_key          │
                              │ download_count  │
                              │ created_at      │
                              │ updated_at      │
                              │ deleted_at      │
                              └─────────────────┘
```

## Table Definitions

### 1. Users Table

```sql
CREATE TABLE users (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Authentication
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    
    -- Account Status
    is_active BOOLEAN DEFAULT true NOT NULL,
    is_email_verified BOOLEAN DEFAULT false NOT NULL,
    email_verification_token VARCHAR(255),
    email_verification_expiry TIMESTAMP,
    
    -- Password Reset
    password_reset_token VARCHAR(255),
    password_reset_expiry TIMESTAMP,
    
    -- Storage Management
    storage_quota_bytes BIGINT DEFAULT 5368709120 NOT NULL, -- 5GB default
    storage_used_bytes BIGINT DEFAULT 0 NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT check_storage_used CHECK (storage_used_bytes >= 0),
    CONSTRAINT check_storage_quota CHECK (storage_quota_bytes > 0),
    CONSTRAINT check_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token) 
    WHERE email_verification_token IS NOT NULL;
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token) 
    WHERE password_reset_token IS NOT NULL;

-- Comments
COMMENT ON TABLE users IS 'Stores user account information';
COMMENT ON COLUMN users.storage_quota_bytes IS 'Maximum storage allocation in bytes';
COMMENT ON COLUMN users.storage_used_bytes IS 'Current storage usage in bytes';
```

### 2. Folders Table

```sql
CREATE TABLE folders (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign Keys
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
    
    -- Folder Information
    folder_name VARCHAR(255) NOT NULL,
    folder_path TEXT NOT NULL,
    
    -- Metadata
    folder_color VARCHAR(7), -- Hex color code #RRGGBB
    folder_icon VARCHAR(50),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT check_folder_name_not_empty CHECK (LENGTH(TRIM(folder_name)) > 0),
    CONSTRAINT unique_folder_per_user UNIQUE(user_id, parent_folder_id, folder_name, deleted_at)
);

-- Indexes
CREATE INDEX idx_folders_user_id ON folders(user_id);
CREATE INDEX idx_folders_parent_folder_id ON folders(parent_folder_id);
CREATE INDEX idx_folders_folder_path ON folders USING GIN(folder_path gin_trgm_ops);
CREATE INDEX idx_folders_created_at ON folders(created_at);
CREATE INDEX idx_folders_deleted_at ON folders(deleted_at) WHERE deleted_at IS NULL;

-- Comments
COMMENT ON TABLE folders IS 'Hierarchical folder structure for organizing files';
COMMENT ON COLUMN folders.folder_path IS 'Full path from root to this folder (e.g., /Documents/Projects)';
```

### 3. File Metadata Table

```sql
CREATE TABLE file_metadata (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign Keys
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    
    -- File Information
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_extension VARCHAR(20),
    
    -- Storage Information
    sha256_hash CHAR(64) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    s3_key VARCHAR(500) NOT NULL UNIQUE,
    s3_version_id VARCHAR(100),
    
    -- File Description
    description TEXT,
    tags TEXT[], -- Array of tags
    
    -- Statistics
    download_count INT DEFAULT 0 NOT NULL,
    last_downloaded_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT check_file_size_positive CHECK (file_size > 0),
    CONSTRAINT check_download_count_positive CHECK (download_count >= 0),
    CONSTRAINT check_file_name_not_empty CHECK (LENGTH(TRIM(file_name)) > 0)
);

-- Indexes
CREATE INDEX idx_file_metadata_user_id ON file_metadata(user_id);
CREATE INDEX idx_file_metadata_folder_id ON file_metadata(folder_id);
CREATE INDEX idx_file_metadata_sha256_hash ON file_metadata(sha256_hash);
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at DESC);
CREATE INDEX idx_file_metadata_file_name ON file_metadata USING GIN(file_name gin_trgm_ops);
CREATE INDEX idx_file_metadata_mime_type ON file_metadata(mime_type);
CREATE INDEX idx_file_metadata_deleted_at ON file_metadata(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_file_metadata_tags ON file_metadata USING GIN(tags);

-- Full-text search index
CREATE INDEX idx_file_metadata_fulltext ON file_metadata 
    USING GIN(to_tsvector('english', file_name || ' ' || COALESCE(description, '')));

-- Comments
COMMENT ON TABLE file_metadata IS 'Stores metadata for all uploaded files';
COMMENT ON COLUMN file_metadata.sha256_hash IS 'SHA-256 hash for duplicate detection';
COMMENT ON COLUMN file_metadata.s3_key IS 'Unique key for S3 object storage';
COMMENT ON COLUMN file_metadata.upload_method IS 'Upload method used: STANDARD (<100MB) or MULTIPART (>100MB)';
COMMENT ON COLUMN file_metadata.multipart_upload_id IS 'AWS S3 multipart upload ID for tracking';
```

### 4. Audit Logs Table

```sql
CREATE TABLE audit_logs (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- User Information
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    user_email VARCHAR(255),
    
    -- Action Details
    action VARCHAR(50) NOT NULL, -- LOGIN, LOGOUT, UPLOAD, DOWNLOAD, DELETE, etc.
    resource_type VARCHAR(50), -- USER, FILE, FOLDER
    resource_id UUID,
    
    -- Request Details
    ip_address INET,
    user_agent TEXT,
    
    -- Additional Information
    details JSONB,
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILURE
    error_message TEXT,
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_details ON audit_logs USING GIN(details);

-- Partitioning by month (for better performance with large datasets)
-- This is a template - actual partitions created dynamically
-- CREATE TABLE audit_logs_2026_08 PARTITION OF audit_logs
--     FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- Comments
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all user actions';
COMMENT ON COLUMN audit_logs.details IS 'JSON field for additional context-specific information';
```

### 5. Refresh Tokens Table

```sql
CREATE TABLE refresh_tokens (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign Key
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Token Information
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    
    -- Device Information
    device_name VARCHAR(100),
    device_type VARCHAR(50), -- WEB, MOBILE, DESKTOP
    ip_address INET,
    user_agent TEXT,
    
    -- Token Status
    is_revoked BOOLEAN DEFAULT false NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT check_expires_at_future CHECK (expires_at > created_at)
);

-- Indexes
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(user_id, is_revoked) 
    WHERE is_revoked = false;

-- Comments
COMMENT ON TABLE refresh_tokens IS 'Stores refresh tokens for JWT authentication';
```

## Database Functions

### 1. Update Timestamp Trigger

```sql
-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to tables
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_folders_updated_at 
    BEFORE UPDATE ON folders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_file_metadata_updated_at 
    BEFORE UPDATE ON file_metadata 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 2. Update User Storage Trigger

```sql
-- Function to update user storage usage
CREATE OR REPLACE FUNCTION update_user_storage()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE users 
        SET storage_used_bytes = storage_used_bytes + NEW.file_size
        WHERE id = NEW.user_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE users 
        SET storage_used_bytes = storage_used_bytes - OLD.file_size
        WHERE id = OLD.user_id;
    ELSIF TG_OP = 'UPDATE' THEN
        UPDATE users 
        SET storage_used_bytes = storage_used_bytes - OLD.file_size + NEW.file_size
        WHERE id = NEW.user_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger
CREATE TRIGGER update_storage_on_file_change
    AFTER INSERT OR UPDATE OR DELETE ON file_metadata
    FOR EACH ROW EXECUTE FUNCTION update_user_storage();
```

### 3. Soft Delete Function

```sql
-- Function for soft deletes
CREATE OR REPLACE FUNCTION soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        UPDATE file_metadata SET deleted_at = CURRENT_TIMESTAMP WHERE id = OLD.id;
        RETURN NULL; -- Prevent actual delete
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;
```

## Stored Procedures

### 1. Get Folder Tree

```sql
CREATE OR REPLACE FUNCTION get_folder_tree(p_user_id UUID, p_folder_id UUID DEFAULT NULL)
RETURNS TABLE (
    folder_id UUID,
    folder_name VARCHAR,
    parent_folder_id UUID,
    folder_path TEXT,
    level INT
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE folder_tree AS (
        -- Base case: start with specified folder or root
        SELECT 
            f.id,
            f.folder_name,
            f.parent_folder_id,
            f.folder_path,
            0 as level
        FROM folders f
        WHERE f.user_id = p_user_id
          AND (f.parent_folder_id = p_folder_id OR (p_folder_id IS NULL AND f.parent_folder_id IS NULL))
          AND f.deleted_at IS NULL
        
        UNION ALL
        
        -- Recursive case: get children
        SELECT 
            f.id,
            f.folder_name,
            f.parent_folder_id,
            f.folder_path,
            ft.level + 1
        FROM folders f
        INNER JOIN folder_tree ft ON f.parent_folder_id = ft.folder_id
        WHERE f.user_id = p_user_id
          AND f.deleted_at IS NULL
    )
    SELECT * FROM folder_tree ORDER BY folder_path;
END;
$$ LANGUAGE plpgsql;
```

### 2. Calculate User Storage

```sql
CREATE OR REPLACE FUNCTION calculate_user_storage(p_user_id UUID)
RETURNS TABLE (
    total_files BIGINT,
    total_size BIGINT,
    file_types JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::BIGINT as total_files,
        COALESCE(SUM(file_size), 0)::BIGINT as total_size,
        jsonb_object_agg(mime_type, type_count) as file_types
    FROM (
        SELECT 
            mime_type,
            COUNT(*)::INT as type_count
        FROM file_metadata
        WHERE user_id = p_user_id
          AND deleted_at IS NULL
        GROUP BY mime_type
    ) types;
END;
$$ LANGUAGE plpgsql;
```

## Views

### 1. Active Files View

```sql
CREATE VIEW active_files AS
SELECT 
    fm.id,
    fm.user_id,
    fm.folder_id,
    fm.file_name,
    fm.file_size,
    fm.mime_type,
    fm.created_at,
    fm.download_count,
    u.email as owner_email,
    f.folder_path
FROM file_metadata fm
INNER JOIN users u ON fm.user_id = u.id
LEFT JOIN folders f ON fm.folder_id = f.id
WHERE fm.deleted_at IS NULL
  AND u.is_active = true;

COMMENT ON VIEW active_files IS 'Shows all active files with owner and folder information';
```

### 2. User Storage Summary View

```sql
CREATE VIEW user_storage_summary AS
SELECT 
    u.id as user_id,
    u.email,
    u.storage_quota_bytes,
    u.storage_used_bytes,
    ROUND((u.storage_used_bytes::NUMERIC / u.storage_quota_bytes * 100), 2) as usage_percentage,
    COUNT(fm.id) as total_files
FROM users u
LEFT JOIN file_metadata fm ON u.id = fm.user_id AND fm.deleted_at IS NULL
WHERE u.deleted_at IS NULL
GROUP BY u.id, u.email, u.storage_quota_bytes, u.storage_used_bytes;

COMMENT ON VIEW user_storage_summary IS 'Summary of storage usage for all users';
```

## Database Maintenance

### 1. Cleanup Expired Refresh Tokens

```sql
CREATE OR REPLACE FUNCTION cleanup_expired_refresh_tokens()
RETURNS INT AS $$
DECLARE
    deleted_count INT;
BEGIN
    DELETE FROM refresh_tokens
    WHERE expires_at < CURRENT_TIMESTAMP
       OR (is_revoked = true AND created_at < CURRENT_TIMESTAMP - INTERVAL '30 days');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Schedule with pg_cron (if available)
-- SELECT cron.schedule('cleanup-tokens', '0 2 * * *', 'SELECT cleanup_expired_refresh_tokens()');
```

### 2. Archive Old Audit Logs

```sql
CREATE OR REPLACE FUNCTION archive_old_audit_logs(days_to_keep INT DEFAULT 90)
RETURNS INT AS $$
DECLARE
    archived_count INT;
BEGIN
    -- Move to archive table (if exists)
    -- For now, just delete
    DELETE FROM audit_logs
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 day' * days_to_keep;
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;
```

## Performance Optimization

### 1. Analyze Statistics

```sql
-- Run after bulk inserts/updates
ANALYZE users;
ANALYZE folders;
ANALYZE file_metadata;
ANALYZE audit_logs;
```

### 2. Vacuum

```sql
-- Regular maintenance
VACUUM ANALYZE;

-- Full vacuum (requires exclusive lock)
VACUUM FULL;
```

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team
