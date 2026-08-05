package com.ziboto.backend.auth.service;

import com.ziboto.backend.cache.RedisService;
import com.ziboto.backend.config.properties.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OTP (One-Time Password) cache service using Redis.
 * 
 * <p>Manages OTP generation, storage, verification, and rate limiting:</p>
 * <ul>
 *   <li>Generate secure random OTPs (6-digit by default)</li>
 *   <li>Store OTPs with configurable TTL (5 minutes default)</li>
 *   <li>Rate limit OTP generation per identifier</li>
 *   <li>Track verification attempts and auto-invalidate after max attempts</li>
 *   <li>Support for different OTP purposes (email verification, 2FA, password reset)</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * All settings are configurable via RedisProperties:
 * <ul>
 *   <li>OTP TTL: app.redis.otp.ttl-minutes (default: 5)</li>
 *   <li>Max generation attempts: app.redis.otp.max-attempts (default: 3)</li>
 *   <li>Rate limit window: app.redis.otp.rate-limit-minutes (default: 15)</li>
 *   <li>Max verification attempts: app.redis.otp.max-verification-attempts (default: 3)</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>
 * // Generate OTP for email verification
 * String otp = otpCacheService.generateOtp("user@example.com", OtpPurpose.EMAIL_VERIFICATION);
 * 
 * // Verify OTP
 * boolean valid = otpCacheService.verifyOtp("user@example.com", otp, OtpPurpose.EMAIL_VERIFICATION);
 * 
 * // Invalidate OTP after use
 * otpCacheService.invalidateOtp("user@example.com", OtpPurpose.EMAIL_VERIFICATION);
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCacheService {
    
    private final RedisService redisService;
    private final RedisProperties redisProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    
    private static final int DEFAULT_OTP_LENGTH = 6;
    
    /**
     * OTP purpose types for different use cases.
     */
    public enum OtpPurpose {
        EMAIL_VERIFICATION,
        PHONE_VERIFICATION,
        TWO_FACTOR_AUTH,
        PASSWORD_RESET,
        ACCOUNT_RECOVERY
    }
    
    /**
     * Generate and cache a new OTP for the given identifier and purpose.
     * 
     * @param identifier user identifier (email, phone, user ID)
     * @param purpose OTP purpose
     * @return generated OTP string, or null if rate limited
     */
    public String generateOtp(String identifier, OtpPurpose purpose) {
        // Check rate limit
        if (isGenerationRateLimited(identifier, purpose)) {
            log.warn("OTP generation rate limited for: {} - purpose: {}", identifier, purpose);
            return null;
        }
        
        // Generate OTP
        String otp = generateSecureOtp(DEFAULT_OTP_LENGTH);
        
        // Build Redis key
        String otpKey = buildOtpKey(identifier, purpose);
        String metaKey = buildOtpMetaKey(identifier, purpose);
        String rateLimitKey = buildRateLimitKey(identifier, purpose);
        
        // Store OTP
        Duration ttl = Duration.ofMinutes(redisProperties.getOtp().getTtlMinutes());
        redisService.set(otpKey, otp, ttl);
        
        // Store metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("generatedAt", System.currentTimeMillis());
        metadata.put("attempts", 0);
        metadata.put("purpose", purpose.name());
        redisService.hashSetAll(metaKey, metadata);
        redisService.expire(metaKey, ttl);
        
        // Update rate limit counter
        Duration rateLimitWindow = Duration.ofMinutes(redisProperties.getOtp().getRateLimitMinutes());
        redisService.incrementWithTTL(rateLimitKey, rateLimitWindow);
        
        log.info("Generated OTP for: {} - purpose: {} - TTL: {} minutes", 
                identifier, purpose, redisProperties.getOtp().getTtlMinutes());
        
        return otp;
    }
    
    /**
     * Generate OTP with custom length.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @param length OTP length (4-8 digits)
     * @return generated OTP string, or null if rate limited
     */
    public String generateOtp(String identifier, OtpPurpose purpose, int length) {
        if (length < 4 || length > 8) {
            throw new IllegalArgumentException("OTP length must be between 4 and 8");
        }
        
        if (isGenerationRateLimited(identifier, purpose)) {
            log.warn("OTP generation rate limited for: {} - purpose: {}", identifier, purpose);
            return null;
        }
        
        String otp = generateSecureOtp(length);
        String otpKey = buildOtpKey(identifier, purpose);
        Duration ttl = Duration.ofMinutes(redisProperties.getOtp().getTtlMinutes());
        
        redisService.set(otpKey, otp, ttl);
        log.info("Generated {}-digit OTP for: {} - purpose: {}", length, identifier, purpose);
        
        return otp;
    }
    
    /**
     * Verify OTP for the given identifier and purpose.
     * 
     * @param identifier user identifier
     * @param otp OTP to verify
     * @param purpose OTP purpose
     * @return true if OTP is valid
     */
    public boolean verifyOtp(String identifier, String otp, OtpPurpose purpose) {
        if (otp == null || otp.trim().isEmpty()) {
            return false;
        }
        
        String otpKey = buildOtpKey(identifier, purpose);
        String metaKey = buildOtpMetaKey(identifier, purpose);
        
        // Get stored OTP
        String storedOtp = (String) redisService.get(otpKey);
        
        if (storedOtp == null) {
            log.debug("OTP not found or expired for: {} - purpose: {}", identifier, purpose);
            return false;
        }
        
        // Increment verification attempts
        incrementVerificationAttempts(metaKey);
        
        // Check if max attempts exceeded
        int attempts = getVerificationAttempts(metaKey);
        if (attempts > redisProperties.getOtp().getMaxVerificationAttempts()) {
            log.warn("Max OTP verification attempts exceeded for: {} - purpose: {}", identifier, purpose);
            invalidateOtp(identifier, purpose);
            return false;
        }
        
        // Verify OTP (timing-safe comparison)
        boolean valid = timingSafeEquals(otp, storedOtp);
        
        if (valid) {
            log.info("OTP verified successfully for: {} - purpose: {}", identifier, purpose);
        } else {
            log.warn("Invalid OTP for: {} - purpose: {} - attempts: {}/{}", 
                    identifier, purpose, attempts, redisProperties.getOtp().getMaxVerificationAttempts());
        }
        
        return valid;
    }
    
    /**
     * Verify and invalidate OTP in one operation.
     * Use this for one-time verification flows.
     * 
     * @param identifier user identifier
     * @param otp OTP to verify
     * @param purpose OTP purpose
     * @return true if OTP is valid
     */
    public boolean verifyAndInvalidateOtp(String identifier, String otp, OtpPurpose purpose) {
        boolean valid = verifyOtp(identifier, otp, purpose);
        if (valid) {
            invalidateOtp(identifier, purpose);
        }
        return valid;
    }
    
    /**
     * Invalidate OTP immediately.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     */
    public void invalidateOtp(String identifier, OtpPurpose purpose) {
        String otpKey = buildOtpKey(identifier, purpose);
        String metaKey = buildOtpMetaKey(identifier, purpose);
        
        redisService.delete(otpKey);
        redisService.delete(metaKey);
        
        log.debug("Invalidated OTP for: {} - purpose: {}", identifier, purpose);
    }
    
    /**
     * Check if OTP exists and is valid.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @return true if OTP exists
     */
    public boolean hasValidOtp(String identifier, OtpPurpose purpose) {
        String otpKey = buildOtpKey(identifier, purpose);
        return redisService.exists(otpKey);
    }
    
    /**
     * Get remaining time for OTP validity in seconds.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @return remaining time in seconds, or 0 if expired/not found
     */
    public long getOtpRemainingTime(String identifier, OtpPurpose purpose) {
        String otpKey = buildOtpKey(identifier, purpose);
        long ttl = redisService.getTimeToLive(otpKey);
        return Math.max(0, ttl);
    }
    
    /**
     * Get number of verification attempts made.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @return number of attempts
     */
    public int getVerificationAttempts(String identifier, OtpPurpose purpose) {
        String metaKey = buildOtpMetaKey(identifier, purpose);
        return getVerificationAttempts(metaKey);
    }
    
    /**
     * Get remaining verification attempts before invalidation.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @return remaining attempts
     */
    public int getRemainingVerificationAttempts(String identifier, OtpPurpose purpose) {
        int attempts = getVerificationAttempts(identifier, purpose);
        return Math.max(0, redisProperties.getOtp().getMaxVerificationAttempts() - attempts);
    }
    
    /**
     * Check if OTP generation is rate limited.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @return true if rate limited
     */
    private boolean isGenerationRateLimited(String identifier, OtpPurpose purpose) {
        String rateLimitKey = buildRateLimitKey(identifier, purpose);
        Long attempts = (Long) redisService.get(rateLimitKey);
        
        if (attempts == null) {
            return false;
        }
        
        return attempts >= redisProperties.getOtp().getMaxAttempts();
    }
    
    /**
     * Get remaining OTP generation attempts.
     * 
     * @param identifier user identifier
     * @param purpose OTP purpose
     * @return remaining attempts
     */
    public int getRemainingGenerationAttempts(String identifier, OtpPurpose purpose) {
        String rateLimitKey = buildRateLimitKey(identifier, purpose);
        Long attempts = (Long) redisService.get(rateLimitKey);
        
        if (attempts == null) {
            return redisProperties.getOtp().getMaxAttempts();
        }
        
        return Math.max(0, redisProperties.getOtp().getMaxAttempts() - attempts.intValue());
    }
    
    /**
     * Generate cryptographically secure random OTP.
     * 
     * @param length OTP length
     * @return OTP string
     */
    private String generateSecureOtp(int length) {
        int max = (int) Math.pow(10, length);
        int otp = secureRandom.nextInt(max);
        return String.format("%0" + length + "d", otp);
    }
    
    /**
     * Increment verification attempts counter.
     * 
     * @param metaKey metadata key
     */
    private void incrementVerificationAttempts(String metaKey) {
        Object currentAttempts = redisService.hashGet(metaKey, "attempts");
        int attempts = currentAttempts != null ? ((Number) currentAttempts).intValue() : 0;
        redisService.hashSet(metaKey, "attempts", attempts + 1);
    }
    
    /**
     * Get verification attempts from metadata.
     * 
     * @param metaKey metadata key
     * @return number of attempts
     */
    private int getVerificationAttempts(String metaKey) {
        Object attempts = redisService.hashGet(metaKey, "attempts");
        return attempts != null ? ((Number) attempts).intValue() : 0;
    }
    
    /**
     * Timing-safe string comparison to prevent timing attacks.
     * 
     * @param a first string
     * @param b second string
     * @return true if strings are equal
     */
    private boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    /**
     * Build Redis key for OTP storage.
     */
    private String buildOtpKey(String identifier, OtpPurpose purpose) {
        return String.format("%s:otp:%s:%s", 
                redisProperties.getKeyPrefix().getOtp(), 
                purpose.name().toLowerCase(), 
                identifier);
    }
    
    /**
     * Build Redis key for OTP metadata.
     */
    private String buildOtpMetaKey(String identifier, OtpPurpose purpose) {
        return String.format("%s:meta:%s:%s", 
                redisProperties.getKeyPrefix().getOtp(), 
                purpose.name().toLowerCase(), 
                identifier);
    }
    
    /**
     * Build Redis key for rate limiting.
     */
    private String buildRateLimitKey(String identifier, OtpPurpose purpose) {
        return String.format("%s:rate:%s:%s", 
                redisProperties.getKeyPrefix().getOtp(), 
                purpose.name().toLowerCase(), 
                identifier);
    }
}
