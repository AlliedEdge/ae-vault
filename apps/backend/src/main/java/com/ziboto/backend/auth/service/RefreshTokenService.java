package com.ziboto.backend.auth.service;

import com.ziboto.backend.auth.entity.RefreshToken;
import com.ziboto.backend.auth.repository.RefreshTokenRepository;
import com.ziboto.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token service for secure token management.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Hash refresh tokens with SHA-256 before storing in PostgreSQL</li>
 *   <li>Validate refresh tokens against hashed values</li>
 *   <li>Support multiple devices per user</li>
 *   <li>Track token usage and device information</li>
 *   <li>Automatic token rotation on refresh</li>
 *   <li>Session management via Redis</li>
 * </ul>
 * 
 * <p>Security:</p>
 * <ul>
 *   <li>Tokens are hashed with SHA-256</li>
 *   <li>Plain tokens never stored in database</li>
 *   <li>Each device gets separate refresh token</li>
 *   <li>Tokens auto-expire after 7 days</li>
 *   <li>Old token invalidated on rotation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionCacheService sessionCacheService;
    
    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 7;
    
    /**
     * Create and store a hashed refresh token.
     * 
     * @param user user entity
     * @param plainToken the plain JWT refresh token
     * @param ipAddress client IP address
     * @param deviceInfo device information
     * @param userAgent user agent string
     * @return saved RefreshToken entity
     */
    @Transactional
    public RefreshToken createRefreshToken(
            User user, 
            String plainToken, 
            String ipAddress, 
            String deviceInfo,
            String userAgent) {
        
        log.debug("Creating refresh token for user: {}", user.getUsername());
        
        // Hash the token with SHA-256
        String tokenHash = hashToken(plainToken);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(REFRESH_TOKEN_EXPIRY_DAYS);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(now)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .userAgent(userAgent)
                .lastUsedAt(now)
                .build();
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        
        log.info("Created refresh token for user: {} - token ID: {}, device: {}", 
                user.getUsername(), refreshToken.getId(), deviceInfo);
        
        return refreshToken;
    }
    
    /**
     * Validate a plain refresh token against all stored hashed tokens for potential matches.
     * This method retrieves all active tokens for the user and checks each hash.
     * 
     * @param plainToken the plain JWT refresh token
     * @param username username from the token
     * @return Optional containing the matching RefreshToken if valid
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String plainToken, String username) {
        log.debug("Validating refresh token for user: {}", username);
        
        // Hash the provided token
        String tokenHash = hashToken(plainToken);
        
        // Get all active (non-revoked, non-expired) tokens for the user
        List<RefreshToken> userTokens = refreshTokenRepository.findAll()
                .stream()
                .filter(token -> token.getUser() != null && 
                               username.equals(token.getUser().getUsername()) &&
                               !token.getRevoked() &&
                               !token.isExpired())
                .toList();
        
        if (userTokens.isEmpty()) {
            log.warn("No active refresh tokens found for user: {}", username);
            return Optional.empty();
        }
        
        log.debug("Found {} active tokens for user: {}", userTokens.size(), username);
        
        // Check each token hash to find a match (constant-time comparison)
        for (RefreshToken storedToken : userTokens) {
            if (MessageDigest.isEqual(
                    tokenHash.getBytes(StandardCharsets.UTF_8),
                    storedToken.getTokenHash().getBytes(StandardCharsets.UTF_8))) {
                log.debug("Refresh token validated successfully for user: {} - token ID: {}", 
                        username, storedToken.getId());
                return Optional.of(storedToken);
            }
        }
        
        log.warn("Invalid refresh token for user: {} - no matching hash found", username);
        return Optional.empty();
    }
    
    /**
     * Revoke a refresh token by its ID.
     * 
     * @param tokenId token UUID
     */
    @Transactional
    public void revokeToken(UUID tokenId) {
        refreshTokenRepository.findById(tokenId).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            
            // Remove from active sessions in Redis
            sessionCacheService.removeActiveSession(
                    token.getUser().getUsername(), 
                    tokenId.toString()
            );
            
            log.info("Revoked refresh token - ID: {}, user: {}", 
                    tokenId, token.getUser().getUsername());
        });
    }
    
    /**
     * Revoke all active tokens for a user.
     * Typically used for "logout all devices" or security events.
     * 
     * @param userId user ID
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        int revokedCount = refreshTokenRepository.revokeAllActiveTokensByUserId(
                userId, 
                LocalDateTime.now()
        );
        
        log.info("Revoked {} active tokens for user ID: {}", revokedCount, userId);
    }
    
    /**
     * Update last used timestamp for a token.
     * 
     * @param tokenId token UUID
     */
    @Transactional
    public void updateLastUsed(UUID tokenId) {
        refreshTokenRepository.findById(tokenId).ifPresent(token -> {
            token.setLastUsedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }
    
    /**
     * Get all active refresh tokens for a user.
     * 
     * @param userId user ID
     * @return list of active RefreshTokens
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveUserTokens(Long userId) {
        return refreshTokenRepository.findActiveTokensByUserId(userId, LocalDateTime.now());
    }
    
    /**
     * Count active tokens for a user.
     * 
     * @param userId user ID
     * @return count of active tokens
     */
    @Transactional(readOnly = true)
    public long countActiveTokens(Long userId) {
        Long count = refreshTokenRepository.countActiveTokensByUserId(userId, LocalDateTime.now());
        return count != null ? count : 0;
    }
    
    /**
     * Clean up expired tokens.
     * Should be called periodically (e.g., via scheduled task).
     * 
     * @return number of deleted tokens
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up {} expired refresh tokens", deletedCount);
        return deletedCount;
    }
    
    /**
     * Clean up revoked tokens older than specified days.
     * 
     * @param daysOld number of days
     * @return number of deleted tokens
     */
    @Transactional
    public int cleanupOldRevokedTokens(int daysOld) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = refreshTokenRepository.deleteRevokedTokensBefore(before);
        log.info("Cleaned up {} revoked tokens older than {} days", deletedCount, daysOld);
        return deletedCount;
    }
    
    /**
     * Hash a token using SHA-256.
     * This is used instead of BCrypt because JWT tokens are too long (>72 bytes).
     * SHA-256 is secure for this use case as:
     * - JWTs are high-entropy random tokens
     * - We're not hashing passwords (which need salts and slow hashing)
     * - The hash is only for comparison, not for preventing brute force
     * 
     * @param token the token to hash
     * @return Base64-encoded SHA-256 hash
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
