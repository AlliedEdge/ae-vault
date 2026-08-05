package com.ziboto.backend.user.controller;

import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.common.dto.PageResponse;
import com.ziboto.backend.common.util.SecurityUtils;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.user.dto.*;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import com.ziboto.backend.user.service.StorageUsageService;
import com.ziboto.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Management REST controller.
 * 
 * <p>Provides endpoints for user management operations:</p>
 * <ul>
 *   <li>GET /api/v1/users/me - Get current user profile</li>
 *   <li>PUT /api/v1/users/me - Update current user profile</li>
 *   <li>GET /api/v1/users/{userId} - Get user by ID</li>
 *   <li>GET /api/v1/users - Get all users (admin only)</li>
 *   <li>GET /api/v1/users/search - Search users (admin only)</li>
 *   <li>PUT /api/v1/users/{userId} - Update user (admin only)</li>
 *   <li>DELETE /api/v1/users/{userId} - Delete user (admin only)</li>
 * </ul>
 * 
 * <p>All endpoints require authentication via JWT bearer token.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    
    private final UserService userService;
    private final StorageUsageService storageUsageService;
    private final UserRepository userRepository;
    
    /**
     * Get current authenticated user's profile.
     * 
     * @return current user response
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the profile information of the currently authenticated user. User identity is extracted from SecurityContext."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Fetching profile for current user: {}", username);
        
        UserResponse response = userService.getCurrentUser(username);
        
        log.info("Profile retrieved successfully for user: {}", username);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }
    
    /**
     * Update current authenticated user's profile using PUT semantics.
     * 
     * @param request profile update request with allowed fields only
     * @return updated profile response
     */
    @PutMapping("/profile")
    @Operation(
        summary = "Update current user profile (PUT)",
        description = "Update the profile of the currently authenticated user. Only allowed fields can be updated: firstName, lastName, avatarUrl, timezone, language. User identity is extracted from SecurityContext. Restricted fields (email, password, roles, storage quota, created date) cannot be updated."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UpdateProfileResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data - Invalid timezone or language code"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<UpdateProfileResponse>> updateCurrentUserProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Updating profile for current user: {}", username);
        
        UpdateProfileResponse response = userService.updateUserProfile(username, request);
        
        log.info("Profile updated successfully for user: {}", username);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
    
    /**
     * Update current authenticated user's profile using PATCH semantics.
     * 
     * @param request profile patch request with optional fields
     * @return updated profile response
     */
    @PatchMapping("/profile")
    @Operation(
        summary = "Update current user profile (PATCH)",
        description = "Partially update the profile of the currently authenticated user. Only provided fields will be updated. Allowed fields: firstName, lastName, avatarUrl, timezone, language. User identity is extracted from SecurityContext. Restricted fields (email, password, roles, storage quota, created date) cannot be updated."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UpdateProfileResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data - Invalid timezone or language code"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<UpdateProfileResponse>> patchCurrentUserProfile(
            @Valid @RequestBody UpdateProfilePatchRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Patching profile for current user: {}", username);
        
        UpdateProfileResponse response = userService.patchCurrentUserProfile(username, request);
        
        log.info("Profile patched successfully for user: {}", username);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
    
    /**
     * Get storage information for the current authenticated user.
     * 
     * @return comprehensive storage usage response
     */
    @GetMapping("/storage")
    @Operation(
        summary = "Get current user storage information",
        description = "Retrieve comprehensive storage usage statistics for the currently authenticated user including quota, used storage, available storage, usage percentage, total files, and total folders. User identity is extracted from SecurityContext. Data is calculated from database and cached in Redis for performance."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Storage info retrieved successfully",
            content = @Content(schema = @Schema(implementation = StorageUsageResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponse<StorageUsageResponse>> getCurrentUserStorage() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Fetching storage info for current user: {}", username);
        
        // Get user ID from username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found with username: " + username);
                });
        
        // Calculate storage usage (uses cache if available)
        StorageUsageResponse response = storageUsageService.calculateStorageUsage(user.getId());
        
        log.info("Storage info retrieved successfully for user: {} - Files: {}, Folders: {}, Used: {} bytes", 
                username, response.getTotalFiles(), response.getTotalFolders(), response.getUsedStorage());
        
        return ResponseEntity.ok(ApiResponse.success("Storage info retrieved successfully", response));
    }
    
    /**
     * Get all active sessions for the current authenticated user.
     * 
     * @return list of active sessions
     */
    @GetMapping("/sessions")
    @Operation(
        summary = "Get current user sessions",
        description = "Retrieve all active sessions (refresh tokens) for the currently authenticated user. User identity is extracted from SecurityContext."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        )
    })
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getCurrentUserSessions() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Fetching sessions for current user: {}", username);
        
        List<SessionResponse> sessions = userService.getUserSessions(username);
        
        log.info("Retrieved {} sessions for user: {}", sessions.size(), username);
        return ResponseEntity.ok(ApiResponse.success("Sessions retrieved successfully", sessions));
    }
    
    /**
     * Revoke a specific session for the current authenticated user.
     * 
     * @param sessionId session ID to revoke
     * @return success response
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
        summary = "Revoke a session",
        description = "Revoke a specific session (refresh token) for the currently authenticated user. User identity is extracted from SecurityContext and validated against the session owner."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Session revoked successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Session does not belong to current user"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Session not found"
        )
    })
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @Parameter(description = "Session ID (refresh token ID)", required = true)
            @PathVariable String sessionId) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Revoking session {} for current user: {}", sessionId, username);
        
        userService.revokeSession(username, sessionId);
        
        log.info("Session {} revoked successfully for user: {}", sessionId, username);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully", null));
    }
    
    /**
     * Get user by ID.
     * 
     * @param userId user ID
     * @return user response
     */
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve user information by user ID. Users can view their own profile, admins can view any profile."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {
        log.info("Fetching user by ID: {}", userId);
        
        UserResponse response = userService.getUserById(userId);
        
        log.info("User retrieved successfully: {}", response.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }
    
    /**
     * Get all users with pagination (admin only).
     * 
     * @param pageable pagination parameters
     * @return page of users
     */
    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve all users with pagination. Admin access required."
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = PageResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin access required"
        )
    })
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching all users - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<UserResponse> usersPage = userService.getAllUsers(pageable);
        PageResponse<UserResponse> pageResponse = buildPageResponse(usersPage);
        
        log.info("Retrieved {} users", usersPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", pageResponse));
    }
    
    /**
     * Search users by query (admin only).
     * 
     * @param query search query
     * @param pageable pagination parameters
     * @return page of matching users
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search users",
        description = "Search users by username, email, first name, or last name. Admin access required."
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = PageResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin access required"
        )
    })
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Searching users with query: '{}' - page: {}, size: {}", 
                query, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<UserResponse> usersPage = userService.searchUsers(query, pageable);
        PageResponse<UserResponse> pageResponse = buildPageResponse(usersPage);
        
        log.info("Found {} users matching query", usersPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success("Users found", pageResponse));
    }
    
    /**
     * Update user (admin only).
     * 
     * @param userId user ID
     * @param request update request
     * @return updated user response
     */
    @PutMapping("/{userId}")
    @Operation(
        summary = "Update user",
        description = "Update user information. Admin access required."
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin access required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user with ID: {}", userId);
        
        UserResponse response = userService.updateUser(userId, request);
        
        log.info("User updated successfully: {}", response.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }
    
    /**
     * Delete user (admin only).
     * 
     * @param userId user ID
     * @return success response
     */
    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Delete user",
        description = "Soft delete a user by setting status to DELETED. Admin access required."
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin access required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {
        log.info("Deleting user with ID: {}", userId);
        
        userService.deleteUser(userId);
        
        log.info("User deleted successfully");
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
    
    /**
     * Build PageResponse from Spring Page.
     * 
     * @param page Spring page
     * @return PageResponse DTO
     */
    private <T> PageResponse<T> buildPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
