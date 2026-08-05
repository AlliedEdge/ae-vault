package com.ziboto.backend.security;

import com.ziboto.backend.auth.service.TokenBlacklistService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 * 
 * <p>Intercepts HTTP requests and validates JWT tokens from the Authorization header.
 * If a valid token is present, authenticates the user in Spring Security context.</p>
 * 
 * <p>Process:</p>
 * <ol>
 *   <li>Extract JWT from Authorization header (Bearer token)</li>
 *   <li>Validate token signature and expiration</li>
 *   <li>Check if token is blacklisted (revoked)</li>
 *   <li>Extract username from token</li>
 *   <li>Load user details from database</li>
 *   <li>Populate SecurityContext with authentication</li>
 * </ol>
 * 
 * <p>Security Features:</p>
 * <ul>
 *   <li><b>Bearer Authentication</b> - Standard OAuth 2.0 Bearer token scheme</li>
 *   <li><b>Token Validation</b> - Signature and expiration checks</li>
 *   <li><b>Token Blacklisting</b> - Revoked tokens are rejected</li>
 *   <li><b>User Extraction</b> - Username extracted from JWT claims</li>
 *   <li><b>SecurityContext Population</b> - User authenticated in Spring Security</li>
 *   <li><b>Exception Handling</b> - Detailed error logging and recovery</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    
    /**
     * Filter incoming requests and authenticate via JWT token.
     * 
     * <p>Authentication Flow:</p>
     * <ol>
     *   <li>Extract JWT from Authorization header</li>
     *   <li>Validate token (signature, expiration, type)</li>
     *   <li>Check token blacklist (logout/revoked)</li>
     *   <li>Extract username from token claims</li>
     *   <li>Load user from database via UserDetailsService</li>
     *   <li>Create authentication object with user details and authorities</li>
     *   <li>Populate SecurityContext for the request</li>
     * </ol>
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Step 1: Extract JWT from Authorization header
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                // Step 2: Validate token (signature and expiration)
                if (tokenProvider.validateAccessToken(jwt)) {
                    
                    // Step 3: Check if token is blacklisted (revoked after logout)
                    if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                        log.debug("Token is blacklisted (revoked), rejecting request");
                        request.setAttribute("jwtException", new SignatureException("Token has been revoked"));
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Step 4: Extract username from token
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    
                    // Only authenticate if not already authenticated
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        try {
                            // Step 5: Load user details from database
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            
                            // Step 6: Create authentication token with user details and authorities
                            UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,  // Credentials (not needed for JWT)
                                            userDetails.getAuthorities()
                                    );
                            
                            // Set additional details (IP address, session ID, etc.)
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );
                            
                            // Step 7: Populate SecurityContext with authentication
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("Authenticated user '{}' from JWT token, authorities: {}", 
                                    username, userDetails.getAuthorities());
                            
                        } catch (UsernameNotFoundException ex) {
                            log.error("User not found: {} - token may be stale", username);
                            request.setAttribute("jwtException", ex);
                        }
                    }
                } else {
                    log.debug("Invalid or expired JWT token");
                }
            } else {
                log.trace("No JWT token found in request");
            }
            
        } catch (ExpiredJwtException ex) {
            log.debug("JWT token expired: {}", ex.getMessage());
            request.setAttribute("jwtException", ex);
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
            request.setAttribute("jwtException", ex);
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT token: {}", ex.getMessage());
            request.setAttribute("jwtException", ex);
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            request.setAttribute("jwtException", ex);
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     * 
     * <p>Supports Bearer token authentication scheme:</p>
     * <ul>
     *   <li>Header format: "Authorization: Bearer {token}"</li>
     *   <li>Token is extracted by removing "Bearer " prefix</li>
     *   <li>Returns null if no token or invalid format</li>
     * </ul>
     * 
     * @param request HTTP request
     * @return JWT token string or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.trace("JWT token extracted from Authorization header");
            return token;
        }
        
        return null;
    }
    
    /**
     * Determine if this filter should be skipped for the current request.
     * 
     * <p>Public endpoints that don't require authentication:</p>
     * <ul>
     *   <li>/api/v1/auth/login - Login endpoint</li>
     *   <li>/api/v1/auth/register - Registration endpoint</li>
     *   <li>/api/v1/auth/refresh - Token refresh endpoint</li>
     *   <li>/actuator/** - Spring Boot Actuator</li>
     *   <li>/swagger-ui/** - Swagger UI</li>
     *   <li>/api-docs/** - OpenAPI documentation</li>
     *   <li>/error - Error page</li>
     * </ul>
     * 
     * @param request HTTP request
     * @return true to skip filter (public endpoint), false to apply filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT validation for public endpoints
        boolean shouldSkip = path.startsWith("/api/v1/auth/login") ||
                             path.startsWith("/api/v1/auth/register") ||
                             path.startsWith("/api/v1/auth/refresh") ||
                             path.startsWith("/actuator") ||
                             path.startsWith("/swagger-ui") ||
                             path.startsWith("/api-docs") ||
                             path.startsWith("/v3/api-docs") ||
                             path.equals("/error");
        
        if (shouldSkip) {
            log.trace("Skipping JWT filter for public endpoint: {}", path);
        }
        
        return shouldSkip;
    }
}
