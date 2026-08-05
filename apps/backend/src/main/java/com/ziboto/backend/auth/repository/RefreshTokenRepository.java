package com.ziboto.backend.auth.repository;

import com.ziboto.backend.auth.entity.RefreshToken;
import com.ziboto.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Find refresh token by token hash.
     * 
     * @param tokenHash the BCrypt hashed refresh token
     * @return Optional containing the RefreshToken if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * Find all refresh tokens for a specific user.
     * 
     * @param user the user entity
     * @return List of RefreshTokens
     */
    List<RefreshToken> findByUser(User user);
    
    /**
     * Find all refresh tokens for a specific user with pagination.
     * 
     * @param user the user entity
     * @param pageable pagination information
     * @return Page of RefreshTokens
     */
    Page<RefreshToken> findByUser(User user, Pageable pageable);
    
    /**
     * Find all refresh tokens for a user by user ID.
     * 
     * @param userId the user ID
     * @return List of RefreshTokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId")
    List<RefreshToken> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find all active (non-revoked and non-expired) refresh tokens for a user.
     * 
     * @param user the user entity
     * @param now current timestamp for expiry check
     * @return List of active RefreshTokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user " +
           "AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * Find all active refresh tokens for a user by user ID.
     * 
     * @param userId the user ID
     * @param now current timestamp for expiry check
     * @return List of active RefreshTokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Find valid refresh token by token hash (non-revoked and non-expired).
     * 
     * @param tokenHash the BCrypt hashed refresh token
     * @param now current timestamp for expiry check
     * @return Optional containing the valid RefreshToken if found
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash " +
           "AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidTokenByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    /**
     * Find all expired tokens.
     * 
     * @param now current timestamp
     * @return List of expired RefreshTokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt <= :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Find all revoked tokens.
     * 
     * @return List of revoked RefreshTokens
     */
    List<RefreshToken> findByRevokedTrue();
    
    /**
     * Find all non-revoked tokens.
     * 
     * @return List of non-revoked RefreshTokens
     */
    List<RefreshToken> findByRevokedFalse();
    
    /**
     * Check if a token hash exists and is valid.
     * 
     * @param tokenHash the BCrypt hashed refresh token
     * @param now current timestamp
     * @return true if token exists and is valid
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt " +
           "WHERE rt.tokenHash = :tokenHash AND rt.revoked = false AND rt.expiresAt > :now")
    Boolean existsValidToken(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    /**
     * Count active tokens for a user.
     * 
     * @param userId the user ID
     * @param now current timestamp
     * @return count of active tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > :now")
    Long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Find tokens by device information.
     * 
     * @param userId the user ID
     * @param deviceInfo the device information string
     * @return List of RefreshTokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.deviceInfo = :deviceInfo")
    List<RefreshToken> findByUserIdAndDeviceInfo(@Param("userId") Long userId, 
                                                   @Param("deviceInfo") String deviceInfo);
    
    /**
     * Find tokens by IP address.
     * 
     * @param userId the user ID
     * @param ipAddress the IP address
     * @return List of RefreshTokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.ipAddress = :ipAddress")
    List<RefreshToken> findByUserIdAndIpAddress(@Param("userId") Long userId, 
                                                  @Param("ipAddress") String ipAddress);
    
    /**
     * Delete all expired tokens.
     * This should be called periodically for cleanup.
     * 
     * @param now current timestamp
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt <= :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Delete all revoked tokens older than specified date.
     * 
     * @param before timestamp before which revoked tokens should be deleted
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt < :before")
    int deleteRevokedTokensBefore(@Param("before") LocalDateTime before);
    
    /**
     * Delete all tokens for a specific user.
     * 
     * @param userId the user ID
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
    
    /**
     * Revoke all active tokens for a user (useful for logout all devices).
     * 
     * @param userId the user ID
     * @param now current timestamp
     * @return number of revoked tokens
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
           "WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    int revokeAllActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * Revoke a specific token by token hash.
     * 
     * @param tokenHash the BCrypt hashed refresh token
     * @return number of revoked tokens (0 or 1)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenHash = :tokenHash")
    int revokeToken(@Param("tokenHash") String tokenHash);
    
    /**
     * Update last used timestamp for a token.
     * 
     * @param tokenHash the BCrypt hashed refresh token
     * @param lastUsedAt the timestamp to set
     * @return number of updated tokens (0 or 1)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.lastUsedAt = :lastUsedAt WHERE rt.tokenHash = :tokenHash")
    int updateLastUsedAt(@Param("tokenHash") String tokenHash, @Param("lastUsedAt") LocalDateTime lastUsedAt);
    
    /**
     * Find tokens that haven't been used in a while (for security monitoring).
     * 
     * @param userId the user ID
     * @param inactiveSince timestamp before which last use is considered inactive
     * @param now current timestamp for expiry check
     * @return List of inactive but still valid tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId " +
           "AND rt.revoked = false AND rt.expiresAt > :now " +
           "AND (rt.lastUsedAt IS NULL OR rt.lastUsedAt < :inactiveSince)")
    List<RefreshToken> findInactiveTokensByUserId(@Param("userId") Long userId,
                                                    @Param("inactiveSince") LocalDateTime inactiveSince,
                                                    @Param("now") LocalDateTime now);
}
