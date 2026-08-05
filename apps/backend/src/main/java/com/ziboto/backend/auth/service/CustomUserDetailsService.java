package com.ziboto.backend.auth.service;

import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * 
 * <p>Loads user details from the database for authentication and authorization.
 * Supports both username and email-based authentication.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Loads user by username or email</li>
 *   <li>Maps user roles to Spring Security authorities</li>
 *   <li>Enforces account status checks (active, suspended, deleted)</li>
 *   <li>Provides detailed UserDetails for authentication</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Load user by username or email for authentication.
     * 
     * <p>This method is called by Spring Security during authentication.
     * It accepts either username or email and returns UserDetails.</p>
     * 
     * @param usernameOrEmail username or email address
     * @return UserDetails object for authentication
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username or email: {}", usernameOrEmail);
        
        // Find user by username or email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.error("User not found with username or email: {}", usernameOrEmail);
                    return new UsernameNotFoundException(
                            "User not found with username or email: " + usernameOrEmail
                    );
                });
        
        log.debug("User found: {} with role: {}", user.getUsername(), user.getRole());
        
        // Build and return UserDetails
        return buildUserDetails(user);
    }
    
    /**
     * Load user by ID for token-based authentication.
     * Useful for refresh token scenarios.
     * 
     * @param userId user ID
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        log.debug("Loading user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });
        
        return buildUserDetails(user);
    }
    
    /**
     * Build Spring Security UserDetails from User entity.
     * 
     * @param user User entity
     * @return UserDetails implementation
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(getAuthorities(user))
                .accountExpired(false)
                .accountLocked(isAccountLocked(user))
                .credentialsExpired(false)
                .disabled(!isAccountEnabled(user))
                .build();
    }
    
    /**
     * Map user role to Spring Security authorities.
     * 
     * @param user User entity
     * @return collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Convert user role to Spring Security authority
        // Role name should be prefixed with "ROLE_" for Spring Security
        return Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name())
        );
    }
    
    /**
     * Check if user account is enabled.
     * Account is enabled if status is ACTIVE.
     * 
     * @param user User entity
     * @return true if account is enabled
     */
    private boolean isAccountEnabled(User user) {
        return user.getStatus() == UserStatus.ACTIVE;
    }
    
    /**
     * Check if user account is locked.
     * Account is locked if status is SUSPENDED.
     * 
     * @param user User entity
     * @return true if account is locked
     */
    private boolean isAccountLocked(User user) {
        return user.getStatus() == UserStatus.SUSPENDED;
    }
}
