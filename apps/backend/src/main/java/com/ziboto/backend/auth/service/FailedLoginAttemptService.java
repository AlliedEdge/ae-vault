package com.ziboto.backend.auth.service;

import com.ziboto.backend.cache.RedisService;
import com.ziboto.backend.config.properties.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Failed login attempt tracking service using Redis.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Track failed login attempts per user and IP</li>
 *   <li>Automatic account lockout after threshold</li>
 *   <li>Automatic unlock after cooldown period</li>
 *   <li>Detailed attempt logging</li>
 *   <li>Security monitoring and alerting</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * All settings are configurable via RedisProperties (app.redis.failed-login.*):
 * <ul>
 *   <li>Max failed attempts: default 5</li>
 *   <li>Lockout duration: default 30 minutes</li>
 *   <li>Tracking window: default 1 hour</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailedLoginAttemptService {
    
    private final RedisService redisService;
    private final RedisProperties redisProperties;
    
    /**
     * Record a failed login attempt.
     * 
     * @param identifier username, email, or IP address
     */
    public void recordFailedAttempt(String identifier) {
        String attemptsKey = buildKey("attempts", identifier);
        String lastAttemptKey = buildKey("last", identifier);
        
        int maxAttempts = redisProperties.getFailedLogin().getMaxAttempts();
        Duration trackingWindow = Duration.ofHours(redisProperties.getFailedLogin().getTrackingHours());
        
        // Increment failed attempts counter
        Long attempts = redisService.incrementWithTTL(attemptsKey, trackingWindow);
        
        if (attempts != null) {
            // Record timestamp of last attempt
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            redisService.set(lastAttemptKey, timestamp, trackingWindow);
            
            log.debug("Recorded failed login attempt for: {} - count: {}/{}", 
                    identifier, attempts, maxAttempts);
            
            // Check if should lock account
            if (attempts >= maxAttempts) {
                lockAccount(identifier);
            }
        }
    }
    
    /**
     * Check if account is locked due to failed attempts.
     * 
     * @param identifier username, email, or IP address
     * @return true if account is locked
     */
    public boolean isLocked(String identifier) {
        String lockoutKey = buildKey("lockout", identifier);
        boolean locked = redisService.exists(lockoutKey);
        if (locked) {
            log.debug("Account locked: {}", identifier);
        }
        return locked;
    }
    
    /**
     * Get number of failed login attempts.
     * 
     * @param identifier username, email, or IP address
     * @return number of failed attempts
     */
    public int getFailedAttempts(String identifier) {
        String attemptsKey = buildKey("attempts", identifier);
        Long attempts = (Long) redisService.get(attemptsKey);
        return attempts != null ? attempts.intValue() : 0;
    }
    
    /**
     * Get remaining attempts before lockout.
     * 
     * @param identifier username, email, or IP address
     * @return remaining attempts
     */
    public int getRemainingAttempts(String identifier) {
        int failed = getFailedAttempts(identifier);
        int maxAttempts = redisProperties.getFailedLogin().getMaxAttempts();
        return Math.max(0, maxAttempts - failed);
    }
    
    /**
     * Get time until account unlock.
     * 
     * @param identifier username, email, or IP address
     * @return remaining lockout time in seconds, or 0 if not locked
     */
    public long getLockoutRemainingTime(String identifier) {
        String lockoutKey = buildKey("lockout", identifier);
        long ttl = redisService.getTimeToLive(lockoutKey);
        return Math.max(0, ttl);
    }
    
    /**
     * Reset failed attempts after successful login.
     * 
     * @param identifier username, email, or IP address
     */
    public void resetFailedAttempts(String identifier) {
        String attemptsKey = buildKey("attempts", identifier);
        String lockoutKey = buildKey("lockout", identifier);
        String lastAttemptKey = buildKey("last", identifier);
        
        redisService.delete(attemptsKey);
        redisService.delete(lockoutKey);
        redisService.delete(lastAttemptKey);
        log.debug("Reset failed attempts for: {}", identifier);
    }
    
    /**
     * Lock account for configured duration.
     * 
     * @param identifier username, email, or IP address
     */
    private void lockAccount(String identifier) {
        String lockoutKey = buildKey("lockout", identifier);
        Duration lockoutDuration = Duration.ofMinutes(redisProperties.getFailedLogin().getLockoutMinutes());
        
        String lockTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        redisService.set(lockoutKey, lockTimestamp, lockoutDuration);
        
        log.warn("Account locked due to failed login attempts: {} - duration: {}", 
                identifier, lockoutDuration);
    }
    
    /**
     * Manually unlock account.
     * Typically used by administrators.
     * 
     * @param identifier username, email, or IP address
     */
    public void unlockAccount(String identifier) {
        resetFailedAttempts(identifier);
        log.info("Account manually unlocked: {}", identifier);
    }
    
    /**
     * Get timestamp of last failed attempt.
     * 
     * @param identifier username, email, or IP address
     * @return timestamp string or null
     */
    public String getLastFailedAttemptTime(String identifier) {
        String lastAttemptKey = buildKey("last", identifier);
        return (String) redisService.get(lastAttemptKey);
    }
    
    /**
     * Check if account should be locked based on failed attempts.
     * 
     * @param identifier username, email, or IP address
     * @return true if should be locked
     */
    public boolean shouldLock(String identifier) {
        int maxAttempts = redisProperties.getFailedLogin().getMaxAttempts();
        return getFailedAttempts(identifier) >= maxAttempts;
    }
    
    /**
     * Build Redis key with namespace.
     * 
     * @param type key type (attempts, lockout, last)
     * @param identifier identifier (username, email, IP)
     * @return full Redis key
     */
    private String buildKey(String type, String identifier) {
        return String.format("%s:%s:%s", 
                redisProperties.getKeyPrefix().getFailedLogin(), 
                type, 
                identifier);
    }
}
