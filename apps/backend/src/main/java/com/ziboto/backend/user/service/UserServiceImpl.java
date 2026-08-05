package com.ziboto.backend.user.service;

import com.ziboto.backend.audit.entity.AuditAction;
import com.ziboto.backend.audit.service.AuditService;
import com.ziboto.backend.auth.entity.RefreshToken;
import com.ziboto.backend.auth.repository.RefreshTokenRepository;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.exception.UnauthorizedException;
import com.ziboto.backend.user.dto.*;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserStatus;
import com.ziboto.backend.user.mapper.UserMapper;
import com.ziboto.backend.user.repository.UserRepository;
import com.ziboto.backend.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of UserService.
 * 
 * <p>Provides user management operations including CRUD, search,
 * and profile updates with proper validation and audit logging.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final AuditService auditService;
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        
        userValidator.validateUserId(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });
        
        log.debug("User found: {}", user.getUsername());
        return userMapper.toResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        log.debug("User found: {}", user.getUsername());
        return userMapper.toResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        
        log.debug("User found: {}", user.getUsername());
        return userMapper.toResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = userRepository.findAll(pageable);
        
        log.debug("Found {} users", users.getTotalElements());
        return users.map(userMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        log.debug("Searching users with query: '{}', page={}, size={}", 
                search, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = userRepository.searchUsers(search, pageable);
        
        log.debug("Found {} users matching search query", users.getTotalElements());
        return users.map(userMapper::toResponse);
    }
    
    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", userId);
        
        // Validate user ID
        userValidator.validateUserId(userId);
        
        // Validate request has at least one field to update
        userValidator.validateAdminUpdateFieldsPresent(request);
        
        // Validate business rules
        userValidator.validateUserUpdate(userId, request);
        
        // Fetch existing user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });
        
        String oldEmail = user.getEmail();
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        
        // Apply updates using mapper
        userMapper.updateUserFromRequest(request, user);
        
        // Save updated user
        User updatedUser = userRepository.save(user);
        
        // Log audit event
        auditService.log(
                "User",
                userId,
                AuditAction.UPDATE,
                String.format("User profile updated - Email: %s -> %s, Name: %s %s -> %s %s",
                        oldEmail, updatedUser.getEmail(),
                        oldFirstName, oldLastName,
                        updatedUser.getFirstName(), updatedUser.getLastName())
        );
        
        log.info("User updated successfully: {}", user.getUsername());
        return userMapper.toResponse(updatedUser);
    }
    
    @Override
    @Transactional
    public UpdateProfileResponse updateCurrentUserProfile(String username, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", username);
        
        // Fetch user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        // Validate request has at least one field to update
        userValidator.validateUpdateFieldsPresent(request);
        
        // Validate business rules
        userValidator.validateProfileUpdate(user.getId(), request);
        
        // Apply updates using mapper
        userMapper.updateUserFromProfileRequest(request, user);
        
        // Save updated user
        User updatedUser = userRepository.save(user);
        
        // Log audit event
        auditService.log(
                "User",
                user.getId(),
                AuditAction.UPDATE,
                String.format("User profile updated by owner - Name: %s %s",
                        updatedUser.getFirstName(), updatedUser.getLastName())
        );
        
        log.info("Profile updated successfully for user: {}", username);
        return userMapper.toUpdateProfileResponse(updatedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        log.debug("Fetching current user profile: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        log.debug("Current user profile retrieved: {}", username);
        return userMapper.toResponse(user);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);
        
        // Validate user ID
        userValidator.validateUserId(userId);
        
        // Fetch user to ensure it exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });
        
        String username = user.getUsername();
        
        // Soft delete by setting status to DELETED
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        
        // Log audit event
        auditService.log(
                "User",
                userId,
                AuditAction.DELETE,
                String.format("User soft deleted: %s", username)
        );
        
        log.info("User soft deleted successfully: {}", username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    @Transactional(readOnly = true)
    public StorageInfoResponse getUserStorageInfo(String username) {
        log.debug("Fetching storage info for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        StorageInfoResponse response = StorageInfoResponse.from(
                user.getStorageQuota(),
                user.getStorageUsed()
        );
        
        log.debug("Storage info retrieved for user: {} - Used: {}/{} bytes ({}%)",
                username, response.getStorageUsed(), response.getStorageQuota(), response.getUsagePercentage());
        
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(String username) {
        log.debug("Fetching sessions for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        // Get all active (non-revoked and non-expired) refresh tokens
        List<RefreshToken> activeTokens = refreshTokenRepository.findActiveTokensByUserId(
                user.getId(),
                LocalDateTime.now()
        );
        
        List<SessionResponse> sessions = activeTokens.stream()
                .map(token -> SessionResponse.builder()
                        .id(token.getId().toString())
                        .deviceInfo(token.getDeviceInfo())
                        .ipAddress(token.getIpAddress())
                        .userAgent(token.getUserAgent())
                        .createdAt(token.getCreatedAt())
                        .lastUsedAt(token.getLastUsedAt())
                        .expiresAt(token.getExpiresAt())
                        .current(false) // Will be set by controller if needed
                        .build())
                .collect(Collectors.toList());
        
        log.debug("Found {} active sessions for user: {}", sessions.size(), username);
        
        return sessions;
    }
    
    @Override
    @Transactional
    public void revokeSession(String username, String sessionId) {
        log.info("Revoking session {} for user: {}", sessionId, username);
        
        // Validate session ID format
        UUID sessionUuid;
        try {
            sessionUuid = UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid session ID format: {}", sessionId);
            throw new ResourceNotFoundException("Invalid session ID format");
        }
        
        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        // Get refresh token
        RefreshToken refreshToken = refreshTokenRepository.findById(sessionUuid)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found");
                });
        
        // Verify the session belongs to the user
        if (!refreshToken.getUser().getId().equals(user.getId())) {
            log.error("Session {} does not belong to user: {}", sessionId, username);
            throw new UnauthorizedException("You are not authorized to revoke this session");
        }
        
        // Revoke the session
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        
        // Log audit event
        auditService.log(
                "Session",
                user.getId(),
                AuditAction.DELETE,
                String.format("Session revoked: %s from %s", sessionId, refreshToken.getIpAddress())
        );
        
        log.info("Session {} revoked successfully for user: {}", sessionId, username);
    }
    
    @Override
    @Transactional
    public UpdateProfileResponse patchCurrentUserProfile(String username, UpdateProfilePatchRequest request) {
        log.info("Patching profile for user: {}", username);
        
        // Fetch user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        // Validate request
        userValidator.validatePatchUpdateFieldsPresent(request);
        userValidator.validatePatchProfileUpdate(request);
        
        // Apply updates using mapper
        userMapper.updateUserFromPatchRequest(request, user);
        
        // Save updated user
        User updatedUser = userRepository.save(user);
        
        // Log audit event
        auditService.log(
                "User",
                user.getId(),
                AuditAction.UPDATE,
                String.format("User profile patched - Fields updated")
        );
        
        log.info("Profile patched successfully for user: {}", username);
        return userMapper.toUpdateProfileResponse(updatedUser);
    }
    
    @Override
    @Transactional
    public UpdateProfileResponse updateUserProfile(String username, ProfileUpdateRequest request) {
        log.info("Updating profile for user: {}", username);
        
        // Fetch user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        // Validate request
        userValidator.validateProfileUpdateFieldsPresent(request);
        userValidator.validateProfileUpdateRequest(request);
        
        // Apply updates using mapper
        userMapper.updateUserFromProfileUpdateRequest(request, user);
        
        // Save updated user
        User updatedUser = userRepository.save(user);
        
        // Log audit event
        auditService.log(
                "User",
                user.getId(),
                AuditAction.UPDATE,
                String.format("User profile updated - Name: %s %s, Timezone: %s, Language: %s",
                        updatedUser.getFirstName(), updatedUser.getLastName(),
                        updatedUser.getTimezone(), updatedUser.getLanguage())
        );
        
        log.info("Profile updated successfully for user: {}", username);
        return userMapper.toUpdateProfileResponse(updatedUser);
    }
    
    /**
     * Convert UpdateProfilePatchRequest to UpdateProfileRequest for validation.
     * 
     * @param patchRequest patch request
     * @return profile request
     */
    private UpdateProfileRequest convertPatchToProfileRequest(UpdateProfilePatchRequest patchRequest) {
        return UpdateProfileRequest.builder()
                .firstName(patchRequest.getFirstName())
                .lastName(patchRequest.getLastName())
                .avatarUrl(patchRequest.getAvatarUrl())
                .timezone(patchRequest.getTimezone())
                .language(patchRequest.getLanguage())
                .build();
    }
}
