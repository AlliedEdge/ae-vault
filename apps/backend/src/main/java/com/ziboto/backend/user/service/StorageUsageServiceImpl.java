package com.ziboto.backend.user.service;

import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.storage.repository.BucketRepository;
import com.ziboto.backend.user.dto.StorageUsageResponse;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of StorageUsageService.
 * 
 * <p>Calculates storage usage statistics from database with Redis caching
 * for performance optimization. All calculations are done in the database
 * using optimized queries to avoid loading entities into memory.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageUsageServiceImpl implements StorageUsageService {
    
    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final BucketRepository bucketRepository;
    private final StorageUsageCacheService cacheService;
    
    @Override
    @Transactional(readOnly = true)
    public StorageUsageResponse calculateStorageUsage(Long userId) {
        log.debug("Calculating storage usage for user: {}", userId);
        
        // Try to get from cache first
        return cacheService.getStorageUsage(userId)
                .orElseGet(() -> {
                    log.debug("Cache miss, calculating from database for user: {}", userId);
                    StorageUsageResponse storageUsage = calculateStorageUsageFromDatabase(userId);
                    
                    // Cache the result
                    cacheService.cacheStorageUsage(userId, storageUsage);
                    
                    return storageUsage;
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public StorageUsageResponse calculateStorageUsageFromDatabase(Long userId) {
        log.debug("Calculating storage usage from database for user: {}", userId);
        
        // Get user's storage quota from User entity
        Long storageQuota = userRepository.findById(userId)
                .map(user -> user.getStorageQuota())
                .orElse(0L);
        
        // Calculate total storage used from FileMetadata
        // This uses an optimized SUM query without loading entities
        Long usedStorage = fileMetadataRepository.calculateTotalStorageByUploaderId(userId);
        
        // Count total files
        // This uses an optimized COUNT query without loading entities
        Long totalFiles = fileMetadataRepository.countByUploaderId(userId);
        
        // Count total folders (buckets)
        // This uses an optimized COUNT query without loading entities
        Long totalFolders = bucketRepository.countByOwnerId(userId);
        
        log.debug("Storage usage calculated: userId={}, quota={}, used={}, files={}, folders={}", 
                userId, storageQuota, usedStorage, totalFiles, totalFolders);
        
        return StorageUsageResponse.of(storageQuota, usedStorage, totalFiles, totalFolders);
    }
    
    @Override
    public void invalidateCache(Long userId) {
        log.debug("Invalidating storage usage cache for user: {}", userId);
        cacheService.evictStorageUsage(userId);
    }
}
