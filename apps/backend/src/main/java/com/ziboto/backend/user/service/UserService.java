package com.ziboto.backend.user.service;

import com.ziboto.backend.user.dto.UpdateProfileRequest;
import com.ziboto.backend.user.dto.UpdateProfileResponse;
import com.ziboto.backend.user.dto.UpdateUserRequest;
import com.ziboto.backend.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for user management operations.
 * 
 * <p>Provides methods for CRUD operations, search, profile updates,
 * and user existence checks.</p>
 */
public interface UserService {
    
    /**
     * Get user by ID.
     * 
     * @param userId user ID
     * @return user response DTO
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    UserResponse getUserById(Long userId);
    
    /**
     * Get user by username.
     * 
     * @param username username
     * @return user response DTO
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    UserResponse getUserByUsername(String username);
    
    /**
     * Get user by email.
     * 
     * @param email email address
     * @return user response DTO
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    UserResponse getUserByEmail(String email);
    
    /**
     * Get all users with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of user responses
     */
    Page<UserResponse> getAllUsers(Pageable pageable);
    
    /**
     * Search users by query.
     * 
     * @param search search query
     * @param pageable pagination parameters
     * @return page of matching user responses
     */
    Page<UserResponse> searchUsers(String search, Pageable pageable);
    
    /**
     * Update user (admin operation).
     * 
     * @param userId user ID
     * @param request update request
     * @return updated user response
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     * @throws com.ziboto.backend.exception.ValidationException if validation fails
     */
    UserResponse updateUser(Long userId, UpdateUserRequest request);
    
    /**
     * Update current user's profile.
     * 
     * @param username username of authenticated user
     * @param request profile update request
     * @return update profile response
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     * @throws com.ziboto.backend.exception.ValidationException if validation fails
     */
    UpdateProfileResponse updateCurrentUserProfile(String username, UpdateProfileRequest request);
    
    /**
     * Get current user profile.
     * 
     * @param username username of authenticated user
     * @return user response DTO
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    UserResponse getCurrentUser(String username);
    
    /**
     * Delete user (soft delete).
     * 
     * @param userId user ID
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    void deleteUser(Long userId);
    
    /**
     * Check if username exists.
     * 
     * @param username username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists.
     * 
     * @param email email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Get storage information for authenticated user.
     * 
     * @param username username of authenticated user
     * @return storage info response
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    com.ziboto.backend.user.dto.StorageInfoResponse getUserStorageInfo(String username);
    
    /**
     * Get all active sessions for authenticated user.
     * 
     * @param username username of authenticated user
     * @return list of session responses
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     */
    java.util.List<com.ziboto.backend.user.dto.SessionResponse> getUserSessions(String username);
    
    /**
     * Revoke a specific session by session ID.
     * 
     * @param username username of authenticated user
     * @param sessionId session ID (refresh token ID)
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if session not found
     * @throws com.ziboto.backend.exception.UnauthorizedException if session doesn't belong to user
     */
    void revokeSession(String username, String sessionId);
    
    /**
     * Update user profile with PATCH semantics.
     * 
     * @param username username of authenticated user
     * @param request patch request with optional fields
     * @return updated profile response
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     * @throws com.ziboto.backend.exception.ValidationException if validation fails
     */
    UpdateProfileResponse patchCurrentUserProfile(String username, com.ziboto.backend.user.dto.UpdateProfilePatchRequest request);
    
    /**
     * Update user profile with PUT semantics using ProfileUpdateRequest.
     * 
     * @param username username of authenticated user
     * @param request profile update request with allowed fields
     * @return updated profile response
     * @throws com.ziboto.backend.exception.ResourceNotFoundException if user not found
     * @throws com.ziboto.backend.exception.ValidationException if validation fails
     */
    UpdateProfileResponse updateUserProfile(String username, com.ziboto.backend.user.dto.ProfileUpdateRequest request);
}
