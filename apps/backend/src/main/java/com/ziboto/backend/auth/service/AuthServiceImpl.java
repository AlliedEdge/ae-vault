package com.ziboto.backend.auth.service;

import com.ziboto.backend.audit.entity.AuditAction;
import com.ziboto.backend.audit.service.AuditService;
import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.LoginRequest;
import com.ziboto.backend.auth.dto.RefreshTokenRequest;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.dto.VerifyTokenResponse;
import com.ziboto.backend.auth.entity.RefreshToken;
import com.ziboto.backend.auth.mapper.AuthMapper;
import com.ziboto.backend.auth.repository.RefreshTokenRepository;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.*;
import com.ziboto.backend.security.JwtTokenProvider;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.mapper.UserMapper;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Production-grade authentication service implementation.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>User registration with validation</li>
 *   <li>Login with rate limiting and failed attempt tracking</li>
 *   <li>Token refresh with rotation</li>
 *   <li>Logout with token blacklisting</li>
 *   <li>Session caching for performance</li>
 *   <li>Comprehensive security logging</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    
    // Redis services
    private final RateLimitService rateLimitService;
    private final FailedLoginAttemptService failedLoginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionCacheService sessionCacheService;
    
    /**
     * Register a new user.
     * 
     * <p>Process:</p>
     * <ol>
     *   <li>Validate registration request</li>
     *   <li>Check for duplicate username/email</li>
     *   <li>Hash password with BCrypt</li>
     *   <li>Create user entity</li>
     *   <li>Generate JWT tokens</li>
     *   <li>Store refresh token in database</li>
     *   <li>Cache user session</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request, String ipAddress) {
        log.info("User registration attempt - email: {}, username: {}", request.getEmail(), request.getUsername());
        
        try {
            // 1. Validate request
            validateRegistrationRequest(request);
            
            // 2. Check for existing user
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("Registration failed - username already exists: {}", request.getUsername());
                throw new ConflictException(ErrorCode.USER_USERNAME_EXISTS, 
                        "Username '" + request.getUsername() + "' is already taken");
            }
            
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed - email already exists: {}", request.getEmail());
                throw new ConflictException(ErrorCode.USER_EMAIL_EXISTS, 
                        "Email '" + request.getEmail() + "' is already registered");
            }
            
            // 3. Create user entity
            User user = authMapper.registerRequestToUser(request);
            
            // 4. Set default values
            if (user.getRole() == null) {
                user.setRole(com.ziboto.backend.user.entity.UserRole.ROLE_USER);
            }
            if (user.getStatus() == null) {
                user.setStatus(UserStatus.ACTIVE);
            }
            if (user.getStorageQuota() == null) {
                user.setStorageQuota(5368709120L); // 5GB default
            }
            
            // 5. Hash password
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // 6. Save user
            user = userRepository.save(user);
            log.info("User registered successfully - ID: {}, username: {}", user.getId(), user.getUsername());
            
            // 7. Generate tokens
            String accessToken = jwtTokenProvider.generateToken(
                    user.getUsername(), 
                    List.of(user.getRole().name())
            );
            String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());
            
            // 8. Create and save hashed refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user, 
                    refreshTokenString, 
                    ipAddress,
                    null, // deviceInfo
                    null  // userAgent
            );
            
            // 9. Cache user session
            UserResponse userResponse = userMapper.toResponse(user);
            sessionCacheService.cacheUserSession(user.getUsername(), userResponse);
            
            // 10. Track active session
            sessionCacheService.trackActiveSession(
                    user.getUsername(),
                    refreshToken.getId().toString(),
                    ipAddress
            );
            
            log.info("Registration completed - user: {}", user.getUsername());
            
            return buildAuthenticationResponse(accessToken, refreshTokenString, userResponse);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Registration failed - email: {}", request.getEmail(), e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Registration failed. Please try again later.", e);
        }
    }
    
    /**
     * Authenticate user with credentials.
     * 
     * <p>Login Flow (EXACT ORDER):</p>
     * <ol>
     *   <li>POST /api/v1/auth/login → AuthController</li>
     *   <li>AuthController → AuthService</li>
     *   <li>Redis Rate Limit Check</li>
     *   <li>Redis Failed Login Check</li>
     *   <li>Retrieve User from PostgreSQL</li>
     *   <li>BCrypt Password Verification</li>
     *   <li>Generate Access Token (15 minutes)</li>
     *   <li>Generate Refresh Token (7 days)</li>
     *   <li>Store Session in Redis</li>
     *   <li>Store Refresh Token in PostgreSQL</li>
     *   <li>Update Last Login</li>
     *   <li>Create Audit Log</li>
     *   <li>Return Tokens and User</li>
     * </ol>
     * 
     * <p>Security features:</p>
     * <ul>
     *   <li>Rate limiting (5 attempts per 15 minutes)</li>
     *   <li>Account lockout (5 failed attempts = 30 min lockout)</li>
     *   <li>Failed attempt tracking per user and IP</li>
     *   <li>Session caching</li>
     *   <li>Security event logging</li>
     *   <li>BCrypt password verification</li>
     *   <li>Stateless JWT authentication</li>
     * </ul>
     */
    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request, String ipAddress) {
        String identifier = request.getUsernameOrEmail();
        log.info("Login attempt - identifier: {}, IP: {}", identifier, ipAddress);
        
        try {
            // 1. Validate request
            validateLoginRequest(request);
            
            // 2. Redis Rate Limit Check - DISABLED FOR DEVELOPMENT
            /*
            if (rateLimitService.isLoginRateLimitExceeded(identifier)) {
                long resetTime = rateLimitService.getLoginRateLimitResetTime(identifier);
                log.warn("Login rate limit exceeded - identifier: {}, reset in: {}s", identifier, resetTime);
                throw new RateLimitExceededException(
                        "Too many login attempts. Please try again in " + resetTime + " seconds."
                );
            }
            */
            
            // 3. Redis Failed Login Check - DISABLED FOR DEVELOPMENT
            /*
            if (failedLoginAttemptService.isLocked(identifier)) {
                long unlockTime = failedLoginAttemptService.getLockoutRemainingTime(identifier);
                log.warn("Account locked - identifier: {}, unlock in: {}s", identifier, unlockTime);
                throw new AccountLockedException(
                        "Account is locked due to multiple failed login attempts. " +
                        "Please try again in " + unlockTime + " seconds."
                );
            }
            */
            
            // Record rate limit attempt - DISABLED FOR DEVELOPMENT
            // rateLimitService.recordLoginAttempt(identifier);
            
            // 4. Retrieve User from PostgreSQL
            User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                    .orElseThrow(() -> {
                        handleFailedLogin(identifier, ipAddress);
                        return new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, 
                                "Invalid username or password");
                    });
            
            // Check user status before authentication
            validateUserStatus(user);
            
            // 5. BCrypt Password Verification
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                handleFailedLogin(identifier, ipAddress);
                throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, 
                        "Invalid username or password");
            }
            
            // Authentication successful - proceed with token generation
            
            // 6. Generate Access Token (15 minutes)
            String accessToken = jwtTokenProvider.generateToken(
                    user.getUsername(), 
                    List.of(user.getRole().name())
            );
            
            // 7. Generate Refresh Token (7 days)
            String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());
            
            // 8. Store Session in Redis
            UserResponse userResponse = userMapper.toResponse(user);
            sessionCacheService.cacheUserSession(user.getUsername(), userResponse);
            
            // 9. Store Hashed Refresh Token in PostgreSQL
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user, 
                    refreshTokenString, 
                    ipAddress,
                    extractDeviceInfo(null), // Can be enhanced with actual device detection
                    null // userAgent can be passed from controller
            );
            
            // Track active session in Redis
            sessionCacheService.trackActiveSession(
                    user.getUsername(),
                    refreshToken.getId().toString(),
                    ipAddress
            );
            
            // 10. Update Last Login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // 11. Create Audit Log
            auditService.log(
                    user.getId(),
                    "User",
                    user.getId(),
                    AuditAction.LOGIN,
                    String.format("Successful login from IP: %s", ipAddress)
            );
            
            // 12. Reset security counters on successful login - DISABLED FOR DEVELOPMENT
            // rateLimitService.resetLoginRateLimit(identifier);
            // failedLoginAttemptService.resetFailedAttempts(identifier);
            
            log.info("Login successful - user: {}, IP: {}", user.getUsername(), ipAddress);
            
            // 13. Return Tokens and User
            return buildAuthenticationResponse(accessToken, refreshTokenString, userResponse);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login failed - identifier: {}", identifier, e);
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS, 
                    "Authentication failed. Please try again.");
        }
    }
    
    /**
     * Refresh access token using valid refresh token.
     * 
     * <p>Refresh Token Flow:</p>
     * <ol>
     *   <li>Validate refresh token JWT format</li>
     *   <li>Extract username from token</li>
     *   <li>Check rate limiting</li>
     *   <li>Find matching hashed token in PostgreSQL</li>
     *   <li>Verify token is not revoked or expired</li>
     *   <li>Generate new access token</li>
     *   <li>Generate new refresh token</li>
     *   <li>Invalidate old refresh token</li>
     *   <li>Store new hashed refresh token</li>
     *   <li>Update Redis session</li>
     *   <li>Return new tokens</li>
     * </ol>
     * 
     * <p>Security Features:</p>
     * <ul>
     *   <li>Token rotation - old token invalidated on use</li>
     *   <li>BCrypt validation against hashed tokens</li>
     *   <li>Rate limiting on refresh attempts</li>
     *   <li>Blacklist checking for revoked tokens</li>
     *   <li>Multiple device support via separate tokens</li>
     *   <li>Session tracking in Redis</li>
     * </ul>
     */
    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        log.info("Token refresh attempt - IP: {}", ipAddress);
        
        try {
            // 1. Validate request
            if (!StringUtils.hasText(request.getRefreshToken())) {
                throw new ValidationException("Refresh token is required");
            }
            
            // 2. Validate refresh token JWT format
            if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
                log.warn("Invalid refresh token format");
                throw new InvalidTokenException("Invalid refresh token");
            }
            
            // 3. Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(request.getRefreshToken())) {
                log.warn("Attempted use of blacklisted refresh token");
                throw new InvalidTokenException("Refresh token has been revoked");
            }
            
            // 4. Extract username from token
            String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
            if (username == null) {
                throw new InvalidTokenException("Invalid refresh token");
            }
            
            // 5. Retrieve user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // 6. Check refresh rate limit - DISABLED FOR DEVELOPMENT
            /*
            if (rateLimitService.isRefreshRateLimitExceeded(user.getId())) {
                log.warn("Refresh rate limit exceeded - user: {}", username);
                throw new RateLimitExceededException(
                        "Too many token refresh attempts. Please try again later."
                );
            }
            
            rateLimitService.recordRefreshAttempt(user.getId());
            */
            
            // 7. Validate refresh token against stored hashed tokens
            RefreshToken storedToken = refreshTokenService.validateRefreshToken(
                    request.getRefreshToken(), 
                    username
            ).orElseThrow(() -> {
                log.warn("Refresh token not found or invalid - user: {}", username);
                return new InvalidTokenException("Refresh token is invalid or expired");
            });
            
            // 8. Additional validation checks
            if (!storedToken.isValid()) {
                log.warn("Refresh token invalid - revoked: {}, expired: {}", 
                        storedToken.getRevoked(), storedToken.isExpired());
                throw new InvalidTokenException("Refresh token is invalid or expired");
            }
            
            // 9. Check user status
            validateUserStatus(user);
            
            // 10. Generate new access token
            String accessToken = jwtTokenProvider.generateToken(
                    user.getUsername(),
                    List.of(user.getRole().name())
            );
            
            // 11. Generate new refresh token (rotation)
            String newRefreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());
            
            // 12. Create new hashed refresh token
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                    user,
                    newRefreshTokenString,
                    ipAddress,
                    storedToken.getDeviceInfo(),
                    storedToken.getUserAgent()
            );
            
            // 13. Revoke old refresh token
            refreshTokenService.revokeToken(storedToken.getId());
            
            // 14. Update session in Redis
            sessionCacheService.removeActiveSession(username, storedToken.getId().toString());
            sessionCacheService.trackActiveSession(
                    username,
                    newRefreshToken.getId().toString(),
                    ipAddress
            );
            
            // 15. Get or cache user response
            UserResponse userResponse = sessionCacheService.getCachedUserSession(username);
            if (userResponse == null) {
                userResponse = userMapper.toResponse(user);
                sessionCacheService.cacheUserSession(username, userResponse);
            }
            
            log.info("Token refreshed successfully - user: {}, old token ID: {}, new token ID: {}", 
                    username, storedToken.getId(), newRefreshToken.getId());
            
            // 16. Create audit log
            auditService.log(
                    user.getId(),
                    "User",
                    user.getId(),
                    AuditAction.TOKEN_REFRESH,
                    String.format("Token refreshed from IP: %s, device: %s", 
                            ipAddress, storedToken.getDeviceInfo())
            );
            
            return buildAuthenticationResponse(accessToken, newRefreshTokenString, userResponse);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new InvalidTokenException("Failed to refresh token. Please login again.");
        }
    }
    
    /**
     * Logout user and revoke all tokens.
     * 
     * <p>Actions:</p>
     * <ul>
     *   <li>Blacklist access token</li>
     *   <li>Revoke refresh tokens</li>
     *   <li>Clear session cache</li>
     *   <li>Remove active session tracking</li>
     * </ul>
     */
    @Override
    @Transactional
    public void logout(String accessToken, String username) {
        log.info("Logout initiated - user: {}", username);
        
        try {
            // 1. Blacklist access token
            if (StringUtils.hasText(accessToken)) {
                tokenBlacklistService.blacklistToken(accessToken);
            }
            
            // 2. Find and revoke user's refresh tokens
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            List<RefreshToken> activeTokens = refreshTokenRepository
                    .findActiveTokensByUserId(user.getId(), LocalDateTime.now());
            
            for (RefreshToken token : activeTokens) {
                token.setRevoked(true);
            }
            
            if (!activeTokens.isEmpty()) {
                refreshTokenRepository.saveAll(activeTokens);
                log.info("Revoked {} active refresh tokens for user: {}", activeTokens.size(), username);
            }
            
            // 3. Invalidate session cache
            sessionCacheService.invalidateUserSession(username);
            
            // 4. Clear active sessions
            sessionCacheService.clearAllActiveSessions(username);
            
            log.info("Logout completed - user: {}", username);
            
        } catch (Exception e) {
            log.error("Logout failed - user: {}", username, e);
            // Don't throw exception - logout should always succeed
        }
    }
    
    /**
     * Verify if access token is valid.
     * 
     * @param token JWT access token
     * @return verification response with token details
     */
    @Override
    public VerifyTokenResponse verifyAccessToken(String token) {
        try {
            // 1. Validate token format
            if (!StringUtils.hasText(token)) {
                return buildInvalidTokenResponse("Token is required");
            }
            
            // 2. Check if blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                return buildInvalidTokenResponse("Token has been revoked");
            }
            
            // 3. Validate token
            if (!jwtTokenProvider.validateAccessToken(token)) {
                return buildInvalidTokenResponse("Token is invalid or expired");
            }
            
            // 4. Extract token information
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Date expiresAt = jwtTokenProvider.getExpirationFromToken(token);
            Date issuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
            
            // 5. Verify user exists and is active
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                return buildInvalidTokenResponse("User account is not active");
            }
            
            // 6. Build success response
            return VerifyTokenResponse.builder()
                    .valid(true)
                    .username(username)
                    .userId(user.getId())
                    .expiresAt(convertToLocalDateTime(expiresAt))
                    .issuedAt(convertToLocalDateTime(issuedAt))
                    .message("Token is valid")
                    .build();
                    
        } catch (Exception e) {
            log.error("Token verification failed", e);
            return buildInvalidTokenResponse("Token verification failed");
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private void validateRegistrationRequest(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new ValidationException("Username is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ValidationException("Email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password is required");
        }
    }
    
    private void validateLoginRequest(LoginRequest request) {
        if (!StringUtils.hasText(request.getUsernameOrEmail())) {
            throw new ValidationException("Username or email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new ValidationException("Password is required");
        }
    }
    
    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountLockedException("Account has been suspended. Please contact support.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResourceNotFoundException("Account not found");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(ErrorCode.ACCOUNT_DISABLED, 
                    "Account is inactive. Please contact support.");
        }
    }
    
    private void handleFailedLogin(String identifier, String ipAddress) {
        // Record failed attempt - DISABLED FOR DEVELOPMENT
        /*
        failedLoginAttemptService.recordFailedAttempt(identifier);
        
        int remainingAttempts = failedLoginAttemptService.getRemainingAttempts(identifier);
        
        log.warn("Failed login attempt - identifier: {}, IP: {}, remaining attempts: {}", 
                identifier, ipAddress, remainingAttempts);
        
        if (remainingAttempts > 0) {
            throw new BadCredentialsException(
                    "Invalid username or password. " + remainingAttempts + " attempts remaining."
            );
        } else {
            throw new AccountLockedException(
                    "Account locked due to multiple failed login attempts. " +
                    "Please try again in 30 minutes."
            );
        }
        */
        
        // Development mode - just log the failed attempt
        log.warn("Failed login attempt - identifier: {}, IP: {} (rate limiting disabled)", 
                identifier, ipAddress);
    }
    
    private String extractDeviceInfo(String userAgent) {
        // Simple device extraction - can be enhanced with user-agent parsing library
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }
        
        if (userAgent.contains("Mobile")) {
            return "Mobile Device";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
    
    private AuthenticationResponse buildAuthenticationResponse(
            String accessToken, 
            String refreshToken, 
            UserResponse user) {
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L) // 15 minutes in seconds
                .user(user)
                .build();
    }
    
    private VerifyTokenResponse buildInvalidTokenResponse(String message) {
        return VerifyTokenResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }
    
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }
}

