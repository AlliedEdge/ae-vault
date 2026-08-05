package com.ziboto.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security Headers Filter for production-grade security.
 * 
 * <p>Adds essential HTTP security headers to all responses:</p>
 * <ul>
 *   <li><b>X-Content-Type-Options: nosniff</b> - Prevents MIME type sniffing</li>
 *   <li><b>X-Frame-Options: DENY</b> - Prevents clickjacking attacks</li>
 *   <li><b>X-XSS-Protection: 1; mode=block</b> - Enables XSS filter in legacy browsers</li>
 *   <li><b>Strict-Transport-Security</b> - Enforces HTTPS (production only)</li>
 *   <li><b>Content-Security-Policy</b> - Restricts resource loading</li>
 *   <li><b>Referrer-Policy: strict-origin-when-cross-origin</b> - Controls referrer information</li>
 *   <li><b>Permissions-Policy</b> - Controls browser features</li>
 *   <li><b>Cache-Control</b> - Controls caching of sensitive responses</li>
 * </ul>
 * 
 * <h2>Security Benefits:</h2>
 * <ul>
 *   <li>Protection against clickjacking attacks</li>
 *   <li>Prevention of MIME type confusion attacks</li>
 *   <li>XSS protection in older browsers</li>
 *   <li>Enforcement of HTTPS connections</li>
 *   <li>Content Security Policy to prevent XSS and injection attacks</li>
 *   <li>Controlled browser feature access</li>
 *   <li>Privacy protection through referrer policy</li>
 * </ul>
 * 
 * <p>Headers are applied to all HTTP responses to ensure comprehensive protection.</p>
 */
@Slf4j
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    // Enable/disable HSTS based on environment (should be true in production)
    private static final boolean ENABLE_HSTS = isProductionEnvironment();
    
    /**
     * Apply security headers to all HTTP responses.
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
        
        // 1. X-Content-Type-Options: Prevents MIME type sniffing
        // Browsers will not try to detect content type, only use declared Content-Type
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // 2. X-Frame-Options: Prevents clickjacking by disallowing framing
        // DENY = page cannot be displayed in a frame, even from same origin
        // Alternative: SAMEORIGIN (allows framing from same origin)
        response.setHeader("X-Frame-Options", "DENY");
        
        // 3. X-XSS-Protection: Legacy XSS filter for older browsers
        // Modern browsers rely on CSP, but this helps legacy browsers
        // 1; mode=block = enable filter and block page if XSS detected
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // 4. Strict-Transport-Security (HSTS): Enforce HTTPS
        // Only enable in production with valid SSL certificate
        // max-age=31536000 = 1 year, includeSubDomains = apply to all subdomains
        // preload = allow inclusion in browser HSTS preload list
        if (ENABLE_HSTS && request.isSecure()) {
            response.setHeader(
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload"
            );
        }
        
        // 5. Content-Security-Policy (CSP): Restrict resource loading
        // This is a strict policy - adjust based on your frontend needs
        String csp = buildContentSecurityPolicy();
        response.setHeader("Content-Security-Policy", csp);
        
        // 6. Referrer-Policy: Control referrer information leakage
        // strict-origin-when-cross-origin = send full URL for same-origin,
        // only origin for cross-origin, nothing for insecure destinations
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // 7. Permissions-Policy: Control browser features
        // Restrict access to sensitive browser APIs
        String permissionsPolicy = buildPermissionsPolicy();
        response.setHeader("Permissions-Policy", permissionsPolicy);
        
        // 8. Cache-Control for sensitive endpoints
        // Prevent caching of authentication and sensitive data
        if (isSensitiveEndpoint(request)) {
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        // 9. Remove Server header to avoid information disclosure
        // Note: This may not work for all servlet containers
        response.setHeader("Server", "");
        
        log.trace("Applied security headers to response for: {}", request.getRequestURI());
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Build Content Security Policy header value.
     * 
     * <p>CSP Directives:</p>
     * <ul>
     *   <li><b>default-src 'self'</b> - Only allow resources from same origin by default</li>
     *   <li><b>script-src 'self'</b> - Only allow scripts from same origin</li>
     *   <li><b>style-src 'self' 'unsafe-inline'</b> - Allow same-origin and inline styles</li>
     *   <li><b>img-src 'self' data: https:</b> - Allow images from same origin, data URIs, and HTTPS</li>
     *   <li><b>font-src 'self'</b> - Only allow fonts from same origin</li>
     *   <li><b>connect-src 'self'</b> - Only allow AJAX/fetch to same origin</li>
     *   <li><b>frame-ancestors 'none'</b> - Prevent framing (modern alternative to X-Frame-Options)</li>
     *   <li><b>base-uri 'self'</b> - Restrict base tag to same origin</li>
     *   <li><b>form-action 'self'</b> - Only allow form submissions to same origin</li>
     *   <li><b>upgrade-insecure-requests</b> - Automatically upgrade HTTP to HTTPS</li>
     * </ul>
     * 
     * <p><b>Note:</b> Adjust this policy based on your frontend framework requirements.
     * React/Vue/Angular may need 'unsafe-inline' or 'unsafe-eval' for development.</p>
     * 
     * @return Content Security Policy header value
     */
    private String buildContentSecurityPolicy() {
        return String.join("; ",
            "default-src 'self'",                          // Default policy
            "script-src 'self'",                           // Scripts from same origin only
            "style-src 'self' 'unsafe-inline'",            // Styles (unsafe-inline needed for some frameworks)
            "img-src 'self' data: https:",                 // Images from self, data URIs, and HTTPS
            "font-src 'self' data:",                       // Fonts from same origin and data URIs
            "connect-src 'self'",                          // AJAX/fetch to same origin only
            "media-src 'self'",                            // Audio/video from same origin
            "object-src 'none'",                           // Disallow plugins (Flash, Java, etc.)
            "frame-ancestors 'none'",                      // Prevent framing (modern X-Frame-Options)
            "base-uri 'self'",                             // Restrict <base> tag
            "form-action 'self'",                          // Forms can only submit to same origin
            "upgrade-insecure-requests"                     // Auto-upgrade HTTP to HTTPS
        );
    }
    
    /**
     * Build Permissions Policy header value.
     * 
     * <p>Controls access to browser features and APIs:</p>
     * <ul>
     *   <li><b>geolocation=()</b> - Disable geolocation API</li>
     *   <li><b>microphone=()</b> - Disable microphone access</li>
     *   <li><b>camera=()</b> - Disable camera access</li>
     *   <li><b>payment=()</b> - Disable payment request API</li>
     *   <li><b>usb=()</b> - Disable USB API</li>
     *   <li><b>magnetometer=()</b> - Disable magnetometer</li>
     *   <li><b>accelerometer=()</b> - Disable accelerometer</li>
     *   <li><b>gyroscope=()</b> - Disable gyroscope</li>
     * </ul>
     * 
     * <p>Use <code>(self)</code> instead of <code>()</code> to allow access from same origin.</p>
     * 
     * @return Permissions Policy header value
     */
    private String buildPermissionsPolicy() {
        return String.join(", ",
            "geolocation=()",           // Disable geolocation
            "microphone=()",            // Disable microphone
            "camera=()",                // Disable camera
            "payment=()",               // Disable payment API
            "usb=()",                   // Disable USB API
            "magnetometer=()",          // Disable magnetometer
            "accelerometer=()",         // Disable accelerometer
            "gyroscope=()",             // Disable gyroscope
            "interest-cohort=()"        // Disable FLoC (privacy)
        );
    }
    
    /**
     * Check if the request is for a sensitive endpoint that should not be cached.
     * 
     * <p>Sensitive endpoints include:</p>
     * <ul>
     *   <li>Authentication endpoints (/api/v1/auth/**)</li>
     *   <li>User profile endpoints (/api/v1/users/me, /api/v1/users/{id})</li>
     *   <li>Any endpoint containing sensitive data</li>
     * </ul>
     * 
     * @param request HTTP request
     * @return true if endpoint is sensitive and should not be cached
     */
    private boolean isSensitiveEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Authentication and user endpoints should never be cached
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/api/v1/users/") ||
               path.contains("/profile") ||
               path.contains("/settings") ||
               path.contains("/password") ||
               path.contains("/token");
    }
    
    /**
     * Determine if running in production environment.
     * HSTS should only be enabled in production with valid SSL certificate.
     * 
     * @return true if production environment
     */
    private static boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active");
        if (profile == null) {
            profile = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        return "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile);
    }
    
    /**
     * Determine if this filter should be skipped for the current request.
     * 
     * <p>Security headers are applied to all requests, but we can skip
     * for static resources if needed for performance.</p>
     * 
     * @param request HTTP request
     * @return true to skip filter, false to apply headers
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Apply headers to all requests by default
        // Can be optimized to skip static resources if needed
        return false;
    }
}
