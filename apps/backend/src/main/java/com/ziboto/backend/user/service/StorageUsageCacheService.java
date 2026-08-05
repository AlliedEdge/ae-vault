package com.ziboto.backend.user.service;

import com.ziboto.backend.user.dto.StorageUsageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache service for storage usage statistics.
 * 
 * <p>Uses Redis to cache storage statistics to reduce database queries.
 * Storage data is cached with a TTL to ensure eventual consistency.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageUsageCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Cache key prefix for storage usage data.
     */
    private static final String CACHE_KEY_PREFIX = "storage:usage:user:";
    
    /**
     * Cache TTL in minutes.
     * Storage data changes frequently, so we use a shorter TTL.
     */
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * Get storage usage from cache.
     * 
     * @param userId user ID
     * @return Optional containing cached storage usage, or empty if not cached
     */
    public Optional<StorageUsageResponse> getStorageUsage(Long userId) {
        try {
            String key = buildCacheKey(userId);
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached instanceof StorageUsageResponse) {
                log.debug("Cache hit for storage usage: userId={}", userId);
                return Optional.of((StorageUsageResponse) cached);
            }
            
            log.debug("Cache miss for storage usage: userId={}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting storage usage from cache: userId={}", userId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Cache storage usage data.
     * 
     * @param userId user ID
     * @param storageUsage storage usage data to cache
     */
    public void cacheStorageUsage(Long userId, StorageUsageResponse storageUsage) {
        try {
            String key = buildCacheKey(userId);
            redisTemplate.opsForValue().set(key, storageUsage, CACHE_TTL);
            log.debug("Cached storage usage: userId={}, ttl={}min", userId, CACHE_TTL.toMinutes());
        } catch (Exception e) {
            log.error("Error caching storage usage: userId={}", userId, e);
            // Don't throw exception - cache failures should not break the application
        }
    }
    
    /**
     * Invalidate (evict) storage usage cache for a user.
     * This should be called when storage data changes (file upload/delete).
     * 
     * @param userId user ID
     */
    public void evictStorageUsage(Long userId) {
        try {
            String key = buildCacheKey(userId);
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Evicted storage usage cache: userId={}, deleted={}", userId, deleted);
        } catch (Exception e) {
            log.error("Error evicting storage usage cache: userId={}", userId, e);
        }
    }
    
    /**
     * Invalidate storage usage cache for multiple users.
     * Useful for bulk operations.
     * 
     * @param userIds user IDs
     */
    public void evictStorageUsageMultiple(Iterable<Long> userIds) {
        try {
            userIds.forEach(this::evictStorageUsage);
        } catch (Exception e) {
            log.error("Error evicting multiple storage usage caches", e);
        }
    }
    
    /**
     * Build cache key for storage usage.
     * 
     * @param userId user ID
     * @return cache key
     */
    private String buildCacheKey(Long userId) {
        return CACHE_KEY_PREFIX + userId;
    }
}
