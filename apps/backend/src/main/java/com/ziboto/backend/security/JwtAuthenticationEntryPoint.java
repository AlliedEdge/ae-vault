package com.ziboto.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziboto.backend.common.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT Authentication Entry Point.
 * 
 * <p>Handles authentication failures and returns appropriate JSON error responses.
 * This is invoked when a user tries to access a protected resource without valid authentication.</p>
 * 
 * <p>Common scenarios:</p>
 * <ul>
 *   <li>No JWT token provided</li>
 *   <li>Invalid JWT token signature</li>
 *   <li>Expired JWT token</li>
 *   <li>Malformed JWT token</li>
 *   <li>Invalid credentials</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Commence authentication process when authentication fails.
     * Returns a JSON error response instead of redirecting to login page.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param authException authentication exception
     * @throws IOException if I/O error occurs
     * @throws ServletException if servlet error occurs
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        log.error("Unauthorized error: {} - Path: {}", 
                authException.getMessage(), 
                request.getRequestURI());
        
        // Determine the specific error type
        String errorMessage = determineErrorMessage(request, authException);
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        
        // Set response properties
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        
        // Create error response
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(false)
                .message(errorMessage)
                .data(null)
                .build();
        
        // Write JSON response
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
    
    /**
     * Determine appropriate error message based on exception type.
     * 
     * @param request HTTP request
     * @param authException authentication exception
     * @return error message
     */
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        // Check if there's a specific JWT exception stored in request attributes
        Object jwtException = request.getAttribute("jwtException");
        
        if (jwtException instanceof ExpiredJwtException) {
            return "JWT token has expired. Please refresh your token or login again.";
        } else if (jwtException instanceof SignatureException) {
            return "Invalid JWT signature. Token may have been tampered with.";
        } else if (jwtException instanceof MalformedJwtException) {
            return "Malformed JWT token. Please provide a valid token.";
        } else if (authException instanceof BadCredentialsException) {
            return "Invalid credentials. Please check your username and password.";
        } else if (authException instanceof InsufficientAuthenticationException) {
            return "Authentication required. Please login to access this resource.";
        }
        
        // Default message
        String message = authException.getMessage();
        if (message != null && !message.isEmpty()) {
            return "Unauthorized: " + message;
        }
        
        return "Full authentication is required to access this resource.";
    }
}

