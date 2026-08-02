# Low-Level Design: AWS S3 Multipart Upload

## Overview
Detailed implementation of AWS S3 multipart upload for handling large files (>100MB) efficiently with resume capability, parallel uploads, and progress tracking.

## Why Multipart Upload?

### Benefits
1. **Large File Support**: Upload files up to 5TB
2. **Improved Throughput**: Parallel upload of parts
3. **Resume Capability**: Continue interrupted uploads
4. **Network Resilience**: Retry individual parts instead of entire file
5. **Better User Experience**: Real-time progress tracking

### When to Use
- Files > 100MB: Always use multipart
- Files 5MB-100MB: Optional, based on network conditions
- Files < 5MB: Use standard upload

## Architecture

### High-Level Flow with Chunking

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT SIDE                               │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Original File: large-video.mp4 (500MB)             │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                  │
│                           │ Split into chunks                │
│                           v                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           File Chunking Process                     │   │
│  │                                                      │   │
│  │  Chunk 1:  [0 MB ─────► 10 MB]   (Part 1)         │   │
│  │  Chunk 2:  [10 MB ────► 20 MB]   (Part 2)         │   │
│  │  Chunk 3:  [20 MB ────► 30 MB]   (Part 3)         │   │
│  │  Chunk 4:  [30 MB ────► 40 MB]   (Part 4)         │   │
│  │  ...                                                │   │
│  │  Chunk 50: [490 MB ───► 500 MB]  (Part 50)        │   │
│  │                                                      │   │
│  │  Total: 50 parts × 10MB each                       │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ Upload flow starts
                           v
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ 1. Initiate Upload Request
       │    (file metadata)
       v
┌──────────────────────────┐
│   FileUploadController   │
└──────────┬───────────────┘
           │ 2. Check file size
           │    If > 100MB
           v
┌──────────────────────────┐
│ MultipartUploadService   │
└──────────┬───────────────┘
           │ 3. Initiate Multipart Upload
           v
┌──────────────────────────┐
│    AWS S3 Service        │
│  (S3 SDK Client)         │
└──────────┬───────────────┘
           │ 4. Return Upload ID
           v
┌──────────────────────────┐
│   Upload Session Cache   │
│      (Redis)             │
└──────────┬───────────────┘
           │ 5. Store session metadata
           │
           v
    [Return upload session to client]

           │
┌──────────┴─────────────┐
│      Client            │
│  Upload Parts Loop     │
└──────────┬─────────────┘
           │ 6. Upload Part 1, 2, 3... N
           │    (chunk by chunk)
           v
┌──────────────────────────┐
│ MultipartUploadService   │
│  - Validate part         │
│  - Upload to S3          │
│  - Store ETags           │
└──────────┬───────────────┘
           │ 7. All parts uploaded
           v
┌──────────────────────────┐
│  Complete Multipart      │
│  Upload (S3)             │
└──────────┬───────────────┘
           │ 8. S3 assembles file
           v
┌──────────────────────────┐
│  Store File Metadata     │
│  (PostgreSQL)            │
└──────────────────────────┘
```

## Upload Flow Sequence

### Phase 1: Initiation

**Step 1: Client initiates upload**
```http
POST /api/v1/files/multipart/initiate
Authorization: Bearer {jwt-token}
Content-Type: application/json


{
  "fileName": "large-video.mp4",
  "fileSize": 524288000,
  "mimeType": "video/mp4",
  "folderId": "550e8400-e29b-41d4-a716-446655440000",
  "sha256Hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

**Step 2: Server response**
```json
{
  "success": true,
  "data": {
    "uploadId": "upload-session-uuid",
    "s3UploadId": "VXBsb2FkIElEIGZvciA2aWWpbmcncyBteS1tb3ZpZS5tMnRzIHVwbG9hZA",
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "partSize": 10485760,
    "totalParts": 50,
    "expiresAt": "2026-08-02T12:30:00Z"
  }
}
```

### Phase 2: Upload Parts

**Step 3: Upload individual parts**
```http
PUT /api/v1/files/multipart/upload/{uploadId}/part/{partNumber}
Authorization: Bearer {jwt-token}
Content-Type: application/octet-stream
Content-Length: 10485760

[Binary data - 10MB chunk]
```

**Step 4: Part upload response**
```json
{
  "success": true,
  "data": {
    "partNumber": 1,

    "eTag": "\"3858f62230ac3c915f300c664312c63f\"",
    "uploadedBytes": 10485760,
    "totalBytes": 524288000,
    "progress": 2.0
  }
}
```

### Phase 3: Completion

**Step 5: Complete upload**
```http
POST /api/v1/files/multipart/complete/{uploadId}
Authorization: Bearer {jwt-token}
```

**Step 6: Completion response**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "fileId": "660e8400-e29b-41d4-a716-446655440000",
    "fileName": "large-video.mp4",
    "fileSize": 524288000,
    "s3Key": "users/{userId}/files/{fileId}/large-video.mp4",
    "uploadedAt": "2026-08-02T10:45:00Z"
  }
}
```

## Implementation Classes

### 1. MultipartUploadController

```java
@RestController
@RequestMapping("/api/v1/files/multipart")
@RequiredArgsConstructor
public class MultipartUploadController {
    
    private final MultipartUploadService multipartUploadService;
    

    @PostMapping("/initiate")
    public ResponseEntity<InitiateUploadResponse> initiateUpload(
            @Valid @RequestBody InitiateUploadRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        InitiateUploadResponse response = multipartUploadService
            .initiateUpload(user.getUserId(), request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/upload/{uploadId}/part/{partNumber}")
    public ResponseEntity<UploadPartResponse> uploadPart(
            @PathVariable String uploadId,
            @PathVariable Integer partNumber,
            @RequestBody byte[] partData,
            @AuthenticationPrincipal UserPrincipal user) {
        
        UploadPartResponse response = multipartUploadService
            .uploadPart(uploadId, partNumber, partData, user.getUserId());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/complete/{uploadId}")
    public ResponseEntity<CompleteUploadResponse> completeUpload(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        CompleteUploadResponse response = multipartUploadService
            .completeUpload(uploadId, user.getUserId());
        
        return ResponseEntity.ok(response);
    }

    
    @DeleteMapping("/abort/{uploadId}")
    public ResponseEntity<Void> abortUpload(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        multipartUploadService.abortUpload(uploadId, user.getUserId());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/status/{uploadId}")
    public ResponseEntity<UploadStatusResponse> getUploadStatus(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        UploadStatusResponse response = multipartUploadService
            .getUploadStatus(uploadId, user.getUserId());
        
        return ResponseEntity.ok(response);
    }
}
```

### 2. MultipartUploadService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MultipartUploadService {
    
    private final S3Client s3Client;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FileMetadataRepository fileMetadataRepository;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    private static final long PART_SIZE = 10 * 1024 * 1024; // 10MB

    private static final long MIN_PART_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_PART_SIZE = 5 * 1024 * 1024 * 1024; // 5GB
    private static final int SESSION_EXPIRE_HOURS = 24;
    
    public InitiateUploadResponse initiateUpload(UUID userId, 
                                                 InitiateUploadRequest request) {
        
        // 1. Validate file size and storage quota
        validateUpload(userId, request.getFileSize());
        
        // 2. Calculate number of parts
        int totalParts = (int) Math.ceil((double) request.getFileSize() / PART_SIZE);
        
        // 3. Generate S3 key
        UUID fileId = UUID.randomUUID();
        String s3Key = generateS3Key(userId, fileId, request.getFileName());
        
        // 4. Initiate multipart upload on S3
        CreateMultipartUploadRequest s3Request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(request.getMimeType())
            .metadata(Map.of(
                "user-id", userId.toString(),
                "file-id", fileId.toString(),
                "original-filename", request.getFileName()
            ))
            .build();
        
        CreateMultipartUploadResponse s3Response = s3Client
            .createMultipartUpload(s3Request);
        

        // 5. Create upload session
        String uploadId = UUID.randomUUID().toString();
        UploadSession session = UploadSession.builder()
            .uploadId(uploadId)
            .s3UploadId(s3Response.uploadId())
            .userId(userId)
            .fileId(fileId)
            .fileName(request.getFileName())
            .fileSize(request.getFileSize())
            .mimeType(request.getMimeType())
            .folderId(request.getFolderId())
            .s3Key(s3Key)
            .partSize(PART_SIZE)
            .totalParts(totalParts)
            .uploadedParts(new HashMap<>())
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(SESSION_EXPIRE_HOURS))
            .build();
        
        // 6. Store session in Redis
        String sessionKey = "upload_session:" + uploadId;
        redisTemplate.opsForValue().set(
            sessionKey, 
            session, 
            SESSION_EXPIRE_HOURS, 
            TimeUnit.HOURS
        );
        
        log.info("Initiated multipart upload: uploadId={}, fileId={}, parts={}", 
            uploadId, fileId, totalParts);
        
        return InitiateUploadResponse.from(session);
    }
    

    public UploadPartResponse uploadPart(String uploadId, 
                                         Integer partNumber, 
                                         byte[] partData, 
                                         UUID userId) {
        
        // 1. Get upload session
        UploadSession session = getUploadSession(uploadId);
        
        // 2. Validate ownership
        validateOwnership(session, userId);
        
        // 3. Validate part number
        if (partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new InvalidPartNumberException(partNumber, session.getTotalParts());
        }
        
        // 4. Validate part size
        validatePartSize(partData.length, partNumber, session.getTotalParts());
        
        // 5. Upload part to S3
        UploadPartRequest s3Request = UploadPartRequest.builder()
            .bucket(bucketName)
            .key(session.getS3Key())
            .uploadId(session.getS3UploadId())
            .partNumber(partNumber)
            .contentLength((long) partData.length)
            .build();
        
        UploadPartResponse s3Response = s3Client.uploadPart(
            s3Request,
            RequestBody.fromBytes(partData)
        );
        

        // 6. Store ETag in session
        session.getUploadedParts().put(partNumber, s3Response.eTag());
        updateUploadSession(session);
        
        // 7. Calculate progress
        long uploadedBytes = (long) session.getUploadedParts().size() * PART_SIZE;
        double progress = (uploadedBytes * 100.0) / session.getFileSize();
        
        log.info("Uploaded part: uploadId={}, partNumber={}, progress={}%", 
            uploadId, partNumber, String.format("%.2f", progress));
        
        return UploadPartResponse.builder()
            .partNumber(partNumber)
            .eTag(s3Response.eTag())
            .uploadedBytes(uploadedBytes)
            .totalBytes(session.getFileSize())
            .progress(progress)
            .build();
    }
    
    public CompleteUploadResponse completeUpload(String uploadId, UUID userId) {
        
        // 1. Get upload session
        UploadSession session = getUploadSession(uploadId);
        
        // 2. Validate ownership
        validateOwnership(session, userId);
        
        // 3. Validate all parts uploaded
        if (session.getUploadedParts().size() != session.getTotalParts()) {
            throw new IncompleteUploadException(
                session.getUploadedParts().size(), 
                session.getTotalParts()
            );
        }

        
        // 4. Build parts list for S3
        List<CompletedPart> completedParts = session.getUploadedParts().entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> CompletedPart.builder()
                .partNumber(entry.getKey())
                .eTag(entry.getValue())
                .build())
            .collect(Collectors.toList());
        
        // 5. Complete multipart upload on S3
        CompleteMultipartUploadRequest s3Request = CompleteMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(session.getS3Key())
            .uploadId(session.getS3UploadId())
            .multipartUpload(CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build())
            .build();
        
        CompleteMultipartUploadResponse s3Response = s3Client
            .completeMultipartUpload(s3Request);
        
        // 6. Save file metadata to database
        FileMetadata metadata = FileMetadata.builder()
            .id(session.getFileId())
            .userId(session.getUserId())
            .folderId(session.getFolderId())
            .fileName(session.getFileName())
            .fileSize(session.getFileSize())
            .mimeType(session.getMimeType())
            .s3Bucket(bucketName)

            .s3Key(session.getS3Key())
            .s3VersionId(s3Response.versionId())
            .build();
        
        fileMetadataRepository.save(metadata);
        
        // 7. Clean up session
        deleteUploadSession(uploadId);
        
        log.info("Completed multipart upload: uploadId={}, fileId={}", 
            uploadId, session.getFileId());
        
        return CompleteUploadResponse.from(metadata);
    }
    
    public void abortUpload(String uploadId, UUID userId) {
        
        // 1. Get upload session
        UploadSession session = getUploadSession(uploadId);
        
        // 2. Validate ownership
        validateOwnership(session, userId);
        
        // 3. Abort multipart upload on S3
        AbortMultipartUploadRequest s3Request = AbortMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(session.getS3Key())
            .uploadId(session.getS3UploadId())
            .build();
        
        s3Client.abortMultipartUpload(s3Request);
        
        // 4. Clean up session
        deleteUploadSession(uploadId);
        
        log.info("Aborted multipart upload: uploadId={}", uploadId);
    }

    
    // Helper methods
    
    private UploadSession getUploadSession(String uploadId) {
        String sessionKey = "upload_session:" + uploadId;
        UploadSession session = (UploadSession) redisTemplate
            .opsForValue().get(sessionKey);
        
        if (session == null) {
            throw new UploadSessionNotFoundException(uploadId);
        }
        
        return session;
    }
    
    private void updateUploadSession(UploadSession session) {
        String sessionKey = "upload_session:" + session.getUploadId();
        redisTemplate.opsForValue().set(
            sessionKey, 
            session, 
            SESSION_EXPIRE_HOURS, 
            TimeUnit.HOURS
        );
    }
    
    private void deleteUploadSession(String uploadId) {
        String sessionKey = "upload_session:" + uploadId;
        redisTemplate.delete(sessionKey);
    }
    
    private String generateS3Key(UUID userId, UUID fileId, String fileName) {
        String sanitizedFileName = sanitizeFileName(fileName);
        return String.format("users/%s/files/%s/%s", 
            userId, fileId, sanitizedFileName);
    }
}
```

## Data Models

### UploadSession (Redis)


```java
@Data
@Builder
public class UploadSession implements Serializable {
    private String uploadId;              // Internal upload ID
    private String s3UploadId;            // S3 multipart upload ID
    private UUID userId;
    private UUID fileId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private UUID folderId;
    private String s3Key;
    private Long partSize;
    private Integer totalParts;
    private Map<Integer, String> uploadedParts; // partNumber -> ETag
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
```

### Request/Response DTOs

```java
@Data
public class InitiateUploadRequest {
    @NotBlank
    @Size(max = 255)
    private String fileName;
    
    @NotNull
    @Min(1)
    private Long fileSize;
    
    @NotBlank
    private String mimeType;
    
    private UUID folderId;
    
    private String sha256Hash;
}

@Data
@Builder
public class InitiateUploadResponse {
    private String uploadId;
    private String s3UploadId;
    private UUID fileId;
    private Long partSize;
    private Integer totalParts;
    private LocalDateTime expiresAt;
}


@Data
@Builder
public class UploadPartResponse {
    private Integer partNumber;
    private String eTag;
    private Long uploadedBytes;
    private Long totalBytes;
    private Double progress;
}

@Data
@Builder
public class CompleteUploadResponse {
    private UUID fileId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String s3Key;
    private LocalDateTime uploadedAt;
}

@Data
@Builder
public class UploadStatusResponse {
    private String uploadId;
    private UUID fileId;
    private String fileName;
    private Long fileSize;
    private Integer totalParts;
    private Integer uploadedParts;
    private Double progress;
    private String status; // IN_PROGRESS, COMPLETED, ABORTED, EXPIRED
    private LocalDateTime expiresAt;
    private List<Integer> missingParts;
}
```

## Redis Cache Structure

### Upload Session Key Pattern
```
Key: upload_session:{uploadId}
TTL: 24 hours
Value: JSON serialized UploadSession object
```

### Example Redis Commands
```redis
# Store session
SET upload_session:abc123 '{"uploadId":"abc123",...}' EX 86400

# Get session
GET upload_session:abc123

# Delete session
DEL upload_session:abc123

# Get all active uploads for monitoring
KEYS upload_session:*
```

## Configuration

### application.yml
```yaml
aws:
  s3:
    bucket: ziboto-storage
    region: us-east-1
    multipart:
      part-size: 10485760  # 10MB
      min-part-size: 5242880  # 5MB
      max-part-size: 5368709120  # 5GB
      session-expire-hours: 24
      max-concurrent-uploads: 10

spring:
  servlet:
    multipart:
      max-file-size: 10MB  # For standard uploads
      max-request-size: 10MB
```

### S3 Client Configuration
```java
@Configuration
public class S3Config {
    
    @Value("${aws.s3.region}")
    private String region;
    
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    @Bean
    public S3TransferManager transferManager(S3Client s3Client) {
        return S3TransferManager.builder()
            .s3Client(s3Client)
            .build();
    }
}
```

## Client-Side Implementation

### JavaScript/TypeScript Example
```typescript
interface UploadSession {
  uploadId: string;
  s3UploadId: string;
  fileId: string;
  partSize: number;
  totalParts: number;
  expiresAt: string;
}

class MultipartUploader {
  private file: File;
  private session: UploadSession | null = null;
  private uploadedParts: Set<number> = new Set();
  
  constructor(file: File) {
    this.file = file;
  }
  
  async upload(
    onProgress?: (progress: number) => void
  ): Promise<string> {
    
    // Step 1: Initiate upload
    this.session = await this.initiateUpload();
    
    // Step 2: Upload parts
    const promises: Promise<void>[] = [];
    
    for (let partNumber = 1; partNumber <= this.session.totalParts; partNumber++) {
      promises.push(this.uploadPart(partNumber, onProgress));
    }
    
    await Promise.all(promises);
    
    // Step 3: Complete upload
    const result = await this.completeUpload();
    
    return result.fileId;
  }
  
  private async initiateUpload(): Promise<UploadSession> {
    const response = await fetch('/api/v1/files/multipart/initiate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getAccessToken()}`
      },
      body: JSON.stringify({
        fileName: this.file.name,
        fileSize: this.file.size,
        mimeType: this.file.type
      })
    });
    
    const data = await response.json();
    return data.data;
  }
  
  private async uploadPart(
    partNumber: number,
    onProgress?: (progress: number) => void
  ): Promise<void> {
    
    const start = (partNumber - 1) * this.session!.partSize;
    const end = Math.min(start + this.session!.partSize, this.file.size);
    const chunk = this.file.slice(start, end);
    
    const response = await fetch(
      `/api/v1/files/multipart/upload/${this.session!.uploadId}/part/${partNumber}`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/octet-stream',
          'Authorization': `Bearer ${getAccessToken()}`
        },
        body: chunk
      }
    );
    
    const data = await response.json();
    
    this.uploadedParts.add(partNumber);
    
    if (onProgress) {
      onProgress(data.data.progress);
    }
  }
  
  private async completeUpload(): Promise<any> {
    const response = await fetch(
      `/api/v1/files/multipart/complete/${this.session!.uploadId}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${getAccessToken()}`
        }
      }
    );
    
    const data = await response.json();
    return data.data;
  }
  
  async abort(): Promise<void> {
    if (!this.session) return;
    
    await fetch(
      `/api/v1/files/multipart/abort/${this.session.uploadId}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${getAccessToken()}`
        }
      }
    );
  }
}

// Usage
const uploader = new MultipartUploader(file);

uploader.upload((progress) => {
  console.log(`Upload progress: ${progress.toFixed(2)}%`);
  updateProgressBar(progress);
})
.then(fileId => {
  console.log('Upload completed:', fileId);
})
.catch(error => {
  console.error('Upload failed:', error);
  uploader.abort();
});
```

## Error Handling

### Common Errors
```java
public class UploadSessionNotFoundException extends RuntimeException {
    public UploadSessionNotFoundException(String uploadId) {
        super("Upload session not found: " + uploadId);
    }
}

public class InvalidPartNumberException extends RuntimeException {
    public InvalidPartNumberException(int partNumber, int totalParts) {
        super(String.format("Invalid part number %d. Expected 1-%d", 
            partNumber, totalParts));
    }
}

public class IncompleteUploadException extends RuntimeException {
    public IncompleteUploadException(int uploaded, int total) {
        super(String.format("Upload incomplete: %d/%d parts uploaded", 
            uploaded, total));
    }
}

public class UploadExpiredException extends RuntimeException {
    public UploadExpiredException(String uploadId) {
        super("Upload session expired: " + uploadId);
    }
}
```

### Error Response Examples
```json
// 404 Not Found - Session Not Found
{
  "success": false,
  "error": {
    "code": "UPLOAD_SESSION_NOT_FOUND",
    "message": "Upload session not found or expired"
  }
}

// 400 Bad Request - Invalid Part Number
{
  "success": false,
  "error": {
    "code": "INVALID_PART_NUMBER",
    "message": "Invalid part number 51. Expected 1-50"
  }
}

// 400 Bad Request - Part Too Small
{
  "success": false,
  "error": {
    "code": "PART_TOO_SMALL",
    "message": "Part size must be at least 5MB (except last part)",
    "minSize": 5242880
  }
}
```

## Monitoring and Metrics

### Key Metrics to Track
```java
@Component
public class MultipartUploadMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordUploadInitiated() {
        meterRegistry.counter("multipart.upload.initiated").increment();
    }
    
    public void recordUploadCompleted(Duration duration) {
        meterRegistry.counter("multipart.upload.completed").increment();
        meterRegistry.timer("multipart.upload.duration").record(duration);
    }
    
    public void recordUploadAborted() {
        meterRegistry.counter("multipart.upload.aborted").increment();
    }
    
    public void recordPartUploaded(long partSize) {
        meterRegistry.counter("multipart.part.uploaded").increment();
        meterRegistry.summary("multipart.part.size").record(partSize);
    }
}
```

### CloudWatch Metrics
- Total multipart uploads initiated
- Total multipart uploads completed
- Total multipart uploads aborted
- Average upload duration
- Average part upload time
- Failed part uploads
- Active upload sessions

## Performance Optimization

### 1. Parallel Part Uploads
```java
public void uploadPartsInParallel(UploadSession session, File file) {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Future<String>> futures = new ArrayList<>();
    
    for (int i = 1; i <= session.getTotalParts(); i++) {
        final int partNumber = i;
        futures.add(executor.submit(() -> 
            uploadPartInternal(session, file, partNumber)
        ));
    }
    
    // Wait for all parts
    for (Future<String> future : futures) {
        future.get();
    }
    
    executor.shutdown();
}
```

### 2. Retry with Exponential Backoff
```java
public String uploadPartWithRetry(UploadSession session, 
                                  byte[] partData, 
                                  int partNumber) {
    int maxRetries = 3;
    int attempt = 0;
    
    while (attempt < maxRetries) {
        try {
            return uploadPart(session, partData, partNumber);
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxRetries) {
                throw e;
            }
            
            long waitTime = (long) Math.pow(2, attempt) * 1000;
            Thread.sleep(waitTime);
        }
    }
    
    throw new RuntimeException("Failed to upload part after retries");
}
```

### 3. Part Size Optimization
- **Small files (100-500MB)**: 10MB parts
- **Medium files (500MB-5GB)**: 50MB parts
- **Large files (>5GB)**: 100MB parts

## Cleanup and Maintenance

### Cleanup Expired Sessions
```java
@Scheduled(cron = "0 0 * * * *") // Every hour
public void cleanupExpiredSessions() {
    Set<String> keys = redisTemplate.keys("upload_session:*");
    
    if (keys == null || keys.isEmpty()) {
        return;
    }
    
    int cleaned = 0;
    for (String key : keys) {
        UploadSession session = getUploadSession(
            key.replace("upload_session:", "")
        );
        
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Abort S3 multipart upload
            abortS3Upload(session);
            // Delete Redis session
            redisTemplate.delete(key);
            cleaned++;
        }
    }
    
    log.info("Cleaned up {} expired upload sessions", cleaned);
}
```

### S3 Lifecycle Policy
```json
{
  "Rules": [{
    "Id": "AbortIncompleteMultipartUpload",
    "Status": "Enabled",
    "AbortIncompleteMultipartUpload": {
      "DaysAfterInitiation": 2
    }
  }]
}
```

## Security Considerations

1. **Authentication**: Verify JWT token for all multipart operations
2. **Authorization**: Validate user owns the upload session
3. **Rate Limiting**: Prevent abuse with strict rate limits
4. **Storage Quota**: Check quota before initiating upload
5. **File Type Validation**: Validate MIME type and extension
6. **Session Expiration**: Automatically expire sessions after 24 hours
7. **Part Validation**: Validate part size and sequence
8. **S3 Bucket Policy**: Restrict access to authenticated users only

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team  
**Related Documents**:
- [File Management System](02-file-management-system.md)
- [Caching Strategy](03-caching-strategy.md)
- [API Specifications](05-api-specifications.md)
