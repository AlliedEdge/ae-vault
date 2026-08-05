package com.ziboto.backend.user.service;

import com.ziboto.backend.user.dto.StorageUsageResponse;

/**
 * Service interface for storage usage operations.
 */
public interface StorageUsageService {
    
    /**
     * Calculate storage usage for a user.
     * Uses cached data if available, otherwise queries database and caches result.
     * 
     * @param userId user ID
     * @return storage usage statistics
     */
    StorageUsageResponse calculateStorageUsage(Long userId);
    
    /**
     * Calculate storage usage from database without using cache.
     * Used to get real-time data and refresh cache.
     * 
     * @param userId user ID
     * @return storage usage statistics
     */
    StorageUsageResponse calculateStorageUsageFromDatabase(Long userId);
    
    /**
     * Invalidate storage usage cache for a user.
     * Should be called after file upload/delete operations.
     * 
     * @param userId user ID
     */
    void invalidateCache(Long userId);
}
