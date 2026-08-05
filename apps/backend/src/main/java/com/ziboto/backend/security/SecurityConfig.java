package com.ziboto.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security Configuration for Ziboto Backend.
 * 
 * <p>Configures JWT-based stateless authentication with the following features:</p>
 * <ul>
 *   <li><b>Stateless Sessions</b> - No server-side sessions, all state in JWT</li>
 *   <li><b>BCrypt Password Encoding</b> - Secure password hashing</li>
 *   <li><b>CORS Support</b> - Configured cross-origin resource sharing</li>
 *   <li><b>CSRF Disabled</b> - Not needed for stateless JWT authentication</li>
 *   <li><b>Public Endpoints</b> - Login, register, docs, actuator</li>
 *   <li><b>Protected Resources</b> - All other endpoints require authentication</li>
 *   <li><b>JWT Filter</b> - Custom filter for JWT token validation</li>
 *   <li><b>Method Security</b> - Annotation-based authorization (@PreAuthorize, @Secured)</li>
 * </ul>
 * 
 * <h2>Security Flow:</h2>
 * <ol>
 *   <li>Client sends request with JWT in Authorization header</li>
 *   <li>JwtAuthenticationFilter extracts and validates token</li>
 *   <li>UserDetailsService loads user from database</li>
 *   <li>AuthenticationProvider verifies credentials</li>
 *   <li>SecurityContext populated with authentication</li>
 *   <li>Request proceeds to controller</li>
 * </ol>
 * 
 * <h2>Public Endpoints:</h2>
 * <ul>
 *   <li>/api/v1/auth/** - Authentication endpoints (login, register, refresh)</li>
 *   <li>/actuator/** - Spring Boot Actuator endpoints</li>
 *   <li>/api-docs/** - OpenAPI documentation</li>
 *   <li>/swagger-ui/** - Swagger UI</li>
 * </ul>
 * 
 * <h2>Protected Endpoints:</h2>
 * <ul>
 *   <li>/api/v1/** - All other API endpoints (require valid JWT)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,        // Enable @Secured annotation
        jsr250Enabled = true,          // Enable @RolesAllowed annotation
        prePostEnabled = true          // Enable @PreAuthorize, @PostAuthorize
)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    
    /**
     * Configure HTTP security for the application.
     * 
     * <p>Security features:</p>
     * <ul>
     *   <li>CSRF protection disabled (stateless JWT authentication)</li>
     *   <li>CORS enabled with custom configuration</li>
     *   <li>Stateless session management (no server-side sessions)</li>
     *   <li>Public access to authentication endpoints</li>
     *   <li>JWT-based authentication for protected resources</li>
     *   <li>Custom authentication entry point for error handling</li>
     * </ul>
     * 
     * @param http HttpSecurity configuration
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security...");
        
        http
                // Disable CSRF protection (not needed for stateless JWT authentication)
                // CSRF protection is designed for session-based authentication
                .csrf(AbstractHttpConfigurer::disable)
                
                // Enable CORS with custom configuration from CorsConfig
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/v1/auth/**",           // Authentication endpoints
                                "/api/v1/health/**",         // Health check endpoints
                                "/actuator/**",              // Actuator endpoints
                                "/api-docs/**",              // OpenAPI docs
                                "/swagger-ui/**",            // Swagger UI
                                "/swagger-ui.html",          // Swagger UI HTML
                                "/v3/api-docs/**",           // OpenAPI v3 docs
                                "/error"                     // Error endpoint
                        ).permitAll()
                        
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                
                // Configure exception handling
                .exceptionHandling(exception -> exception
                        // Custom entry point for authentication failures
                        // Returns JSON error responses instead of redirecting to login page
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                
                // Configure session management
                .sessionManagement(session -> session
                        // Stateless sessions - no server-side session storage
                        // All authentication state stored in JWT token
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Register custom authentication provider
                .authenticationProvider(authenticationProvider())
                
                // Add security headers filter first (before any authentication)
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Add JWT authentication filter before username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("Spring Security configuration completed");
        return http.build();
    }
    
    /**
     * Configure authentication provider with custom UserDetailsService and password encoder.
     * 
     * <p>The DaoAuthenticationProvider:</p>
     * <ul>
     *   <li>Loads user details from database via UserDetailsService</li>
     *   <li>Verifies password using BCrypt encoder</li>
     *   <li>Checks account status (enabled, locked, expired)</li>
     * </ul>
     * 
     * @return configured DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        log.debug("Configuring authentication provider");
        
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        
        // Set password encoder for password verification
        authProvider.setPasswordEncoder(passwordEncoder());
        
        // Hide user not found exceptions for security (prevents user enumeration)
        authProvider.setHideUserNotFoundExceptions(true);
        
        return authProvider;
    }
    
    /**
     * Expose AuthenticationManager as a Spring bean.
     * 
     * <p>Required for:</p>
     * <ul>
     *   <li>Manual authentication in login endpoint</li>
     *   <li>Programmatic authentication</li>
     *   <li>Testing authentication</li>
     * </ul>
     * 
     * @param config authentication configuration
     * @return AuthenticationManager bean
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.debug("Configuring authentication manager");
        return config.getAuthenticationManager();
    }
    
    /**
     * Configure BCrypt password encoder.
     * 
     * <p>BCrypt features:</p>
     * <ul>
     *   <li>Adaptive hashing (automatically increases strength over time)</li>
     *   <li>Built-in salt generation (unique salt per password)</li>
     *   <li>Configurable strength (default: 10 rounds)</li>
     *   <li>Resistant to rainbow table attacks</li>
     *   <li>Slow by design to prevent brute-force attacks</li>
     * </ul>
     * 
     * <p>Default strength: 10 (2^10 = 1024 rounds)</p>
     * <p>Higher strength = more secure but slower (11 = ~500ms, 12 = ~1s)</p>
     * 
     * @return BCryptPasswordEncoder with default strength
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Configuring BCrypt password encoder with default strength (10)");
        
        // Use default strength (10) for good balance of security and performance
        // For higher security in production, consider strength 12 or 13
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Configure BCrypt password encoder with custom strength.
     * Use this if you need stronger password hashing.
     * 
     * @param strength BCrypt strength (10-31, recommended: 10-13)
     * @return BCryptPasswordEncoder with custom strength
     */
    public static PasswordEncoder passwordEncoder(int strength) {
        if (strength < 10 || strength > 31) {
            throw new IllegalArgumentException(
                    "BCrypt strength must be between 10 and 31. Recommended: 10-13"
            );
        }
        log.info("Configuring BCrypt password encoder with strength: {}", strength);
        return new BCryptPasswordEncoder(strength);
    }
}
