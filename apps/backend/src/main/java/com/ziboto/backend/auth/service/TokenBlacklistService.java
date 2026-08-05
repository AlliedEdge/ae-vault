package com.ziboto.backend.auth.service;

import com.ziboto.backend.cache.RedisService;
import com.ziboto.backend.config.properties.RedisProperties;
import com.ziboto.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * JWT Token blacklist service using Redis.
 * 
 * <p>Implements token revocation/blacklisting for logout functionality:</p>
 * <ul>
 *   <li>Blacklist tokens after logout</li>
 *   <li>Blacklist tokens after password change</li>
 *   <li>Blacklist all user tokens (logout all devices)</li>
 *   <li>Automatic expiration based on token TTL</li>
 *   <li>Fast lookup for token validation</li>
 * </ul>
 * 
 * <p>Blacklisted tokens are stored in Redis with TTL matching the token's
 * remaining expiration time. Once the token naturally expires, it's automatically
 * removed from the blacklist.</p>
 * 
 * <h2>Configuration:</h2>
 * Token blacklisting can be enabled/disabled via RedisProperties (app.redis.token-blacklist.enabled).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final RedisService redisService;
    private final RedisProperties redisProperties;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Blacklist a JWT token (typically after logout).
     * Token is stored in Redis until its natural expiration.
     * 
     * @param token JWT token to blacklist
     */
    public void blacklistToken(String token) {
        if (!redisProperties.getTokenBlacklist().isEnabled()) {
            log.debug("Token blacklisting is disabled");
            return;
        }
        
        // Calculate remaining TTL for the token
        long remainingTtl = jwtTokenProvider.getTokenRemainingTime(token);
        
        if (remainingTtl > 0) {
            String key = buildKey("token", token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            
            // Store token with remaining TTL
            redisService.set(key, username, Duration.ofSeconds(remainingTtl));
            
            log.debug("Blacklisted token for user: {} - TTL: {}s", username, remainingTtl);
        } else {
            log.debug("Token already expired, not blacklisting");
        }
    }
    
    /**
     * Check if a token is blacklisted.
     * 
     * @param token JWT token to check
     * @return true if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        if (!redisProperties.getTokenBlacklist().isEnabled()) {
            return false;
        }
        
        String key = buildKey("token", token);
        if (redisService.exists(key)) {
            log.debug("Token is blacklisted");
            return true;
        }
        
        // Also check if all user's tokens are blacklisted
        String username = jwtTokenProvider.getUsernameFromToken(token);
        if (username != null) {
            Date tokenIssuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
            return isUserTokensBlacklistedBefore(username, tokenIssuedAt);
        }
        
        return false;
    }
    
    /**
     * Blacklist all tokens for a user issued before a specific time.
     * Useful for "logout all devices" or after password change.
     * 
     * @param username username
     * @param beforeTime tokens issued before this time are invalidated
     */
    public void blacklistUserTokensBefore(String username, Date beforeTime) {
        if (!redisProperties.getTokenBlacklist().isEnabled()) {
            log.debug("Token blacklisting is disabled");
            return;
        }
        
        String key = buildKey("user", username);
        long timestamp = beforeTime.getTime();
        
        // Store timestamp with configured max TTL
        Duration maxTtl = Duration.ofDays(redisProperties.getTokenBlacklist().getMaxTtlDays());
        redisService.set(key, timestamp, maxTtl);
        
        log.info("Blacklisted all tokens for user: {} issued before: {}", username, beforeTime);
    }
    
    /**
     * Blacklist all current tokens for a user.
     * Typically used for "logout all devices" or password change.
     * 
     * @param username username
     */
    public void blacklistAllUserTokens(String username) {
        blacklistUserTokensBefore(username, new Date());
    }
    
    /**
     * Check if user's tokens issued before a specific time are blacklisted.
     * 
     * @param username username
     * @param tokenIssuedAt token issued at timestamp
     * @return true if token is invalidated
     */
    private boolean isUserTokensBlacklistedBefore(String username, Date tokenIssuedAt) {
        String key = buildKey("user", username);
        Long blacklistTimestamp = (Long) redisService.get(key);
        
        if (blacklistTimestamp != null && tokenIssuedAt != null) {
            boolean blacklisted = tokenIssuedAt.getTime() < blacklistTimestamp;
            if (blacklisted) {
                log.debug("Token for user {} issued before blacklist time", username);
            }
            return blacklisted;
        }
        
        return false;
    }
    
    /**
     * Remove token from blacklist.
     * Rarely needed as tokens auto-expire.
     * 
     * @param token JWT token
     */
    public void removeFromBlacklist(String token) {
        String key = buildKey("token", token);
        redisService.delete(key);
        log.debug("Removed token from blacklist");
    }
    
    /**
     * Clear user token blacklist.
     * Allows previously invalidated tokens to be used again.
     * Use with caution.
     * 
     * @param username username
     */
    public void clearUserBlacklist(String username) {
        String key = buildKey("user", username);
        redisService.delete(key);
        log.info("Cleared token blacklist for user: {}", username);
    }
    
    /**
     * Get remaining time before user blacklist expires.
     * 
     * @param username username
     * @return remaining time in seconds, or 0 if not blacklisted
     */
    public long getUserBlacklistRemainingTime(String username) {
        String key = buildKey("user", username);
        long ttl = redisService.getTimeToLive(key);
        return Math.max(0, ttl);
    }
    
    /**
     * Check if user has any active blacklist.
     * 
     * @param username username
     * @return true if user has active blacklist
     */
    public boolean hasUserBlacklist(String username) {
        String key = buildKey("user", username);
        return redisService.exists(key);
    }
    
    /**
     * Build Redis key with namespace.
     * 
     * @param type key type (token, user)
     * @param identifier identifier (token string, username)
     * @return full Redis key
     */
    private String buildKey(String type, String identifier) {
        return String.format("%s:%s:%s", 
                redisProperties.getKeyPrefix().getTokenBlacklist(), 
                type, 
                identifier);
    }
}
