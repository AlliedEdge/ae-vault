package com.ziboto.backend.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Redis service providing all common Redis operations.
 * 
 * <p>This service acts as a centralized facade for all Redis operations
 * used throughout the application. It provides:</p>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Basic Operations:</b> set, get, delete with TTL support</li>
 *   <li><b>Counter Operations:</b> increment, decrement with TTL</li>
 *   <li><b>Hash Operations:</b> store and retrieve hash maps</li>
 *   <li><b>Set Operations:</b> manage sets of values</li>
 *   <li><b>TTL Management:</b> set, get, and check expiration</li>
 *   <li><b>Pattern Operations:</b> delete by pattern (use with caution)</li>
 *   <li><b>Existence Checks:</b> verify key existence</li>
 * </ul>
 * 
 * <h2>Error Handling:</h2>
 * All methods fail gracefully - Redis errors are logged but don't throw exceptions.
 * This ensures Redis failures don't break the application (fail-open strategy).
 * 
 * <h2>Usage Examples:</h2>
 * <pre>
 * // Simple caching
 * redisService.set("user:123", userData, Duration.ofHours(1));
 * UserData cached = redisService.get("user:123", UserData.class);
 * 
 * // Rate limiting
 * Long count = redisService.incrementWithTTL("rate:login:user123", Duration.ofMinutes(15));
 * boolean limited = count != null && count > 5;
 * 
 * // Session tracking
 * redisService.hashSet("session:abc123", "ip", "192.168.1.1");
 * redisService.hashSet("session:abc123", "device", "Chrome");
 * Map&lt;String, Object&gt; session = redisService.hashGetAll("session:abc123");
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ============================================================
    // Basic Key-Value Operations
    // ============================================================
    
    /**
     * Store value without expiration.
     * 
     * @param key cache key
     * @param value value to store
     * @return true if successful
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Set key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to set key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Store value with TTL expiration.
     * 
     * @param key cache key
     * @param value value to store
     * @param ttl time-to-live duration
     * @return true if successful
     */
    public boolean set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Set key: {} with TTL: {}", key, ttl);
            return true;
        } catch (Exception e) {
            log.error("Failed to set key with TTL: {}", key, e);
            return false;
        }
    }
    
    /**
     * Store value only if key doesn't exist (SET NX).
     * 
     * @param key cache key
     * @param value value to store
     * @param ttl time-to-live duration
     * @return true if value was set, false if key already exists
     */
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            log.debug("SetIfAbsent key: {} - result: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to setIfAbsent key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Retrieve value from cache.
     * 
     * @param key cache key
     * @return cached value or null
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Get key: {} - found: {}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Failed to get key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Retrieve typed value from cache.
     * 
     * @param key cache key
     * @param type expected value type
     * @param <T> value type
     * @return cached value or null
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
            log.error("Failed to get typed key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Delete key from cache.
     * 
     * @param key cache key
     * @return true if key was deleted
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Delete key: {} - success: {}", key, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("Failed to delete key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Delete multiple keys.
     * 
     * @param keys collection of keys to delete
     * @return number of keys deleted
     */
    public long delete(Collection<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            Long deleted = redisTemplate.delete(keys);
            log.debug("Deleted {} keys", deleted);
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            log.error("Failed to delete multiple keys", e);
            return 0;
        }
    }
    
    /**
     * Delete all keys matching pattern.
     * <b>WARNING:</b> Use with caution - can be expensive for large datasets.
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
            log.error("Failed to delete by pattern: {}", pattern, e);
            return 0;
        }
    }
    
    /**
     * Check if key exists.
     * 
     * @param key cache key
     * @return true if key exists
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check existence of key: {}", key, e);
            return false;
        }
    }
    
    // ============================================================
    // TTL Operations
    // ============================================================
    
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
     * @return remaining TTL in seconds, -1 if no expiration, -2 if key doesn't exist
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
     * Remove expiration from key (make it persistent).
     * 
     * @param key cache key
     * @return true if successful
     */
    public boolean persist(String key) {
        try {
            Boolean result = redisTemplate.persist(key);
            log.debug("Persist key: {} - success: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to persist key: {}", key, e);
            return false;
        }
    }
    
    // ============================================================
    // Counter Operations
    // ============================================================
    
    /**
     * Increment numeric value.
     * 
     * @param key cache key
     * @return new value after increment, or null on error
     */
    public Long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.debug("Incremented key: {} - new value: {}", key, value);
            return value;
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
     * @return new value after increment, or null on error
     */
    public Long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Incremented key: {} by {} - new value: {}", key, delta, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to increment key: {} by {}", key, delta, e);
            return null;
        }
    }
    
    /**
     * Increment value and set TTL if it's the first increment.
     * Useful for rate limiting counters.
     * 
     * @param key cache key
     * @param ttl time-to-live duration (applied only on first increment)
     * @return new value after increment, or null on error
     */
    public Long incrementWithTTL(String key, Duration ttl) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // First increment - set TTL
                redisTemplate.expire(key, ttl);
            }
            log.debug("Incremented key: {} with TTL: {} - new value: {}", key, ttl, count);
            return count;
        } catch (Exception e) {
            log.error("Failed to increment key with TTL: {}", key, e);
            return null;
        }
    }
    
    /**
     * Decrement numeric value.
     * 
     * @param key cache key
     * @return new value after decrement, or null on error
     */
    public Long decrement(String key) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key);
            log.debug("Decremented key: {} - new value: {}", key, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to decrement key: {}", key, e);
            return null;
        }
    }
    
    /**
     * Decrement numeric value by delta.
     * 
     * @param key cache key
     * @param delta decrement amount
     * @return new value after decrement, or null on error
     */
    public Long decrement(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key, delta);
            log.debug("Decremented key: {} by {} - new value: {}", key, delta, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to decrement key: {} by {}", key, delta, e);
            return null;
        }
    }
    
    // ============================================================
    // Hash Operations
    // ============================================================
    
    /**
     * Set field in hash.
     * 
     * @param key hash key
     * @param field field name
     * @param value field value
     * @return true if successful
     */
    public boolean hashSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("HashSet key: {} - field: {}", key, field);
            return true;
        } catch (Exception e) {
            log.error("Failed to hashSet key: {} field: {}", key, field, e);
            return false;
        }
    }
    
    /**
     * Set multiple fields in hash.
     * 
     * @param key hash key
     * @param values map of field-value pairs
     * @return true if successful
     */
    public boolean hashSetAll(String key, Map<String, Object> values) {
        try {
            redisTemplate.opsForHash().putAll(key, values);
            log.debug("HashSetAll key: {} - fields: {}", key, values.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to hashSetAll key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get field value from hash.
     * 
     * @param key hash key
     * @param field field name
     * @return field value or null
     */
    public Object hashGet(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            log.debug("HashGet key: {} field: {} - found: {}", key, field, value != null);
            return value;
        } catch (Exception e) {
            log.error("Failed to hashGet key: {} field: {}", key, field, e);
            return null;
        }
    }
    
    /**
     * Get all fields and values from hash.
     * 
     * @param key hash key
     * @return map of field-value pairs
     */
    public Map<String, Object> hashGetAll(String key) {
        try {
            Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(key);
            Map<String, Object> result = new HashMap<>();
            rawMap.forEach((k, v) -> result.put(k.toString(), v));
            log.debug("HashGetAll key: {} - fields: {}", key, result.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to hashGetAll key: {}", key, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Delete field from hash.
     * 
     * @param key hash key
     * @param fields field names to delete
     * @return number of fields deleted
     */
    public long hashDelete(String key, Object... fields) {
        try {
            Long deleted = redisTemplate.opsForHash().delete(key, fields);
            log.debug("HashDelete key: {} - deleted: {}", key, deleted);
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            log.error("Failed to hashDelete key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * Check if hash field exists.
     * 
     * @param key hash key
     * @param field field name
     * @return true if field exists
     */
    public boolean hashExists(String key, String field) {
        try {
            Boolean exists = redisTemplate.opsForHash().hasKey(key, field);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check hash field existence: {} - {}", key, field, e);
            return false;
        }
    }
    
    /**
     * Get hash size (number of fields).
     * 
     * @param key hash key
     * @return number of fields in hash
     */
    public long hashSize(String key) {
        try {
            Long size = redisTemplate.opsForHash().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get hash size for key: {}", key, e);
            return 0;
        }
    }
    
    // ============================================================
    // Set Operations
    // ============================================================
    
    /**
     * Add member(s) to set.
     * 
     * @param key set key
     * @param values values to add
     * @return number of elements added
     */
    public long setAdd(String key, Object... values) {
        try {
            Long added = redisTemplate.opsForSet().add(key, values);
            log.debug("SetAdd key: {} - added: {}", key, added);
            return added != null ? added : 0;
        } catch (Exception e) {
            log.error("Failed to setAdd key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * Remove member(s) from set.
     * 
     * @param key set key
     * @param values values to remove
     * @return number of elements removed
     */
    public long setRemove(String key, Object... values) {
        try {
            Long removed = redisTemplate.opsForSet().remove(key, values);
            log.debug("SetRemove key: {} - removed: {}", key, removed);
            return removed != null ? removed : 0;
        } catch (Exception e) {
            log.error("Failed to setRemove key: {}", key, e);
            return 0;
        }
    }
    
    /**
     * Check if value is member of set.
     * 
     * @param key set key
     * @param value value to check
     * @return true if value is in set
     */
    public boolean setIsMember(String key, Object value) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, value);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Failed to check set membership for key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get all members of set.
     * 
     * @param key set key
     * @return set of all members
     */
    public Set<Object> setMembers(String key) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            log.debug("SetMembers key: {} - size: {}", key, members != null ? members.size() : 0);
            return members != null ? members : new HashSet<>();
        } catch (Exception e) {
            log.error("Failed to get set members for key: {}", key, e);
            return new HashSet<>();
        }
    }
    
    /**
     * Get set size.
     * 
     * @param key set key
     * @return number of elements in set
     */
    public long setSize(String key) {
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get set size for key: {}", key, e);
            return 0;
        }
    }
}
