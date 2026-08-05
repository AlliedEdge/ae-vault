package com.ziboto.backend.auth.controller;

import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.auth.dto.LoginRequest;
import com.ziboto.backend.auth.dto.RefreshTokenRequest;
import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.auth.dto.VerifyTokenResponse;
import com.ziboto.backend.auth.service.AuthService;
import com.ziboto.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST controller.
 * 
 * <p>Provides endpoints for user authentication operations:</p>
 * <ul>
 *   <li>POST /api/v1/auth/register - Register new user</li>
 *   <li>POST /api/v1/auth/login - Login with credentials</li>
 *   <li>POST /api/v1/auth/logout - Logout and revoke tokens</li>
 *   <li>POST /api/v1/auth/refresh - Refresh access token</li>
 *   <li>GET /api/v1/auth/verify - Verify token validity</li>
 * </ul>
 * 
 * <p>All business logic is delegated to AuthService.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Register a new user account.
     * 
     * @param request registration details including username, email, and password
     * @param httpRequest HTTP request for extracting client IP address
     * @return authentication response with JWT tokens and user information
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Create a new user account with the provided credentials. Returns JWT tokens upon successful registration."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User successfully registered",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid registration data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Username or email already exists"
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = extractClientIpAddress(httpRequest);
        log.info("Registration request received for username: {} from IP: {}", request.getUsername(), ipAddress);
        
        AuthenticationResponse response = authService.register(request, ipAddress);
        
        log.info("User registered successfully: {}", request.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }
    
    /**
     * Authenticate user with credentials.
     * 
     * @param request login credentials (username/email and password)
     * @param httpRequest HTTP request for extracting client IP address
     * @return authentication response with JWT tokens and user information
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with username/email and password. Returns JWT tokens upon successful authentication."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "429",
            description = "Too many login attempts"
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = extractClientIpAddress(httpRequest);
        log.info("Login request received for user: {} from IP: {}", request.getUsernameOrEmail(), ipAddress);
        
        AuthenticationResponse response = authService.login(request, ipAddress);
        
        log.info("User logged in successfully: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    /**
     * Refresh access token using refresh token.
     * 
     * @param request refresh token request
     * @param httpRequest HTTP request for extracting client IP address
     * @return new authentication response with fresh JWT tokens
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Obtain a new access token using a valid refresh token. The old refresh token is invalidated and a new one is issued."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        )
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = extractClientIpAddress(httpRequest);
        log.info("Token refresh request received from IP: {}", ipAddress);
        
        AuthenticationResponse response = authService.refreshToken(request, ipAddress);
        
        log.info("Token refreshed successfully for user");
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
    
    /**
     * Logout user and revoke tokens.
     * 
     * @param authorizationHeader Authorization header containing JWT token (optional)
     * @return success response
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logout the authenticated user and revoke their tokens. The access token is blacklisted and refresh tokens are invalidated."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logout successful"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Bearer token in format: Bearer <token>", required = false)
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        
        try {
            // If authorization header is present, extract token and perform full logout
            if (StringUtils.hasText(authorizationHeader)) {
                String accessToken = extractTokenFromHeader(authorizationHeader);
                
                // Get username from security context
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() 
                        && !"anonymousUser".equals(authentication.getName())) {
                    String username = authentication.getName();
                    log.info("Logout request received for user: {}", username);
                    authService.logout(accessToken, username);
                    log.info("User logged out successfully: {}", username);
                } else {
                    log.debug("Logout request with token but no authenticated user");
                }
            } else {
                log.debug("Logout request without authorization header - client-side logout only");
            }
            
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        } catch (Exception e) {
            log.error("Error during logout", e);
            // Still return success to allow client-side logout
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        }
    }
    
    /**
     * Verify JWT access token validity.
     * 
     * @param authorizationHeader Authorization header containing JWT token
     * @return token verification response with token details
     */
    @GetMapping("/verify")
    @Operation(
        summary = "Verify token",
        description = "Verify the validity of a JWT access token. Returns token details if valid."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token verified",
            content = @Content(schema = @Schema(implementation = VerifyTokenResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired token"
        )
    })
    public ResponseEntity<ApiResponse<VerifyTokenResponse>> verifyToken(
            @Parameter(description = "Bearer token in format: Bearer <token>", required = true)
            @RequestHeader("Authorization") String authorizationHeader) {
        
        String accessToken = extractTokenFromHeader(authorizationHeader);
        
        log.debug("Token verification request received");
        
        VerifyTokenResponse response = authService.verifyAccessToken(accessToken);
        
        if (response.getValid()) {
            log.debug("Token verified successfully for user: {}", response.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
        } else {
            log.debug("Token verification failed: {}", response.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(response.getMessage(), response));
        }
    }
    
    /**
     * Extract JWT token from Authorization header.
     * 
     * @param authorizationHeader Authorization header value
     * @return JWT token string without Bearer prefix
     * @throws IllegalArgumentException if header format is invalid
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new IllegalArgumentException("Authorization header is missing");
        }
        
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        
        throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
    }
    
    /**
     * Extract client IP address from HTTP request.
     * 
     * <p>Checks multiple headers in order:</p>
     * <ol>
     *   <li>X-Forwarded-For (proxy/load balancer)</li>
     *   <li>X-Real-IP (nginx proxy)</li>
     *   <li>Remote address from request</li>
     * </ol>
     * 
     * @param request HTTP servlet request
     * @return client IP address
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (!StringUtils.hasText(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (!StringUtils.hasText(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (StringUtils.hasText(ipAddress) && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return StringUtils.hasText(ipAddress) ? ipAddress : "unknown";
    }
}
