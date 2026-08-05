# Storage Usage API Documentation

## Overview
Comprehensive storage usage API with database-driven calculations, Redis caching, and query optimization. All values are calculated from the database in real-time and cached for performance.

## Key Features

✅ **Database-Driven** - All values calculated from database, no hardcoded data
✅ **Optimized Queries** - Uses COUNT and SUM without loading entities
✅ **Redis Caching** - 5-minute TTL for performance
✅ **Cache-Aside Pattern** - Automatic cache fallback to database
✅ **Comprehensive Metrics** - Storage, files, and folders

## API Endpoint

### GET /api/v1/users/storage

Get comprehensive storage usage statistics for the authenticated user.

**Authorization:** Authenticated user (JWT token required)

**Security:** User identity extracted from SecurityContext

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Storage info retrieved successfully",
  "data": {
    "totalStorage": 5368709120,
    "usedStorage": 1073741824,
    "availableStorage": 4294967296,
    "usedPercentage": 20.0,
    "totalFiles": 42,
    "totalFolders": 5
  },
  "timestamp": "2026-08-05T17:56:00"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `totalStorage` | Long | Total storage quota in bytes |
| `usedStorage` | Long | Storage currently used in bytes |
| `availableStorage` | Long | Available storage (totalStorage - usedStorage) |
| `usedPercentage` | Double | Percentage of storage used (0.00 - 100.00) |
| `totalFiles` | Long | Total number of files owned by user |
| `totalFolders` | Long | Total number of folders/buckets owned by user |

## Data Sources

### Total Storage (Quota)
- **Source:** `users.storage_quota` column
- **Query:** Direct field access via User entity
- **Description:** Admin-configured storage quota for the user

### Used Storage
- **Source:** `file_metadata.file_size` column
- **Query:** `SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.uploader.id = :userId`
- **Description:** Sum of all file sizes uploaded by the user
- **Optimization:** Uses `COALESCE` to return 0 instead of NULL

### Available Storage
- **Calculation:** `totalStorage - usedStorage`
- **Description:** Remaining storage available to the user

### Used Percentage
- **Calculation:** `(usedStorage * 100.0) / totalStorage`
- **Description:** Percentage of quota used, rounded to 2 decimal places
- **Range:** 0.00 to 100.00

### Total Files
- **Source:** `file_metadata` table
- **Query:** `SELECT COUNT(f) FROM FileMetadata f WHERE f.uploader.id = :userId`
- **Description:** Count of all files uploaded by the user
- **Optimization:** Uses COUNT without loading entities

### Total Folders
- **Source:** `buckets` table
- **Query:** `SELECT COUNT(b) FROM Bucket b WHERE b.owner.id = :userId`
- **Description:** Count of all buckets/folders owned by the user
- **Optimization:** Uses COUNT without loading entities

## Database Queries

### Optimized Query Strategy

All queries are optimized to avoid loading entities into memory:

```java
// ❌ BAD - Loads all files into memory
List<FileMetadata> files = fileRepository.findByUploaderId(userId);
long totalSize = files.stream().mapToLong(FileMetadata::getFileSize).sum();

// ✅ GOOD - Calculates in database
@Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.uploader.id = :userId")
Long calculateTotalStorageByUploaderId(@Param("uploaderId") Long uploaderId);
```

### Query Performance

**FileMetadata Queries:**
```sql
-- Count files
SELECT COUNT(f.id) FROM file_metadata f WHERE f.user_id = ?

-- Calculate storage
SELECT COALESCE(SUM(f.file_size), 0) FROM file_metadata f WHERE f.user_id = ?
```

**Bucket Queries:**
```sql
-- Count folders
SELECT COUNT(b.id) FROM buckets b WHERE b.user_id = ?
```

**Indexes Required:**
- `file_metadata.user_id` (for file queries)
- `buckets.user_id` (for folder queries)

## Redis Caching

### Cache Strategy

**Pattern:** Cache-Aside (Lazy Loading)

1. Check cache first
2. If cache miss, query database
3. Cache the result with TTL
4. Return data to client

### Cache Configuration

**Cache Key:** `storage:usage:user:{userId}`

**TTL:** 5 minutes (300 seconds)

**Serialization:** Java Serialization (StorageUsageResponse implements Serializable)

**Storage:** Redis String value

### Cache Operations

**Get from Cache:**
```java
Optional<StorageUsageResponse> cached = cacheService.getStorageUsage(userId);
```

**Cache Data:**
```java
cacheService.cacheStorageUsage(userId, storageUsage);
```

**Invalidate Cache:**
```java
cacheService.evictStorageUsage(userId);
```

### Cache Invalidation

Cache should be invalidated when storage data changes:

**When to Invalidate:**
- ✅ After file upload
- ✅ After file deletion
- ✅ After folder creation
- ✅ After folder deletion
- ✅ After admin updates storage quota

**Example Integration:**
```java
@Service
public class FileService {
    private final StorageUsageService storageUsageService;
    
    public void uploadFile(MultipartFile file, Long userId) {
        // ... upload logic ...
        
        // Invalidate cache after upload
        storageUsageService.invalidateCache(userId);
    }
    
    public void deleteFile(Long fileId, Long userId) {
        // ... delete logic ...
        
        // Invalidate cache after deletion
        storageUsageService.invalidateCache(userId);
    }
}
```

### Cache Error Handling

Cache failures **do not** break the application:

```java
try {
    // Try to cache
    redisTemplate.opsForValue().set(key, value, ttl);
} catch (Exception e) {
    log.error("Error caching data", e);
    // Don't throw - continue without caching
}
```

## Performance Considerations

### Query Optimization

**1. Use Aggregate Functions**
- `COUNT(*)` instead of loading and counting in Java
- `SUM(column)` instead of loading and summing in Java
- `COALESCE(SUM(column), 0)` to handle NULL cases

**2. Avoid Loading Entities**
```java
// ❌ Loads entities (slow for large datasets)
List<FileMetadata> files = repository.findAll();

// ✅ Aggregates in database (fast)
Long count = repository.countFiles();
```

**3. Use Proper Indexes**
- Index on `file_metadata.user_id`
- Index on `buckets.user_id`

### Caching Strategy

**Cache Hit Rate Metrics:**
- Expected: 70-90% cache hit rate
- Cache miss triggers database query
- Cache failures fallback to database

**TTL Selection:**
- **5 minutes** balances:
  - Freshness (data not too stale)
  - Performance (reduces DB load)
  - Memory (doesn't consume too much Redis memory)

**Cache Size Estimation:**
- StorageUsageResponse: ~150 bytes per user
- 10,000 active users: ~1.5 MB in Redis
- Minimal memory footprint

## Integration Guide

### Using the API

**Frontend Example (JavaScript):**
```javascript
async function getStorageInfo() {
  const response = await fetch('/api/v1/users/storage', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  const result = await response.json();
  
  if (result.success) {
    const storage = result.data;
    console.log(`Used: ${storage.usedStorage} bytes`);
    console.log(`Total: ${storage.totalStorage} bytes`);
    console.log(`Usage: ${storage.usedPercentage}%`);
    console.log(`Files: ${storage.totalFiles}`);
    console.log(`Folders: ${storage.totalFolders}`);
  }
}
```

**Display Storage Bar:**
```javascript
function renderStorageBar(storage) {
  const percentage = storage.usedPercentage;
  const usedGB = (storage.usedStorage / 1024 / 1024 / 1024).toFixed(2);
  const totalGB = (storage.totalStorage / 1024 / 1024 / 1024).toFixed(2);
  
  return `
    <div class="storage-bar">
      <div class="progress" style="width: ${percentage}%"></div>
      <span>${usedGB} GB / ${totalGB} GB (${percentage}%)</span>
    </div>
    <div class="storage-stats">
      <span>${storage.totalFiles} files</span>
      <span>${storage.totalFolders} folders</span>
    </div>
  `;
}
```

### Backend Integration

**File Upload Service:**
```java
@Service
public class FileUploadService {
    private final FileMetadataRepository fileRepository;
    private final StorageUsageService storageUsageService;
    
    @Transactional
    public void uploadFile(MultipartFile file, Long userId) {
        // Check if user has enough storage
        StorageUsageResponse storage = storageUsageService
            .calculateStorageUsageFromDatabase(userId);
        
        long fileSize = file.getSize();
        if (storage.getAvailableStorage() < fileSize) {
            throw new InsufficientStorageException(
                "Not enough storage. Need " + fileSize + 
                " bytes, available " + storage.getAvailableStorage()
            );
        }
        
        // Upload file
        FileMetadata metadata = uploadToStorage(file);
        fileRepository.save(metadata);
        
        // Invalidate cache
        storageUsageService.invalidateCache(userId);
    }
}
```

**File Deletion Service:**
```java
@Service
public class FileDeletionService {
    private final FileMetadataRepository fileRepository;
    private final StorageUsageService storageUsageService;
    
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        // Delete file
        FileMetadata file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException());
        
        deleteFromStorage(file.getStorageKey());
        fileRepository.delete(file);
        
        // Invalidate cache
        storageUsageService.invalidateCache(userId);
    }
}
```

## Error Handling

### 401 Unauthorized
```json
{
  "success": false,
  "message": "User is not authenticated",
  "timestamp": "2026-08-05T17:56:00"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "User not found with username: john_doe",
  "timestamp": "2026-08-05T17:56:00"
}
```

### Database Error
If database query fails, the error is logged and a 500 error is returned:
```json
{
  "success": false,
  "message": "Error calculating storage usage",
  "timestamp": "2026-08-05T17:56:00"
}
```

### Cache Error
Cache errors are logged but **do not** fail the request. The system automatically falls back to database queries.

## Monitoring

### Key Metrics to Monitor

**1. Cache Hit Rate**
```
cache_hits / (cache_hits + cache_misses)
Target: > 80%
```

**2. Database Query Time**
```
AVG(query_execution_time)
Target: < 100ms
```

**3. API Response Time**
```
AVG(api_response_time)
Target: < 200ms (cache hit), < 500ms (cache miss)
```

**4. Storage Calculation Frequency**
```
requests_per_minute
Monitor for unusual spikes
```

### Logging

**Debug Logs:**
```
Cache hit: userId=123
Cache miss: userId=123
Calculating from database: userId=123
```

**Info Logs:**
```
Storage info retrieved: userId=123, files=42, folders=5, used=1073741824 bytes
```

**Error Logs:**
```
Error caching storage usage: userId=123
Error calculating storage: userId=123
```

## Testing

### cURL Example

```bash
curl -X GET "http://localhost:8080/api/v1/users/storage" \
  -H "Authorization: Bearer <access_token>"
```

### Expected Response

```json
{
  "success": true,
  "message": "Storage info retrieved successfully",
  "data": {
    "totalStorage": 5368709120,
    "usedStorage": 1073741824,
    "availableStorage": 4294967296,
    "usedPercentage": 20.0,
    "totalFiles": 42,
    "totalFolders": 5
  },
  "timestamp": "2026-08-05T17:56:00"
}
```

### Test Scenarios

**1. New User (No Files)**
```json
{
  "totalStorage": 5368709120,
  "usedStorage": 0,
  "availableStorage": 5368709120,
  "usedPercentage": 0.0,
  "totalFiles": 0,
  "totalFolders": 0
}
```

**2. User at 50% Capacity**
```json
{
  "totalStorage": 1073741824,
  "usedStorage": 536870912,
  "availableStorage": 536870912,
  "usedPercentage": 50.0,
  "totalFiles": 25,
  "totalFolders": 3
}
```

**3. User Near Quota**
```json
{
  "totalStorage": 1073741824,
  "usedStorage": 1069547520,
  "availableStorage": 4194304,
  "usedPercentage": 99.61,
  "totalFiles": 100,
  "totalFolders": 10
}
```

## Database Schema

### Required Tables

**users:**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    storage_quota BIGINT, -- in bytes
    -- ... other fields
);
```

**file_metadata:**
```sql
CREATE TABLE file_metadata (
    id BIGINT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL, -- in bytes
    user_id BIGINT NOT NULL,
    -- ... other fields
    
    CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_file_user (user_id)
);
```

**buckets:**
```sql
CREATE TABLE buckets (
    id BIGINT PRIMARY KEY,
    name VARCHAR(63) NOT NULL,
    user_id BIGINT NOT NULL,
    -- ... other fields
    
    CONSTRAINT fk_bucket_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_bucket_user (user_id)
);
```

## Future Enhancements

1. **Storage by File Type** - Breakdown of storage used by file type
2. **Storage Trends** - Historical storage usage over time
3. **Storage Warnings** - Alerts when approaching quota (90%, 95%, 99%)
4. **Storage Analytics** - Most common file types, largest files
5. **Folder Statistics** - Storage used per folder
6. **Shared Storage** - Track shared vs personal storage
7. **Storage Forecasting** - Predict when storage will be full
8. **Compression Suggestions** - Identify files that could be compressed

## Summary

✅ **Database-Driven** - All calculations from database
✅ **No Hardcoding** - Dynamic values based on actual data
✅ **Optimized** - Aggregate queries, no entity loading
✅ **Cached** - Redis with 5-minute TTL
✅ **Reliable** - Cache failures don't break functionality
✅ **Comprehensive** - Storage, files, folders metrics
✅ **Performant** - Sub-second response times
✅ **Maintainable** - Clean separation of concerns
