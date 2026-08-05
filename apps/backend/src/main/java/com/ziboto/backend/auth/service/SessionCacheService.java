package com.ziboto.backend.auth.service;

import com.ziboto.backend.cache.RedisService;
import com.ziboto.backend.config.properties.RedisProperties;
import com.ziboto.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Session cache service using Redis.
 * 
 * <p>Caches user session data to reduce database queries:</p>
 * <ul>
 *   <li>User profile information</li>
 *   <li>User permissions and roles</li>
 *   <li>Active session tracking</li>
 *   <li>Session metadata (IP, device, etc.)</li>
 *   <li>Automatic TTL expiration</li>
 *   <li>Optional sliding window expiration</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * All settings are configurable via RedisProperties (app.redis.session.*):
 * <ul>
 *   <li>Session TTL: default 1 hour</li>
 *   <li>Extended TTL: default 24 hours (for metadata)</li>
 *   <li>Sliding window: default enabled</li>
 *   <li>Max concurrent sessions: default 0 (unlimited)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCacheService {
    
    private final RedisService redisService;
    private final RedisProperties redisProperties;
    
    /**
     * Cache user session data.
     * 
     * @param username username
     * @param userResponse user data to cache
     */
    public void cacheUserSession(String username, UserResponse userResponse) {
        String key = buildKey("user", username);
        Duration ttl = Duration.ofHours(redisProperties.getSession().getTtlHours());
        redisService.set(key, userResponse, ttl);
        log.debug("Cached session for user: {}", username);
    }
    
    /**
     * Cache user session with custom TTL.
     * 
     * @param username username
     * @param userResponse user data to cache
     * @param ttl time-to-live duration
     */
    public void cacheUserSession(String username, UserResponse userResponse, Duration ttl) {
        String key = buildKey("user", username);
        redisService.set(key, userResponse, ttl);
        log.debug("Cached session for user: {} with TTL: {}", username, ttl);
    }
    
    /**
     * Get cached user session data.
     * 
     * @param username username
     * @return cached user data or null if not found
     */
    public UserResponse getCachedUserSession(String username) {
        String key = buildKey("user", username);
        Object cached = redisService.get(key);
        
        if (cached instanceof UserResponse) {
            log.debug("Retrieved cached session for user: {}", username);
            
            // Refresh TTL on access if sliding window is enabled
            if (redisProperties.getSession().isSlidingWindow()) {
                Duration ttl = Duration.ofHours(redisProperties.getSession().getTtlHours());
                redisService.expire(key, ttl);
            }
            
            return (UserResponse) cached;
        }
        
        log.debug("No cached session found for user: {}", username);
        return null;
    }
    
    /**
     * Invalidate user session cache.
     * Typically called after profile update or logout.
     * 
     * @param username username
     */
    public void invalidateUserSession(String username) {
        String key = buildKey("user", username);
        redisService.delete(key);
        log.debug("Invalidated session for user: {}", username);
    }
    
    /**
     * Store session metadata (IP, device, login time, etc.).
     * 
     * @param sessionId session identifier (e.g., refresh token ID)
     * @param metadata session metadata
     */
    public void cacheSessionMetadata(String sessionId, Map<String, Object> metadata) {
        String key = buildKey("meta", sessionId);
        Duration ttl = Duration.ofHours(redisProperties.getSession().getExtendedTtlHours());
        redisService.hashSetAll(key, metadata);
        redisService.expire(key, ttl);
        log.debug("Cached session metadata for session: {}", sessionId);
    }
    
    /**
     * Get session metadata.
     * 
     * @param sessionId session identifier
     * @return session metadata map
     */
    public Map<String, Object> getSessionMetadata(String sessionId) {
        String key = buildKey("meta", sessionId);
        Map<String, Object> metadata = redisService.hashGetAll(key);
        log.debug("Retrieved session metadata for session: {}", sessionId);
        return metadata;
    }
    
    /**
     * Update session metadata field.
     * 
     * @param sessionId session identifier
     * @param field metadata field name
     * @param value field value
     */
    public void updateSessionMetadata(String sessionId, String field, Object value) {
        String key = buildKey("meta", sessionId);
        redisService.hashSet(key, field, value);
        log.debug("Updated session metadata for session: {} - field: {}", sessionId, field);
    }
    
    /**
     * Delete session metadata.
     * 
     * @param sessionId session identifier
     */
    public void deleteSessionMetadata(String sessionId) {
        String key = buildKey("meta", sessionId);
        redisService.delete(key);
        log.debug("Deleted session metadata for session: {}", sessionId);
    }
    
    /**
     * Track active session for user.
     * Useful for "active sessions" page and concurrent session limiting.
     * 
     * @param username username
     * @param sessionId session identifier
     * @param deviceInfo device information
     */
    public void trackActiveSession(String username, String sessionId, String deviceInfo) {
        String key = buildKey("active", username);
        Duration ttl = Duration.ofHours(redisProperties.getSession().getExtendedTtlHours());
        
        // Check concurrent session limit if enabled
        int maxSessions = redisProperties.getSession().getMaxConcurrentSessions();
        if (maxSessions > 0) {
            long currentSessions = redisService.hashSize(key);
            if (currentSessions >= maxSessions) {
                log.warn("Max concurrent sessions ({}) reached for user: {}", maxSessions, username);
                // Could implement logic to remove oldest session here
            }
        }
        
        redisService.hashSet(key, sessionId, deviceInfo);
        redisService.expire(key, ttl);
        log.debug("Tracked active session for user: {} - session: {}", username, sessionId);
    }
    
    /**
     * Get all active sessions for user.
     * 
     * @param username username
     * @return map of session ID to device info
     */
    public Map<String, Object> getActiveSessions(String username) {
        String key = buildKey("active", username);
        Map<String, Object> sessions = redisService.hashGetAll(key);
        log.debug("Retrieved {} active sessions for user: {}", sessions.size(), username);
        return sessions;
    }
    
    /**
     * Remove active session tracking.
     * 
     * @param username username
     * @param sessionId session identifier
     */
    public void removeActiveSession(String username, String sessionId) {
        String key = buildKey("active", username);
        redisService.hashDelete(key, sessionId);
        log.debug("Removed active session for user: {} - session: {}", username, sessionId);
    }
    
    /**
     * Clear all active sessions for user.
     * Typically used during "logout all devices".
     * 
     * @param username username
     */
    public void clearAllActiveSessions(String username) {
        String key = buildKey("active", username);
        redisService.delete(key);
        log.info("Cleared all active sessions for user: {}", username);
    }
    
    /**
     * Count active sessions for user.
     * 
     * @param username username
     * @return number of active sessions
     */
    public long countActiveSessions(String username) {
        String key = buildKey("active", username);
        return redisService.hashSize(key);
    }
    
    /**
     * Check if session exists.
     * 
     * @param username username
     * @param sessionId session identifier
     * @return true if session is tracked
     */
    public boolean isSessionActive(String username, String sessionId) {
        String key = buildKey("active", username);
        return redisService.hashExists(key, sessionId);
    }
    
    /**
     * Refresh session TTL to prevent expiration.
     * 
     * @param username username
     */
    public void refreshSessionTTL(String username) {
        String key = buildKey("user", username);
        if (redisService.exists(key)) {
            Duration ttl = Duration.ofHours(redisProperties.getSession().getTtlHours());
            redisService.expire(key, ttl);
            log.debug("Refreshed session TTL for user: {}", username);
        }
    }
    
    /**
     * Build Redis key with namespace.
     * 
     * @param type key type (user, meta, active)
     * @param identifier identifier (username, session ID)
     * @return full Redis key
     */
    private String buildKey(String type, String identifier) {
        return String.format("%s:%s:%s", 
                redisProperties.getKeyPrefix().getSession(), 
                type, 
                identifier);
    }
}
