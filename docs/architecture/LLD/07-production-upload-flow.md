# Production-Ready Upload Flow Architecture

## Overview
This document describes the production-grade, real-world implementation of file upload for Ziboto using presigned URLs, direct client-to-S3 uploads, and industry best practices.

## Complete Production Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (React)                                 │
│                                                                          │
│  User Interface: File Selection + Upload Progress                       │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 1. User Authentication
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     Spring Boot Authentication                           │
│                                                                          │
│  POST /api/v1/auth/login                                                │
│  ✓ Validate credentials                                                 │
│  ✓ Generate JWT Access Token (15 min)                                   │
│  ✓ Generate JWT Refresh Token (7 days)                                  │
│  ✓ Store session in Redis                                               │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ Returns: JWT Tokens
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (React)                                 │
│                                                                          │
│  ✓ Store tokens in memory/localStorage                                  │
│  ✓ User selects file (e.g., video.mp4, 500MB)                          │
│  ✓ Check file size → >100MB → Use multipart upload                     │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 2. Request Upload Session
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                  Request Upload Session                                  │
│                                                                          │
│  POST /api/v1/files/multipart/initiate                                  │
│  Authorization: Bearer {jwt-token}                                       │
│                                                                          │
│  Request Body:                                                           │
│  {                                                                       │
│    "fileName": "video.mp4",                                             │
│    "fileSize": 524288000,                                               │
│    "mimeType": "video/mp4",                                             │
│    "folderId": "uuid-folder-id",                                        │
│    "sha256Hash": "abc123..."  // Optional for deduplication             │
│  }                                                                       │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Upload Service                             │
│                                                                          │
│  ✓ Validate JWT (JwtAuthenticationFilter)                               │
│  ✓ Extract userId from token                                            │
│  ✓ Check rate limiting (Redis: 50 req/hour)                             │
│  ✓ Validate file size (max 5GB per file)                                │
│  ✓ Validate file type (whitelist: video/*, image/*, application/pdf)    │
│  ✓ Check storage quota (query PostgreSQL)                               │
│     - User quota: 5GB, Used: 2GB, Available: 3GB                        │
│     - Requested: 500MB → ✓ OK                                           │
│  ✓ Check for duplicate (SHA256 hash in DB) → Optional                  │
│  ✓ Generate unique Upload ID (UUID)                                     │
│  ✓ Generate unique File ID (UUID)                                       │
│  ✓ Calculate chunk strategy:                                            │
│     - fileSize: 500MB                                                    │
│     - chunkSize: 10MB                                                    │
│     - totalParts: ceil(500/10) = 50 chunks                              │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 3. Initiate S3 Multipart Upload
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          AWS S3 Service                                  │
│                                                                          │
│  S3 API: CreateMultipartUpload                                          │
│  Bucket: ziboto-storage                                                 │
│  Key: users/{userId}/files/{fileId}/video.mp4                          │
│                                                                          │
│  Returns: s3UploadId = "VXBsb2FkIElEIGZvciA2aWW..."                    │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 4. Generate Presigned URLs
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Upload Service                             │
│                                                                          │
│  Generate 50 Presigned URLs (one per chunk):                            │
│                                                                          │
│  For each part (1 to 50):                                               │
│    presignedUrl = s3Client.generatePresignedUrl(                        │
│      bucket: "ziboto-storage",                                          │
│      key: "users/{userId}/files/{fileId}/video.mp4",                   │
│      uploadId: s3UploadId,                                              │
│      partNumber: partNumber,                                            │
│      expiration: 2 hours,                                               │
│      method: PUT                                                         │
│    )                                                                     │
│                                                                          │
│  presignedUrls[1] = "https://s3.amazonaws.com/...?signature=..."       │
│  presignedUrls[2] = "https://s3.amazonaws.com/...?signature=..."       │
│  ...                                                                     │
│  presignedUrls[50] = "https://s3.amazonaws.com/...?signature=..."      │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 5. Store Upload Session
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          Redis Cache                                     │
│                                                                          │
│  Key: upload_session:{uploadId}                                         │
│  TTL: 24 hours                                                           │
│  Value: {                                                                │
│    "uploadId": "uuid-upload-id",                                        │
│    "s3UploadId": "VXBsb2FkIElEIGZv...",                                │
│    "userId": "uuid-user-id",                                            │
│    "fileId": "uuid-file-id",                                            │
│    "fileName": "video.mp4",                                             │
│    "fileSize": 524288000,                                               │
│    "chunkSize": 10485760,                                               │
│    "totalParts": 50,                                                     │
│    "uploadedParts": {},  // Will store part# → ETag                     │
│    "presignedUrls": [...],  // All 50 URLs                              │
│    "createdAt": "2026-08-02T10:00:00Z",                                 │
│    "expiresAt": "2026-08-03T10:00:00Z"                                  │
│  }                                                                       │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 6. Return Response to Client
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (React)                                 │
│                                                                          │
│  Response Body:                                                          │
│  {                                                                       │
│    "success": true,                                                      │
│    "data": {                                                             │
│      "uploadId": "uuid-upload-id",                                      │
│      "fileId": "uuid-file-id",                                          │
│      "chunkSize": 10485760,                                             │
│      "totalParts": 50,                                                   │
│      "presignedUrls": [                                                  │
│        "https://s3.amazonaws.com/ziboto-storage/...?part=1&sig=...",   │
│        "https://s3.amazonaws.com/ziboto-storage/...?part=2&sig=...",   │
│        ...                                                               │
│      ],                                                                  │
│      "expiresAt": "2026-08-03T10:00:00Z"                                │
│    }                                                                     │
│  }                                                                       │
│                                                                          │
│  ✓ Client now has all presigned URLs                                    │
│  ✓ No need to authenticate for each chunk upload                        │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 7. Client Splits File Into Chunks
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         React Client                                     │
│                                                                          │
│  File Chunking Process:                                                  │
│                                                                          │
│  const file = selectedFile;  // 500MB                                   │
│  const chunkSize = 10485760;  // 10MB                                   │
│  const totalChunks = 50;                                                 │
│                                                                          │
│  chunks = [                                                              │
│    { part: 1,  data: file.slice(0, 10MB),        url: presignedUrls[0] },│
│    { part: 2,  data: file.slice(10MB, 20MB),     url: presignedUrls[1] },│
│    { part: 3,  data: file.slice(20MB, 30MB),     url: presignedUrls[2] },│
│    ...                                                                   │
│    { part: 50, data: file.slice(490MB, 500MB),   url: presignedUrls[49]}│
│  ]                                                                       │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 8. Parallel Direct Upload to S3
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                 Parallel Multipart Upload (Direct to S3)                 │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Concurrent Upload Pool (5 parallel uploads)                    │   │
│  │                                                                  │   │
│  │  Thread 1: Chunk 1  ────► PUT {presignedUrl[0]} ───► S3        │   │
│  │                           Body: [10MB binary]                    │   │
│  │                           ← Returns: ETag-1                      │   │
│  │                                                                  │   │
│  │  Thread 2: Chunk 2  ────► PUT {presignedUrl[1]} ───► S3        │   │
│  │                           Body: [10MB binary]                    │   │
│  │                           ← Returns: ETag-2                      │   │
│  │                                                                  │   │
│  │  Thread 3: Chunk 3  ────► PUT {presignedUrl[2]} ───► S3        │   │
│  │                           Body: [10MB binary]                    │   │
│  │                           ← Returns: ETag-3                      │   │
│  │                                                                  │   │
│  │  Thread 4: Chunk 4  ────► PUT {presignedUrl[3]} ───► S3        │   │
│  │                           Body: [10MB binary]                    │   │
│  │                           ← Returns: ETag-4                      │   │
│  │                                                                  │   │
│  │  Thread 5: Chunk 5  ────► PUT {presignedUrl[4]} ───► S3        │   │
│  │                           Body: [10MB binary]                    │   │
│  │                           ← Returns: ETag-5                      │   │
│  │                                                                  │   │
│  │  Wait for batch completion, then upload chunks 6-10...          │   │
│  │  Continue until all 50 chunks uploaded                          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  Features:                                                               │
│  ✓ Direct client-to-S3 (no backend bandwidth usage)                     │
│  ✓ Retry only failed chunks with exponential backoff                    │
│  ✓ Resume interrupted uploads (check uploaded parts)                    │
│  ✓ Real-time progress tracking (update UI after each chunk)             │
│  ✓ Network resilience (automatic retry on connection loss)              │
│                                                                          │
│  Progress Visualization:                                                 │
│    Part  1/50: ▓░░░░░░░░░ 2%   ETag-1                                  │
│    Part  5/50: ▓▓▓▓▓░░░░░ 10%  ETag-5                                  │
│    Part 25/50: ▓▓▓▓▓▓▓▓▓▓ 50%  ETag-25                                 │
│    Part 50/50: ▓▓▓▓▓▓▓▓▓▓ 100% ETag-50 ✓                               │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 9. Notify Backend of Part Upload
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                  Track Upload Progress (Optional)                        │
│                                                                          │
│  For each uploaded chunk, client notifies backend:                      │
│                                                                          │
│  POST /api/v1/files/multipart/track/{uploadId}                          │
│  Authorization: Bearer {jwt-token}                                       │
│  Body: {                                                                 │
│    "partNumber": 1,                                                      │
│    "eTag": "3858f62230ac3c915f300c664312c63f"                           │
│  }                                                                       │
│                                                                          │
│  Backend updates Redis:                                                  │
│    session.uploadedParts[1] = "ETag-1"                                  │
│    session.uploadedParts[2] = "ETag-2"                                  │
│    ...                                                                   │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 10. All Chunks Uploaded
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (React)                                 │
│                                                                          │
│  uploadedParts = {                                                       │
│    1: "ETag-1", 2: "ETag-2", 3: "ETag-3", ..., 50: "ETag-50"           │
│  }                                                                       │
│                                                                          │
│  ✓ All 50 chunks uploaded successfully                                  │
│  ✓ Ready to complete upload                                             │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 11. Complete Upload Request
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                  Complete Multipart Upload                               │
│                                                                          │
│  POST /api/v1/files/multipart/complete/{uploadId}                       │
│  Authorization: Bearer {jwt-token}                                       │
│                                                                          │
│  Request Body:                                                           │
│  {                                                                       │
│    "uploadedParts": {                                                    │
│      "1": "ETag-1",                                                      │
│      "2": "ETag-2",                                                      │
│      ...                                                                 │
│      "50": "ETag-50"                                                     │
│    }                                                                     │
│  }                                                                       │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot Upload Service                            │
│                                                                          │
│  ✓ Validate JWT                                                          │
│  ✓ Get upload session from Redis                                        │
│  ✓ Verify all 50 parts uploaded (compare ETags)                         │
│  ✓ Build CompletedPart list for S3                                      │
│  ✓ Calculate final file checksum (optional SHA-256)                     │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 12. Complete S3 Multipart Upload
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          AWS S3 Service                                  │
│                                                                          │
│  S3 API: CompleteMultipartUpload                                        │
│  UploadId: s3UploadId                                                   │
│  Parts: [                                                                │
│    { PartNumber: 1,  ETag: "ETag-1"  },                                │
│    { PartNumber: 2,  ETag: "ETag-2"  },                                │
│    ...                                                                   │
│    { PartNumber: 50, ETag: "ETag-50" }                                 │
│  ]                                                                       │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────┐       │
│  │  S3 ASSEMBLES FILE:                                          │       │
│  │                                                               │       │
│  │  Chunk 1 (10MB) ──┐                                         │       │
│  │  Chunk 2 (10MB) ──┤                                         │       │
│  │  Chunk 3 (10MB) ──┤                                         │       │
│  │  ...              ├──► Final File: video.mp4 (500MB)       │       │
│  │  Chunk 48 (10MB) ─┤                                         │       │
│  │  Chunk 49 (10MB) ─┤                                         │       │
│  │  Chunk 50 (10MB) ─┘                                         │       │
│  └─────────────────────────────────────────────────────────────┘       │
│                                                                          │
│  Returns:                                                                │
│    - S3 Object URL                                                       │
│    - Version ID                                                          │
│    - ETag (final file ETag)                                             │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                │ 13. Save Metadata to Database
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot Upload Service                            │
│                                                                          │
│  Create FileMetadata entity:                                            │
│  {                                                                       │
│    id: fileId,                                                          │
│    userId: userId,                                                      │
│    folderId: folderId,                                                  │
│    fileName: "video.mp4",                                               │
│    originalFileName: "video.mp4",                                       │
│    fileSize: 524288000,                                                 │
│    mimeType: "video/mp4",                                               │
│    sha256Hash: "abc123...",                                             │
│    s3Bucket: "ziboto-storage",                                          │
│    s3Key: "users/{userId}/files/{fileId}/video.mp4",                   │
│    s3VersionId: "version-id-from-s3",                                   │
│    uploadMethod: "MULTIPART",                                           │
│    multipartUploadId: s3UploadId,                                       │
│    downloadCount: 0,                                                     │
│    createdAt: "2026-08-02T10:45:00Z",                                   │
│    updatedAt: "2026-08-02T10:45:00Z"                                    │
│  }                                                                       │
│                                                                          │
│  ✓ Save to PostgreSQL                                                   │
│  ✓ Update user storage_used_bytes (+500MB)                              │
│  ✓ Cache metadata in Redis                                              │
│  ✓ Delete upload session from Redis                                     │
│  ✓ Create audit log entry                                               │
└──────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────┐        ┌────────────────────────────┐
│      PostgreSQL      │        │        AWS S3              │
├──────────────────────┤        ├────────────────────────────┤
│ file_metadata        │        │ Bucket: ziboto-storage     │
│                      │        │                            │
│ ✓ id (UUID)          │        │ Key: users/{userId}/       │
│ ✓ user_id            │        │      files/{fileId}/       │
│ ✓ folder_id          │        │      video.mp4             │
│ ✓ file_name          │        │                            │
│ ✓ file_size          │        │ Size: 500MB                │
│ ✓ mime_type          │        │ StorageClass: STANDARD     │
│ ✓ sha256_hash        │        │ Encryption: AES-256        │
│ ✓ s3_bucket          │        │                            │
│ ✓ s3_key             │        │ File is now accessible     │
│ ✓ s3_version_id      │        │ via S3 URL or presigned    │
│ ✓ upload_method      │        │ download URLs              │
│ ✓ download_count     │        │                            │
│ ✓ created_at         │        │                            │
│ ✓ updated_at         │        │                            │
└──────────────────────┘        └────────────────────────────┘
                                │
                                │ 14. Return Success Response
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (React)                                 │
│                                                                          │
│  Response Body:                                                          │
│  {                                                                       │
│    "success": true,                                                      │
│    "message": "File uploaded successfully",                              │
│    "data": {                                                             │
│      "fileId": "uuid-file-id",                                          │
│      "fileName": "video.mp4",                                           │
│      "fileSize": 524288000,                                             │
│      "mimeType": "video/mp4",                                           │
│      "uploadedAt": "2026-08-02T10:45:00Z",                              │
│      "downloadUrl": "/api/v1/files/{fileId}/download"                   │
│    }                                                                     │
│  }                                                                       │
│                                                                          │
│  ✓ Upload Complete!                                                     │
│  ✓ Update UI with success message                                       │
│  ✓ Show file in file list                                               │
│  ✓ User storage updated: 2GB → 2.5GB                                   │
└──────────────────────────────────────────────────────────────────────────┘
```

## Key Production Features

### 1. Security
- **JWT Authentication**: Every API call validated
- **Presigned URLs**: Time-limited (2 hours), signed access to S3
- **Rate Limiting**: Redis-based (50 uploads/hour per user)
- **Storage Quota**: Checked before upload initiation
- **File Type Validation**: Whitelist of allowed MIME types
- **Virus Scanning**: (Future) Integrate with ClamAV or AWS Macie

### 2. Performance
- **Direct Client-to-S3**: No backend bandwidth usage
- **Parallel Uploads**: 5 concurrent chunks
- **Presigned URLs**: Pre-generated, no authentication per chunk
- **Redis Caching**: Fast session lookup
- **Connection Pooling**: Reuse DB and S3 connections

### 3. Reliability
- **Automatic Retry**: Failed chunks retried with exponential backoff
- **Resume Capability**: Continue from last uploaded chunk
- **Session Expiration**: 24-hour TTL, automatic cleanup
- **Idempotent Operations**: Safe to retry
- **Audit Logging**: Track all upload operations

### 4. Scalability
- **Stateless Backend**: Session in Redis, scales horizontally
- **S3 for Storage**: Unlimited scalability
- **PostgreSQL**: Metadata only, not file content
- **Redis**: Fast distributed cache
- **Load Balancer**: Multiple backend instances

### 5. Monitoring
- **CloudWatch Metrics**: Upload success/failure rates
- **Application Logs**: Structured logging (JSON)
- **Performance Metrics**: Upload duration, chunk upload time
- **Error Tracking**: Sentry/Rollbar integration
- **User Analytics**: Track storage usage patterns

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team
