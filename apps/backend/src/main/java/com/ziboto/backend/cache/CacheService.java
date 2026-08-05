package com.ziboto.backend.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * General-purpose Redis cache service.
 * 
 * <p>Provides basic cache operations with TTL support:</p>
 * <ul>
 *   <li>Set/Get operations with automatic serialization</li>
 *   <li>TTL management</li>
 *   <li>Pattern-based deletion</li>
 *   <li>Key existence checks</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Store value in cache without expiration.
     * 
     * @param key cache key
     * @param value value to store
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Cached value for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to cache value for key: {}", key, e);
        }
    }
    
    /**
     * Store value in cache with TTL expiration.
     * 
     * @param key cache key
     * @param value value to store
     * @param ttl time-to-live duration
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached value for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Failed to cache value for key: {}", key, e);
        }
    }
    
    /**
     * Retrieve value from cache.
     * 
     * @param key cache key
     * @return cached value or null if not found
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Retrieved value for key: {} - found: {}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Failed to get value for key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Retrieve typed value from cache.
     * 
     * @param key cache key
     * @param type expected value type
     * @return cached value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get typed value for key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Delete value from cache.
     * 
     * @param key cache key
     * @return true if key was deleted
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Deleted key: {} - success: {}", key, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("Failed to delete key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Delete all keys matching pattern.
     * Use with caution - can be expensive for large datasets.
     * 
     * @param pattern key pattern (e.g., "user:*")
     * @return number of deleted keys
     */
    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.debug("Deleted {} keys matching pattern: {}", deleted, pattern);
                return deleted != null ? deleted : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to delete keys by pattern: {}", pattern, e);
            return 0;
        }
    }
    
    /**
     * Check if key exists in cache.
     * 
     * @param key cache key
     * @return true if key exists
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check existence for key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Set expiration time for existing key.
     * 
     * @param key cache key
     * @param ttl time-to-live duration
     * @return true if expiration was set
     */
    public boolean expire(String key, Duration ttl) {
        try {
            Boolean expired = redisTemplate.expire(key, ttl);
            log.debug("Set expiration for key: {} - TTL: {} - success: {}", key, ttl, expired);
            return Boolean.TRUE.equals(expired);
        } catch (Exception e) {
            log.error("Failed to set expiration for key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get remaining TTL for key.
     * 
     * @param key cache key
     * @return remaining TTL in seconds, or -1 if no expiration, -2 if key doesn't exist
     */
    public long getTimeToLive(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -2;
        } catch (Exception e) {
            log.error("Failed to get TTL for key: {}", key, e);
            return -2;
        }
    }
    
    /**
     * Increment numeric value in cache.
     * 
     * @param key cache key
     * @return new value after increment
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Increment numeric value by delta.
     * 
     * @param key cache key
     * @param delta increment amount
     * @return new value after increment
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Failed to increment key: {} by {}", key, delta, e);
            return null;
        }
    }
    
    /**
     * Decrement numeric value in cache.
     * 
     * @param key cache key
     * @return new value after decrement
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Failed to decrement key: {}", key, e);
            return null;
        }
    }
}
