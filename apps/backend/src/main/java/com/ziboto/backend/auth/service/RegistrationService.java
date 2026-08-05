package com.ziboto.backend.auth.service;

import com.ziboto.backend.auth.dto.RegisterRequest;
import com.ziboto.backend.user.dto.UserResponse;

/**
 * Service interface for user registration operations.
 */
public interface RegistrationService {
    
    /**
     * Registers a new user in the system.
     *
     * @param request the registration request containing user details
     * @return UserResponse containing the created user information
     * @throws com.ziboto.backend.exception.ConflictException if email or username already exists
     */
    UserResponse register(RegisterRequest request);
}
