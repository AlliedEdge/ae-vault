package com.ziboto.backend.common.util;

import com.ziboto.backend.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class for Spring Security operations.
 * 
 * <p>Provides safe extraction of authenticated user information
 * from SecurityContext without accepting user input.</p>
 */
@Slf4j
public final class SecurityUtils {
    
    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Get the username of the currently authenticated user.
     * 
     * @return username from SecurityContext
     * @throws UnauthorizedException if no authentication found or user is not authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in SecurityContext");
            throw new UnauthorizedException("User is not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        log.error("Unknown principal type in SecurityContext: {}", principal.getClass().getName());
        throw new UnauthorizedException("Unable to extract username from SecurityContext");
    }
    
    /**
     * Get the Authentication object from SecurityContext.
     * 
     * @return current Authentication
     * @throws UnauthorizedException if no authentication found
     */
    public static Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in SecurityContext");
            throw new UnauthorizedException("User is not authenticated");
        }
        
        return authentication;
    }
    
    /**
     * Check if the current user has a specific role.
     * 
     * @param role role to check (e.g., "ROLE_ADMIN")
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        try {
            Authentication authentication = getCurrentAuthentication();
            return authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
        } catch (UnauthorizedException e) {
            return false;
        }
    }
    
    /**
     * Check if there is an authenticated user in the SecurityContext.
     * 
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
