# Low-Level Design: File Management System

## Overview
Core file upload, download, and management operations for Ziboto v1.

## System Components

### 1. File Controller Layer
- **Package**: `com.ziboto.file`
- **Key Classes**:
  - `FileController`
  - `FolderController`
  - `FileUploadController`
  - `MultipartUploadController` (for large files >100MB)

### 2. Service Layer
- **Package**: `com.ziboto.file.service`
- **Key Classes**:
  - `FileService`
  - `FolderService`
  - `S3StorageService`
  - `MultipartUploadService` (handles large file uploads)
  - `MetadataService`

### 3. Repository Layer
- **Package**: `com.ziboto.file.repository`
- **Key Classes**:
  - `FileMetadataRepository`
  - `FolderRepository`

## File Upload Flow

### Standard Upload (<100MB)

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ 1. Select File
       │    (file < 100MB)
       v
┌──────────────────────────┐
│  Upload Request          │
│  POST /files/upload      │
│  Content-Type: multipart │
└──────┬───────────────────┘
       │ 2. Send entire file
       v
┌──────────────────────────┐
│  FileUploadController    │
│  - Receive file          │
│  - Check size (<100MB)   │
└──────┬───────────────────┘
       │ 3. Validate & Process
       v
┌──────────────────────────┐
│     FileService          │
│  - Validate file type    │
│  - Check storage quota   │
│  - Calculate SHA-256     │
└──────┬───────────────────┘
       │ 4. Check for duplicate
       v
┌──────────────────────────┐
│    PostgreSQL DB         │
│  Check if hash exists    │
└──────┬───────────────────┘
       │ 5. No duplicate found
       v
┌──────────────────────────┐
│   S3StorageService       │
│  - Generate S3 key       │
│  - Upload to S3          │
└──────┬───────────────────┘
       │ 6. Upload complete
       v
┌──────────────────────────┐
│      AWS S3              │
│  Store file object       │
└──────┬───────────────────┘
       │ 7. Save metadata
       v
┌──────────────────────────┐
│    PostgreSQL DB         │
│  - Save file_metadata    │
│  - Update storage_used   │
└──────┬───────────────────┘
       │ 8. Cache metadata
       v
┌──────────────────────────┐
│       Redis              │
│  Cache file metadata     │
└──────┬───────────────────┘
       │ 9. Success response
       v
┌──────────────────────────┐
│       Client             │
│  Receive file metadata   │
└──────────────────────────┘
```

### Chunked Upload (>100MB) - Overview

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ File size check
       │ If > 100MB → Use Multipart Upload
       │ 
       │ 1. Split file into chunks
       │    Chunk 1: 0-10MB
       │    Chunk 2: 10-20MB
       │    Chunk 3: 20-30MB
       │    ... (10MB each)
       │
       v
┌────────────────────────────────────┐
│  Initiate Multipart Upload         │
│  POST /files/multipart/initiate    │
│  - fileName, fileSize, mimeType    │
└──────┬─────────────────────────────┘
       │ 2. Get uploadId, partSize, totalParts
       v
┌────────────────────────────────────┐
│  Upload Parts Loop                 │
│  For each chunk (1 to totalParts): │
│                                    │
│  PUT /multipart/upload/           │
│      {uploadId}/part/{partNumber}  │
│                                    │
│  Send chunk binary data            │
│  Receive ETag + progress           │
└──────┬─────────────────────────────┘
       │ 3. All chunks uploaded
       v
┌────────────────────────────────────┐
│  Complete Multipart Upload         │
│  POST /files/multipart/complete    │
│      /{uploadId}                   │
│                                    │
│  S3 assembles all chunks          │
└──────┬─────────────────────────────┘
       │ 4. Success response
       v
┌────────────────────────────────────┐
│       Client                       │
│  Receive fileId + metadata         │
└────────────────────────────────────┘
```

For detailed chunked upload implementation, see [S3 Multipart Upload](06-s3-multipart-upload.md)

## API Endpoints

### 1. Upload File

**Endpoint**: `POST /api/v1/files/upload`

**Note**: For files >100MB, use the multipart upload endpoints (see [S3 Multipart Upload](06-s3-multipart-upload.md))

**Request** (multipart/form-data):
```
file: [binary]
folderId: "uuid-string" (optional, defaults to root)
```

**Headers**:
```
Authorization: Bearer {jwt-token}
Content-Type: multipart/form-data
```

**Response**:
```json
{
  "success": true,
  "data": {
    "fileId": "uuid-string",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "mimeType": "application/pdf",
    "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "uploadedAt": "2026-08-02T10:30:00Z",
    "folderId": "uuid-string",
    "s3Key": "users/{userId}/files/{fileId}/document.pdf"
  }
}
```

### 2. Download File

**Endpoint**: `GET /api/v1/files/{fileId}/download`

**Response**: File stream with appropriate headers
```
Content-Type: {file-mime-type}
Content-Disposition: attachment; filename="{filename}"
Content-Length: {file-size}
```

### 3. Get File Metadata

**Endpoint**: `GET /api/v1/files/{fileId}`

**Response**:
```json
{
  "success": true,
  "data": {
    "fileId": "uuid-string",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "mimeType": "application/pdf",
    "sha256Hash": "...",
    "uploadedAt": "2026-08-02T10:30:00Z",
    "lastModified": "2026-08-02T10:30:00Z",
    "folderId": "uuid-string",
    "folderPath": "/Projects/Documents",
    "owner": {
      "userId": "uuid-string",
      "email": "user@example.com",
      "name": "John Doe"
    },
    "downloadCount": 5
  }
}
```

### 4. List Files

**Endpoint**: `GET /api/v1/files?folderId={folderId}&page=0&size=20&sort=uploadedAt,desc`

**Response**:
```json
{
  "success": true,
  "data": {
    "files": [
      {
        "fileId": "uuid-1",
        "fileName": "document.pdf",
        "fileSize": 1048576,
        "mimeType": "application/pdf",
        "uploadedAt": "2026-08-02T10:30:00Z"
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 5,
      "totalElements": 98,
      "pageSize": 20
    }
  }
}
```

### 5. Delete File

**Endpoint**: `DELETE /api/v1/files/{fileId}`

**Response**:
```json
{
  "success": true,
  "message": "File deleted successfully"
}
```

### 6. Create Folder

**Endpoint**: `POST /api/v1/folders`

**Request**:
```json
{
  "folderName": "Projects",
  "parentFolderId": "uuid-string"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "folderId": "uuid-string",
    "folderName": "Projects",
    "parentFolderId": "uuid-string",
    "path": "/Documents/Projects",
    "createdAt": "2026-08-02T10:30:00Z"
  }
}
```

## Database Schema

### File Metadata Table
```sql
CREATE TABLE file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_extension VARCHAR(20),
    
    sha256_hash VARCHAR(64) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    
    download_count INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_folder_id (folder_id),
    INDEX idx_sha256_hash (sha256_hash),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted_at (deleted_at)
);
```

### Folders Table
```sql
CREATE TABLE folders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
    
    folder_name VARCHAR(255) NOT NULL,
    folder_path TEXT NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_parent_folder_id (parent_folder_id),
    INDEX idx_folder_path (folder_path(255)),
    
    CONSTRAINT unique_folder_per_user UNIQUE(user_id, parent_folder_id, folder_name)
);
```

## Class Diagram

```
┌──────────────────────────┐
│    FileController        │
├──────────────────────────┤
│ - fileService            │
├──────────────────────────┤
│ + uploadFile()           │
│ + downloadFile()         │
│ + getFileMetadata()      │
│ + listFiles()            │
│ + deleteFile()           │
│ + searchFiles()          │
└──────────────────────────┘
            │
            │ uses
            v
┌──────────────────────────┐
│      FileService         │
├──────────────────────────┤
│ - fileRepository         │
│ - s3StorageService       │
│ - metadataService        │
│ - redisTemplate          │
├──────────────────────────┤
│ + uploadFile()           │
│ + downloadFile()         │
│ + deleteFile()           │
│ + calculateHash()        │
│ + checkDuplicate()       │
│ + updateStorageQuota()   │
└──────────────────────────┘
            │
            │ uses
            v
┌──────────────────────────┐
│   S3StorageService       │
├──────────────────────────┤
│ - s3Client               │
│ - bucketName             │
├──────────────────────────┤
│ + uploadToS3()           │
│ + downloadFromS3()       │
│ + deleteFromS3()         │
│ + generatePresignedUrl() │
│ + getS3Key()             │
└──────────────────────────┘
```

## S3 Storage Structure

### Bucket Organization
```
ziboto-storage/
├── users/
│   ├── {userId-1}/
│   │   ├── files/
│   │   │   ├── {fileId-1}/
│   │   │   │   └── original-filename.pdf
│   │   │   └── {fileId-2}/
│   │   │       └── image.jpg
│   │   └── thumbnails/
│   │       └── {fileId-2}/
│   │           └── thumb_image.jpg
│   └── {userId-2}/
│       └── files/
│           └── ...
```

### S3 Key Format
```
users/{userId}/files/{fileId}/{sanitizedFileName}
```

## File Upload Processing Logic

### Standard Upload (Files < 100MB)

```java
public FileMetadata uploadFile(MultipartFile file, UUID userId, UUID folderId) {
    // 1. Check file size - redirect to multipart if needed
    if (file.getSize() > 100 * 1024 * 1024) {
        throw new FileTooLargeException("Use multipart upload for files >100MB");
    }
    
    // 2. Validate file
    validateFile(file);
    
    // 3. Check storage quota
    checkStorageQuota(userId, file.getSize());
    
    // 3. Calculate SHA-256 hash
    String sha256Hash = calculateSHA256(file.getInputStream());
    
    // 4. Check for duplicate (optional deduplication)
    Optional<FileMetadata> duplicate = checkDuplicate(userId, sha256Hash);
    if (duplicate.isPresent()) {
        return handleDuplicate(duplicate.get(), folderId);
    }
    
    // 5. Generate unique file ID
    UUID fileId = UUID.randomUUID();
    
    // 6. Upload to S3
    String s3Key = s3StorageService.uploadFile(userId, fileId, file);
    
    // 7. Create metadata
    FileMetadata metadata = FileMetadata.builder()
        .id(fileId)
        .userId(userId)
        .folderId(folderId)
        .fileName(file.getOriginalFilename())
        .fileSize(file.getSize())
        .mimeType(file.getContentType())
        .sha256Hash(sha256Hash)
        .s3Key(s3Key)
        .build();
    
    // 8. Save to database
    fileRepository.save(metadata);
    
    // 9. Update user storage
    updateUserStorage(userId, file.getSize());
    
    // 10. Cache metadata in Redis
    cacheFileMetadata(metadata);
    
    return metadata;
}
```

## File Download with Streaming

```java
public void downloadFile(UUID fileId, HttpServletResponse response) {
    // 1. Get metadata from cache or DB
    FileMetadata metadata = getFileMetadata(fileId);
    
    // 2. Check permissions
    checkDownloadPermission(metadata);
    
    // 3. Set response headers
    response.setContentType(metadata.getMimeType());
    response.setHeader("Content-Disposition", 
        "attachment; filename=\"" + metadata.getFileName() + "\"");
    response.setContentLengthLong(metadata.getFileSize());
    
    // 4. Stream from S3
    try (InputStream s3Stream = s3StorageService.getFileStream(metadata.getS3Key());
         OutputStream responseStream = response.getOutputStream()) {
        
        IOUtils.copy(s3Stream, responseStream);
        responseStream.flush();
    }
    
    // 5. Increment download counter (async)
    incrementDownloadCount(fileId);
}
```

## Redis Caching Strategy

### File Metadata Cache
```
Key: file_metadata:{fileId}
TTL: 1 hour
Value: {JSON serialized FileMetadata}
```

### Folder Structure Cache
```
Key: folder_tree:{userId}:{folderId}
TTL: 30 minutes
Value: {JSON array of files and subfolders}
```

### User Storage Stats Cache
```
Key: user_storage:{userId}
TTL: 5 minutes
Value: {
  "quotaBytes": 5368709120,
  "usedBytes": 1234567890,
  "fileCount": 42
}
```

## Validation Rules

| Validation | Rule |
|------------|------|
| File size | Max 500MB per file (configurable) |
| File name | Max 255 characters, sanitized |
| MIME type | Whitelist of allowed types |
| Storage quota | Check before upload |
| Folder depth | Max 10 levels |
| File count | Max 10,000 files per user (v1) |

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| FILE_001 | 400 | Invalid file format |
| FILE_002 | 413 | File too large |
| FILE_003 | 507 | Storage quota exceeded |
| FILE_004 | 404 | File not found |
| FILE_005 | 403 | Access denied |
| FILE_006 | 409 | Duplicate file |
| FILE_007 | 400 | Invalid folder structure |

## Performance Optimizations

1. **Multipart Upload**: For files > 100MB, use S3 multipart upload (see [S3 Multipart Upload](06-s3-multipart-upload.md))
   - Upload parts in parallel
   - Resume capability for interrupted uploads
   - Better network resilience
2. **Streaming**: Stream files directly from S3 without loading into memory
3. **Caching**: Cache frequently accessed metadata in Redis
4. **Async Processing**: Background tasks for hash calculation, thumbnail generation
5. **Connection Pooling**: Reuse S3 client connections

## Security Measures

1. **File Validation**: Check MIME type, extension, and magic bytes
2. **Virus Scanning**: (v2) Integrate with antivirus service
3. **Presigned URLs**: Generate time-limited S3 URLs for downloads
4. **Access Control**: Verify user ownership before operations
5. **Input Sanitization**: Sanitize filenames to prevent path traversal

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team
