package com.ziboto.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Utility class for security-related operations.
 * 
 * <p>Provides helper methods to access security context and user information.</p>
 */
@Slf4j
public final class SecurityUtils {
    
    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Get the current authenticated user's authentication object.
     * 
     * @return Optional containing Authentication if user is authenticated
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of(authentication);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the current authenticated user's username.
     * 
     * @return Optional containing username if user is authenticated
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication()
                .map(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof UserDetails) {
                        return ((UserDetails) principal).getUsername();
                    } else if (principal instanceof String) {
                        return (String) principal;
                    }
                    return null;
                });
    }
    
    /**
     * Get the current authenticated user's UserDetails.
     * 
     * @return Optional containing UserDetails if user is authenticated
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        return getCurrentAuthentication()
                .map(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof UserDetails) {
                        return (UserDetails) principal;
                    }
                    return null;
                });
    }
    
    /**
     * Check if current user is authenticated.
     * 
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentAuthentication().isPresent();
    }
    
    /**
     * Check if current user has a specific role.
     * 
     * @param role role name (with or without "ROLE_" prefix)
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        String roleToCheck = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals(roleToCheck)))
                .orElse(false);
    }
    
    /**
     * Check if current user has any of the specified roles.
     * 
     * @param roles role names (with or without "ROLE_" prefix)
     * @return true if user has any of the roles
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if current user has all of the specified roles.
     * 
     * @param roles role names (with or without "ROLE_" prefix)
     * @return true if user has all of the roles
     */
    public static boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Log current security context for debugging.
     */
    public static void logSecurityContext() {
        getCurrentAuthentication().ifPresentOrElse(
                auth -> {
                    log.debug("Current authentication: {}", auth.getName());
                    log.debug("Authorities: {}", auth.getAuthorities());
                    log.debug("Is authenticated: {}", auth.isAuthenticated());
                },
                () -> log.debug("No authentication in context")
        );
    }
}
