package com.ziboto.backend.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration properties.
 * 
 * <p>Properties are loaded from application.yml and can be overridden by environment variables:</p>
 * <ul>
 *   <li>JWT_SECRET - Secret key for signing tokens (must be at least 256 bits)</li>
 *   <li>JWT_EXPIRATION - Access token expiration in milliseconds (default: 15 minutes)</li>
 *   <li>JWT_REFRESH_EXPIRATION - Refresh token expiration in milliseconds (default: 7 days)</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {
    
    /**
     * Secret key for signing JWT tokens.
     * Must be Base64 encoded and at least 256 bits (32 bytes).
     * Set via JWT_SECRET environment variable.
     */
    private String secret;
    
    /**
     * Access token expiration time in milliseconds.
     * Default: 900000 (15 minutes)
     * Set via JWT_EXPIRATION environment variable.
     */
    private Long expiration = 900000L; // 15 minutes
    
    /**
     * Refresh token expiration time in milliseconds.
     * Default: 604800000 (7 days)
     * Set via JWT_REFRESH_EXPIRATION environment variable.
     */
    private Long refreshExpiration = 604800000L; // 7 days
    
    /**
     * Token issuer identifier.
     */
    private String issuer = "ziboto";
    
    /**
     * Token audience identifier.
     */
    private String audience = "ziboto-api";
    
    /**
     * Get access token expiration in seconds.
     * 
     * @return expiration time in seconds
     */
    public Long getExpirationInSeconds() {
        return expiration / 1000;
    }
    
    /**
     * Get refresh token expiration in seconds.
     * 
     * @return refresh expiration time in seconds
     */
    public Long getRefreshExpirationInSeconds() {
        return refreshExpiration / 1000;
    }
}
