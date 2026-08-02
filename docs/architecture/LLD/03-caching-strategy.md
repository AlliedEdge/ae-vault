# Low-Level Design: Redis Caching Strategy

## Overview
Comprehensive caching strategy for Ziboto using Redis to optimize performance and reduce database load.

## Cache Architecture

```
┌─────────────┐
│  Controller │
└──────┬──────┘
       │
       v
┌──────────────────────────────────────────────┐
│           Service Layer                      │
│                                              │
│  ┌──────────────────────────────────────┐  │
│  │     Cache-Aside Pattern              │  │
│  │                                      │  │
│  │  1. Check Redis Cache ──────┐       │  │
│  │       │                      │       │  │
│  │       v                      │       │  │
│  │   Hit? Return        No      │       │  │
│  │       │                      │       │  │
│  │      Yes                     v       │  │
│  │       │              2. Query DB     │  │
│  │       │                      │       │  │
│  │       │                      v       │  │
│  │       │              3. Cache Result │  │
│  │       │                      │       │  │
│  │       └──────────────────────┘       │  │
│  └──────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
       │
       v
┌──────────────┐        ┌──────────────┐
│    Redis     │        │  PostgreSQL  │
│    Cache     │        │   Database   │
└──────────────┘        └──────────────┘
```

## Cache Categories

### 1. Session Cache
**Purpose**: Store user authentication sessions  
**TTL**: 7 days  
**Eviction**: Manual on logout

```redis
Key Pattern: session:{userId}
Value Type: Hash
Fields:
  - userId: "uuid-string"
  - email: "user@example.com"
  - roles: "USER,ADMIN"
  - lastAccess: "2026-08-02T10:30:00Z"
  - ipAddress: "192.168.1.1"
  - userAgent: "Mozilla/5.0..."
```

**Operations**:
```java
// Store session
public void storeSession(String userId, SessionData session) {
    String key = "session:" + userId;
    redisTemplate.opsForHash().putAll(key, sessionToMap(session));
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
}

// Get session
public SessionData getSession(String userId) {
    String key = "session:" + userId;
    Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
    return mapToSession(data);
}

// Delete session (logout)
public void deleteSession(String userId) {
    redisTemplate.delete("session:" + userId);
}
```

### 2. File Metadata Cache
**Purpose**: Cache frequently accessed file metadata  
**TTL**: 1 hour  
**Eviction**: LRU + TTL

```redis
Key Pattern: file_metadata:{fileId}
Value Type: String (JSON)
Value Example:
{
  "fileId": "uuid-string",
  "fileName": "document.pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "userId": "uuid-string",
  "folderId": "uuid-string",
  "s3Key": "users/.../file.pdf",
  "sha256Hash": "e3b0c442...",
  "uploadedAt": "2026-08-02T10:30:00Z",
  "downloadCount": 5
}
```

**Operations**:
```java
@Cacheable(value = "file_metadata", key = "#fileId")
public FileMetadata getFileMetadata(UUID fileId) {
    // If not in cache, this will execute and cache the result
    return fileRepository.findById(fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));
}

@CacheEvict(value = "file_metadata", key = "#fileId")
public void updateFileMetadata(UUID fileId, FileMetadata metadata) {
    fileRepository.save(metadata);
}

@CacheEvict(value = "file_metadata", key = "#fileId")
public void deleteFile(UUID fileId) {
    fileRepository.deleteById(fileId);
}
```

### 3. Folder Structure Cache
**Purpose**: Cache folder hierarchies and file listings  
**TTL**: 30 minutes  
**Eviction**: Manual on folder/file changes

```redis
Key Pattern: folder_tree:{userId}:{folderId}
Value Type: String (JSON)
Value Example:
{
  "folderId": "uuid-string",
  "folderName": "Projects",
  "path": "/Documents/Projects",
  "subfolders": [
    {"folderId": "uuid-1", "name": "2026"},
    {"folderId": "uuid-2", "name": "Archive"}
  ],
  "files": [
    {"fileId": "uuid-3", "name": "report.pdf", "size": 1048576}
  ],
  "totalFiles": 15,
  "totalSize": 52428800
}
```

**Operations**:
```java
public FolderTree getFolderContents(UUID userId, UUID folderId) {
    String key = String.format("folder_tree:%s:%s", userId, folderId);
    
    // Try cache first
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return objectMapper.readValue(cached, FolderTree.class);
    }
    
    // Query database
    FolderTree tree = buildFolderTree(userId, folderId);
    
    // Cache result
    redisTemplate.opsForValue().set(key, 
        objectMapper.writeValueAsString(tree), 
        30, TimeUnit.MINUTES);
    
    return tree;
}

public void invalidateFolderCache(UUID userId, UUID folderId) {
    // Invalidate current folder and parent folders up the tree
    String pattern = String.format("folder_tree:%s:*", userId);
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}
```

### 4. User Storage Stats Cache
**Purpose**: Track user's storage usage  
**TTL**: 5 minutes  
**Eviction**: Manual on file upload/delete

```redis
Key Pattern: user_storage:{userId}
Value Type: Hash
Fields:
  - quotaBytes: "5368709120"
  - usedBytes: "1234567890"
  - fileCount: "42"
  - lastUpdated: "2026-08-02T10:30:00Z"
```

**Operations**:
```java
public UserStorageStats getStorageStats(UUID userId) {
    String key = "user_storage:" + userId;
    Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
    
    if (cached.isEmpty()) {
        UserStorageStats stats = calculateStorageStats(userId);
        cacheStorageStats(userId, stats);
        return stats;
    }
    
    return mapToStorageStats(cached);
}

public void updateStorageUsed(UUID userId, long deltaBytes) {
    String key = "user_storage:" + userId;
    redisTemplate.opsForHash().increment(key, "usedBytes", deltaBytes);
    redisTemplate.opsForHash().increment(key, "fileCount", 1);
    redisTemplate.expire(key, 5, TimeUnit.MINUTES);
}
```

### 5. Search Results Cache
**Purpose**: Cache search query results  
**TTL**: 15 minutes  
**Eviction**: LRU + TTL

```redis
Key Pattern: search:{userId}:{queryHash}
Value Type: String (JSON array)
Value Example:
[
  {"fileId": "uuid-1", "fileName": "report.pdf", "score": 0.95},
  {"fileId": "uuid-2", "fileName": "invoice.pdf", "score": 0.87}
]
```

**Operations**:
```java
public List<FileSearchResult> searchFiles(UUID userId, String query) {
    String queryHash = DigestUtils.md5Hex(query.toLowerCase());
    String key = String.format("search:%s:%s", userId, queryHash);
    
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return objectMapper.readValue(cached, 
            new TypeReference<List<FileSearchResult>>() {});
    }
    
    List<FileSearchResult> results = performSearch(userId, query);
    
    redisTemplate.opsForValue().set(key, 
        objectMapper.writeValueAsString(results),
        15, TimeUnit.MINUTES);
    
    return results;
}
```

### 6. Rate Limiting Cache
**Purpose**: Implement API rate limiting  
**TTL**: 1 minute (sliding window)  
**Eviction**: TTL

```redis
Key Pattern: rate_limit:{userId}:{endpoint}:{timestamp}
Value Type: String (counter)
```

**Operations**:
```java
public boolean checkRateLimit(UUID userId, String endpoint) {
    long currentMinute = System.currentTimeMillis() / 60000;
    String key = String.format("rate_limit:%s:%s:%d", 
        userId, endpoint, currentMinute);
    
    Long count = redisTemplate.opsForValue().increment(key);
    
    if (count == 1) {
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
    }
    
    // Max 100 requests per minute
    return count <= 100;
}
```

### 7. Presigned URL Cache
**Purpose**: Cache S3 presigned URLs  
**TTL**: 5 minutes (URL valid for 10 minutes)  
**Eviction**: TTL

```redis
Key Pattern: presigned_url:{fileId}
Value Type: String
Value: "https://s3.amazonaws.com/bucket/key?signature=..."
```

**Operations**:
```java
public String getPresignedUrl(UUID fileId) {
    String key = "presigned_url:" + fileId;
    String cached = redisTemplate.opsForValue().get(key);
    
    if (cached != null) {
        return cached;
    }
    
    String url = s3Service.generatePresignedUrl(fileId, 10, TimeUnit.MINUTES);
    redisTemplate.opsForValue().set(key, url, 5, TimeUnit.MINUTES);
    
    return url;
}
```

### 8. Upload Session Cache
**Purpose**: Store multipart upload session data (see [S3 Multipart Upload](06-s3-multipart-upload.md))  
**TTL**: 24 hours  
**Eviction**: Manual on complete/abort

```redis
Key Pattern: upload_session:{uploadId}
Value Type: String (JSON)
Value Example:
{
  "uploadId": "session-uuid",
  "s3UploadId": "AWS-multipart-upload-id",
  "userId": "user-uuid",
  "fileId": "file-uuid",
  "fileName": "large-video.mp4",
  "fileSize": 524288000,
  "partSize": 10485760,
  "totalParts": 50,
  "uploadedParts": {
    "1": "etag-1",
    "2": "etag-2",
    "3": "etag-3"
  },
  "createdAt": "2026-08-02T10:00:00Z",
  "expiresAt": "2026-08-03T10:00:00Z"
}
```

**Operations**:
```java
public void storeUploadSession(UploadSession session) {
    String key = "upload_session:" + session.getUploadId();
    redisTemplate.opsForValue().set(key, 
        objectMapper.writeValueAsString(session),
        24, TimeUnit.HOURS);
}

public UploadSession getUploadSession(String uploadId) {
    String key = "upload_session:" + uploadId;
    String cached = redisTemplate.opsForValue().get(key);
    
    if (cached == null) {
        throw new UploadSessionNotFoundException(uploadId);
    }
    
    return objectMapper.readValue(cached, UploadSession.class);
}

public void deleteUploadSession(String uploadId) {
    redisTemplate.delete("upload_session:" + uploadId);
}
```

## Cache Invalidation Strategy

### Write-Through Cache
For critical data that must be consistent:

```java
@Transactional
public void updateFileMetadata(UUID fileId, FileMetadata metadata) {
    // 1. Update database
    fileRepository.save(metadata);
    
    // 2. Update cache
    String key = "file_metadata:" + fileId;
    redisTemplate.opsForValue().set(key, 
        objectMapper.writeValueAsString(metadata),
        1, TimeUnit.HOURS);
}
```

### Cache-Aside with Invalidation
For frequently changing data:

```java
@CacheEvict(value = "folder_tree", allEntries = true)
public void uploadFile(FileUploadRequest request) {
    // Upload file logic
    // Cache automatically invalidated by annotation
}
```

### Lazy Invalidation
For eventually consistent data:

```java
@Async
public void invalidateCacheAsync(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}
```

## Cache Configuration

### Redis Configuration
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 1000ms
      shutdown-timeout: 100ms
```

### Cache Manager Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        
        // File metadata: 1 hour TTL
        cacheConfigs.put("file_metadata", 
            createCacheConfig(Duration.ofHours(1)));
        
        // Folder tree: 30 minutes TTL
        cacheConfigs.put("folder_tree", 
            createCacheConfig(Duration.ofMinutes(30)));
        
        // User storage: 5 minutes TTL
        cacheConfigs.put("user_storage", 
            createCacheConfig(Duration.ofMinutes(5)));
        
        // Search results: 15 minutes TTL
        cacheConfigs.put("search", 
            createCacheConfig(Duration.ofMinutes(15)));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(createCacheConfig(Duration.ofMinutes(10)))
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
    
    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

## Monitoring and Metrics

### Cache Hit Ratio
```java
@Aspect
@Component
public class CacheMetricsAspect {
    
    private final MeterRegistry meterRegistry;
    
    @Around("@annotation(cacheable)")
    public Object measureCacheHit(ProceedingJoinPoint pjp, Cacheable cacheable) {
        String cacheName = cacheable.value()[0];
        
        try {
            Object result = pjp.proceed();
            
            // Cache hit
            meterRegistry.counter("cache.hit", "cache", cacheName).increment();
            
            return result;
        } catch (Throwable e) {
            // Cache miss
            meterRegistry.counter("cache.miss", "cache", cacheName).increment();
            throw new RuntimeException(e);
        }
    }
}
```

### Redis Health Check
```java
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("redis", "Available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "Unavailable")
                .withException(e)
                .build();
        }
        
        return Health.down().build();
    }
}
```

## Performance Optimization

### 1. Pipeline Operations
Batch multiple Redis operations:

```java
public void cacheMultipleFiles(List<FileMetadata> files) {
    redisTemplate.executePipelined(new SessionCallback<Object>() {
        @Override
        public Object execute(RedisOperations operations) {
            files.forEach(file -> {
                String key = "file_metadata:" + file.getId();
                operations.opsForValue().set(key, file, 1, TimeUnit.HOURS);
            });
            return null;
        }
    });
}
```

### 2. Compression
Compress large values:

```java
public void cacheLargeData(String key, Object data) {
    byte[] serialized = objectMapper.writeValueAsBytes(data);
    byte[] compressed = compress(serialized);
    redisTemplate.opsForValue().set(key.getBytes(), compressed);
}
```

### 3. Lazy Loading
Load cache on first access:

```java
@PostConstruct
public void warmupCache() {
    // Preload frequently accessed data
    List<FileMetadata> popular = fileRepository.findMostDownloaded(100);
    popular.forEach(file -> cacheFileMetadata(file));
}
```

## Error Handling

```java
public FileMetadata getFileWithFallback(UUID fileId) {
    try {
        // Try cache first
        return getCachedFileMetadata(fileId);
    } catch (RedisConnectionException e) {
        log.warn("Redis unavailable, falling back to database", e);
        // Fallback to database
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
    }
}
```

## Cache Statistics

```redis
# Monitor cache statistics
INFO stats

# Monitor memory usage
INFO memory

# Monitor key counts
DBSIZE

# Monitor slow queries
SLOWLOG GET 10
```

---

**Version**: 1.0  
**Last Updated**: 2026-08-02  
**Author**: Ziboto Team
