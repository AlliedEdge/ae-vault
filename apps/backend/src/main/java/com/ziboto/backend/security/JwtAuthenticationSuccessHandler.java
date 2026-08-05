package com.ziboto.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziboto.backend.auth.dto.AuthenticationResponse;
import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.user.dto.UserResponse;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.mapper.UserMapper;
import com.ziboto.backend.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * JWT Authentication Success Handler.
 * 
 * <p>Handles successful authentication by generating JWT tokens and returning
 * them in a JSON response. This handler is typically used with form-based login
 * or other authentication mechanisms that integrate with Spring Security.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Generates access and refresh tokens upon successful authentication</li>
 *   <li>Returns JSON response with tokens and user information</li>
 *   <li>Logs successful authentication events</li>
 *   <li>Can be customized for different authentication flows</li>
 * </ul>
 * 
 * <p><b>Note:</b> For the current implementation using controller-based authentication,
 * this handler is optional. It's provided for flexibility if you want to add
 * additional authentication mechanisms (e.g., OAuth2, SAML) in the future.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    /**
     * Handle successful authentication.
     * 
     * <p>Process:</p>
     * <ol>
     *   <li>Extract authenticated user information</li>
     *   <li>Generate access token (15 minutes)</li>
     *   <li>Generate refresh token (7 days)</li>
     *   <li>Load full user details from database</li>
     *   <li>Return JSON response with tokens and user data</li>
     * </ol>
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param authentication successful authentication object
     * @throws IOException if I/O error occurs
     * @throws ServletException if servlet error occurs
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        log.info("Authentication successful for user: {}", authentication.getName());
        
        try {
            // Extract user details from authentication
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            
            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            
            // Load full user details from database
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found after authentication"));
            
            UserResponse userResponse = userMapper.toResponse(user);
            
            // Build authentication response
            AuthenticationResponse authResponse = AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900L) // 15 minutes in seconds
                    .user(userResponse)
                    .build();
            
            // Create API response
            ApiResponse<AuthenticationResponse> apiResponse = ApiResponse.success(
                    "Authentication successful",
                    authResponse
            );
            
            // Set response properties
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);
            
            // Write JSON response
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
            
            log.info("JWT tokens generated and returned for user: {}", username);
            
        } catch (Exception e) {
            log.error("Error handling authentication success", e);
            
            // Return error response
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            ApiResponse<Object> errorResponse = ApiResponse.error(
                    "Authentication succeeded but failed to generate tokens",
                    null
            );
            
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        }
    }
}
