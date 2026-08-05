package com.ziboto.backend.auth.service;

import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.LoginRequest;
import com.ziboto.backend.auth.dto.RefreshTokenRequest;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.dto.VerifyTokenResponse;

/**
 * Authentication service interface.
 * 
 * <p>Provides core authentication operations:</p>
 * <ul>
 *   <li>User registration</li>
 *   <li>User login with credentials</li>
 *   <li>Token refresh</li>
 *   <li>Logout (token revocation)</li>
 *   <li>Token verification</li>
 * </ul>
 */
public interface AuthService {
    
    /**
     * Register a new user.
     * 
     * @param request registration request with user details
     * @param ipAddress client IP address for tracking
     * @return authentication response with tokens and user info
     */
    AuthenticationResponse register(RegisterRequest request, String ipAddress);
    
    /**
     * Authenticate user with credentials.
     * 
     * @param request login request with username/email and password
     * @param ipAddress client IP address for security tracking
     * @return authentication response with tokens and user info
     */
    AuthenticationResponse login(LoginRequest request, String ipAddress);
    
    /**
     * Refresh access token using refresh token.
     * 
     * @param request refresh token request
     * @param ipAddress client IP address
     * @return new authentication response with fresh tokens
     */
    AuthenticationResponse refreshToken(RefreshTokenRequest request, String ipAddress);
    
    /**
     * Logout user and revoke tokens.
     * 
     * @param accessToken JWT access token to revoke
     * @param username username of the user logging out
     */
    void logout(String accessToken, String username);
    
    /**
     * Verify if access token is valid.
     * 
     * @param token JWT access token to verify
     * @return verification response with token details
     */
    VerifyTokenResponse verifyAccessToken(String token);
}
