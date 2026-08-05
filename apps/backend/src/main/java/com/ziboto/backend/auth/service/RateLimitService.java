package com.ziboto.backend.auth.service;

import com.ziboto.backend.cache.RedisService;
import com.ziboto.backend.config.properties.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting service using Redis.
 * 
 * <p>Implements token bucket algorithm for rate limiting with Redis:</p>
 * <ul>
 *   <li>Login rate limiting (prevent brute force attacks)</li>
 *   <li>Signup rate limiting (prevent spam registrations)</li>
 *   <li>API rate limiting per user/IP</li>
 *   <li>Token refresh rate limiting</li>
 *   <li>Automatic TTL expiration</li>
 *   <li>Sliding window rate limiting</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * All limits are configurable via RedisProperties (app.redis.rate-limit.*):
 * <ul>
 *   <li>Login attempts: default 5 per 15 minutes</li>
 *   <li>Signup attempts: default 3 per 60 minutes</li>
 *   <li>API calls: default 100 per minute</li>
 *   <li>Token refresh: default 10 per hour</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisService redisService;
    private final RedisProperties redisProperties;
    
    /**
     * Check if login rate limit is exceeded for identifier (username, email, or IP).
     * 
     * @param identifier username, email, or IP address
     * @return true if rate limit exceeded
     */
    public boolean isLoginRateLimitExceeded(String identifier) {
        String key = buildKey("login", identifier);
        int maxAttempts = redisProperties.getRateLimit().getLogin().getMaxAttempts();
        Duration window = Duration.ofMinutes(redisProperties.getRateLimit().getLogin().getWindowMinutes());
        return isRateLimitExceeded(key, maxAttempts, window);
    }
    
    /**
     * Record a login attempt for identifier.
     * 
     * @param identifier username, email, or IP address
     * @return remaining attempts before rate limit
     */
    public int recordLoginAttempt(String identifier) {
        String key = buildKey("login", identifier);
        int maxAttempts = redisProperties.getRateLimit().getLogin().getMaxAttempts();
        Duration window = Duration.ofMinutes(redisProperties.getRateLimit().getLogin().getWindowMinutes());
        return recordAttempt(key, maxAttempts, window);
    }
    
    /**
     * Get remaining login attempts for identifier.
     * 
     * @param identifier username, email, or IP address
     * @return remaining attempts
     */
    public int getRemainingLoginAttempts(String identifier) {
        String key = buildKey("login", identifier);
        int maxAttempts = redisProperties.getRateLimit().getLogin().getMaxAttempts();
        return getRemainingAttempts(key, maxAttempts);
    }
    
    /**
     * Reset login rate limit for identifier.
     * Typically called after successful login.
     * 
     * @param identifier username, email, or IP address
     */
    public void resetLoginRateLimit(String identifier) {
        String key = buildKey("login", identifier);
        redisService.delete(key);
        log.debug("Reset login rate limit for: {}", identifier);
    }
    
    /**
     * Get time until login rate limit reset.
     * 
     * @param identifier username, email, or IP address
     * @return remaining time in seconds, or 0 if not limited
     */
    public long getLoginRateLimitResetTime(String identifier) {
        String key = buildKey("login", identifier);
        return getResetTime(key);
    }
    
    /**
     * Check if signup rate limit is exceeded for identifier (email or IP).
     * 
     * @param identifier email or IP address
     * @return true if rate limit exceeded
     */
    public boolean isSignupRateLimitExceeded(String identifier) {
        String key = buildKey("signup", identifier);
        int maxAttempts = redisProperties.getRateLimit().getSignup().getMaxAttempts();
        Duration window = Duration.ofMinutes(redisProperties.getRateLimit().getSignup().getWindowMinutes());
        return isRateLimitExceeded(key, maxAttempts, window);
    }
    
    /**
     * Record a signup attempt for identifier.
     * 
     * @param identifier email or IP address
     * @return remaining attempts before rate limit
     */
    public int recordSignupAttempt(String identifier) {
        String key = buildKey("signup", identifier);
        int maxAttempts = redisProperties.getRateLimit().getSignup().getMaxAttempts();
        Duration window = Duration.ofMinutes(redisProperties.getRateLimit().getSignup().getWindowMinutes());
        return recordAttempt(key, maxAttempts, window);
    }
    
    /**
     * Get remaining signup attempts for identifier.
     * 
     * @param identifier email or IP address
     * @return remaining attempts
     */
    public int getRemainingSignupAttempts(String identifier) {
        String key = buildKey("signup", identifier);
        int maxAttempts = redisProperties.getRateLimit().getSignup().getMaxAttempts();
        return getRemainingAttempts(key, maxAttempts);
    }
    
    /**
     * Reset signup rate limit for identifier.
     * 
     * @param identifier email or IP address
     */
    public void resetSignupRateLimit(String identifier) {
        String key = buildKey("signup", identifier);
        redisService.delete(key);
        log.debug("Reset signup rate limit for: {}", identifier);
    }
    
    /**
     * Check if API rate limit is exceeded for user.
     * 
     * @param userId user ID
     * @return true if rate limit exceeded
     */
    public boolean isApiRateLimitExceeded(Long userId) {
        String key = buildKey("api", userId.toString());
        int maxRequests = redisProperties.getRateLimit().getApi().getMaxRequests();
        Duration window = Duration.ofMinutes(redisProperties.getRateLimit().getApi().getWindowMinutes());
        return isRateLimitExceeded(key, maxRequests, window);
    }
    
    /**
     * Record an API request for user.
     * 
     * @param userId user ID
     * @return remaining requests before rate limit
     */
    public int recordApiRequest(Long userId) {
        String key = buildKey("api", userId.toString());
        int maxRequests = redisProperties.getRateLimit().getApi().getMaxRequests();
        Duration window = Duration.ofMinutes(redisProperties.getRateLimit().getApi().getWindowMinutes());
        return recordAttempt(key, maxRequests, window);
    }
    
    /**
     * Check if token refresh rate limit is exceeded.
     * 
     * @param userId user ID
     * @return true if rate limit exceeded
     */
    public boolean isRefreshRateLimitExceeded(Long userId) {
        String key = buildKey("refresh", userId.toString());
        int maxAttempts = redisProperties.getRateLimit().getRefresh().getMaxAttempts();
        Duration window = Duration.ofHours(redisProperties.getRateLimit().getRefresh().getWindowHours());
        return isRateLimitExceeded(key, maxAttempts, window);
    }
    
    /**
     * Record a token refresh attempt.
     * 
     * @param userId user ID
     * @return remaining attempts before rate limit
     */
    public int recordRefreshAttempt(Long userId) {
        String key = buildKey("refresh", userId.toString());
        int maxAttempts = redisProperties.getRateLimit().getRefresh().getMaxAttempts();
        Duration window = Duration.ofHours(redisProperties.getRateLimit().getRefresh().getWindowHours());
        return recordAttempt(key, maxAttempts, window);
    }
    
    /**
     * Generic rate limit check.
     * 
     * @param key Redis key
     * @param maxAttempts maximum attempts allowed
     * @param window time window
     * @return true if rate limit exceeded
     */
    private boolean isRateLimitExceeded(String key, int maxAttempts, Duration window) {
        Long count = (Long) redisService.get(key);
        if (count == null) {
            return false;
        }
        boolean exceeded = count >= maxAttempts;
        if (exceeded) {
            log.warn("Rate limit exceeded for key: {} - attempts: {}/{}", key, count, maxAttempts);
        }
        return exceeded;
    }
    
    /**
     * Record an attempt and update counter.
     * 
     * @param key Redis key
     * @param maxAttempts maximum attempts allowed
     * @param window time window
     * @return remaining attempts
     */
    private int recordAttempt(String key, int maxAttempts, Duration window) {
        Long count = redisService.incrementWithTTL(key, window);
        if (count != null) {
            int remaining = Math.max(0, maxAttempts - count.intValue());
            log.debug("Recorded attempt for key: {} - count: {}/{} - remaining: {}", 
                    key, count, maxAttempts, remaining);
            return remaining;
        }
        return maxAttempts;
    }
    
    /**
     * Get remaining attempts before rate limit.
     * 
     * @param key Redis key
     * @param maxAttempts maximum attempts allowed
     * @return remaining attempts
     */
    private int getRemainingAttempts(String key, int maxAttempts) {
        Long count = (Long) redisService.get(key);
        if (count == null) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - count.intValue());
    }
    
    /**
     * Get time until rate limit reset.
     * 
     * @param key Redis key
     * @return remaining time in seconds
     */
    private long getResetTime(String key) {
        long ttl = redisService.getTimeToLive(key);
        return Math.max(0, ttl);
    }
    
    /**
     * Build Redis key with namespace.
     * 
     * @param type rate limit type (login, signup, api, refresh)
     * @param identifier identifier (username, email, IP, user ID)
     * @return full Redis key
     */
    private String buildKey(String type, String identifier) {
        return String.format("%s:%s:%s", 
                redisProperties.getKeyPrefix().getRateLimit(), 
                type, 
                identifier);
    }
}
