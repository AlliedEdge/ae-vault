package com.ziboto.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * 
 * <p>Handles:</p>
 * <ul>
 *   <li>Access token generation (15 minutes expiry)</li>
 *   <li>Refresh token generation (7 days expiry)</li>
 *   <li>Token validation and signature verification</li>
 *   <li>Extracting user information from tokens</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    private SecretKey key;
    
    /**
     * Initialize the secret key from configuration.
     * The key is Base64 decoded and must be at least 256 bits.
     */
    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            log.error("JWT secret is not configured! Set JWT_SECRET environment variable.");
            throw new IllegalStateException("JWT secret must be configured");
        }
        
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT Token Provider initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWT secret key", e);
            throw new IllegalStateException("Failed to initialize JWT secret key", e);
        }
    }
    
    /**
     * Generate an access token for authenticated user.
     * Token expires in 15 minutes by default.
     * 
     * @param authentication Spring Security authentication object
     * @return JWT access token string
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
    }
    
    /**
     * Generate an access token for a username with roles.
     * 
     * @param username the username
     * @param roles list of role names
     * @return JWT access token string
     */
    public String generateToken(String username, Iterable<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("type", "access");
        
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }
    
    /**
     * Generate a refresh token for authenticated user.
     * Token expires in 7 days by default.
     * 
     * @param authentication Spring Security authentication object
     * @return JWT refresh token string
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateRefreshToken(userDetails.getUsername());
    }
    
    /**
     * Generate a refresh token for a username.
     * Refresh tokens have longer expiration and fewer claims.
     * 
     * @param username the username
     * @return JWT refresh token string
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpiration());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }
    
    /**
     * Extract username from JWT token.
     * 
     * @param token JWT token string
     * @return username (subject claim)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * Extract user ID from JWT token claims.
     * 
     * @param token JWT token string
     * @return user ID if present
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("userId") != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }
    
    /**
     * Extract roles from JWT token claims.
     * 
     * @param token JWT token string
     * @return list of role names
     */
    @SuppressWarnings("unchecked")
    public Iterable<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("roles") != null) {
            return (Iterable<String>) claims.get("roles");
        }
        return null;
    }
    
    /**
     * Get token type (access or refresh).
     * 
     * @param token JWT token string
     * @return token type
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("type") : null;
    }
    
    /**
     * Get token expiration date.
     * 
     * @param token JWT token string
     * @return expiration date
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }
    
    /**
     * Get token issued at date.
     * 
     * @param token JWT token string
     * @return issued at date
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getIssuedAt() : null;
    }
    
    /**
     * Validate JWT token.
     * Checks signature, expiration, and token structure.
     * 
     * @param token JWT token string
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT token validation error: {}", ex.getMessage());
        }
        return false;
    }
    
    /**
     * Validate token and check if it's an access token.
     * 
     * @param token JWT token string
     * @return true if valid access token
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token) && "access".equals(getTokenType(token));
    }
    
    /**
     * Validate token and check if it's a refresh token.
     * 
     * @param token JWT token string
     * @return true if valid refresh token
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token) && "refresh".equals(getTokenType(token));
    }
    
    /**
     * Extract all claims from JWT token.
     * Returns null if token is invalid.
     * 
     * @param token JWT token string
     * @return Claims object or null
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            // Return claims even if expired for error handling
            log.debug("Token expired but returning claims for error handling");
            return ex.getClaims();
        } catch (Exception ex) {
            log.error("Failed to parse JWT claims: {}", ex.getMessage());
            return null;
        }
    }
    
    /**
     * Check if token is expired.
     * 
     * @param token JWT token string
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception ex) {
            return true;
        }
    }
    
    /**
     * Get remaining time until token expiration in seconds.
     * 
     * @param token JWT token string
     * @return remaining time in seconds, or 0 if expired
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            if (expiration != null) {
                long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                return Math.max(0, remaining);
            }
        } catch (Exception ex) {
            log.error("Failed to get token remaining time: {}", ex.getMessage());
        }
        return 0;
    }
}
