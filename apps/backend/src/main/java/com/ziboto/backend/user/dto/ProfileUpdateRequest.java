package com.ziboto.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user profile update requests.
 * 
 * <p>Contains ONLY fields that users are allowed to update themselves.
 * Restricted fields (email, password, roles, storage quota, etc.) are NOT included.</p>
 * 
 * <p>Allowed updates:</p>
 * <ul>
 *   <li>First Name</li>
 *   <li>Last Name</li>
 *   <li>Profile Picture URL (avatarUrl)</li>
 *   <li>Timezone</li>
 *   <li>Language</li>
 * </ul>
 * 
 * <p>NOT allowed (enforced by DTO design):</p>
 * <ul>
 *   <li>Email</li>
 *   <li>Password</li>
 *   <li>Username</li>
 *   <li>Roles</li>
 *   <li>Status</li>
 *   <li>Storage Quota</li>
 *   <li>Created Date</li>
 *   <li>Email Verified</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;
    
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;
    
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @Pattern(
        regexp = "^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$",
        message = "Avatar URL must be a valid URL"
    )
    private String avatarUrl;
    
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    @Pattern(
        regexp = "^[A-Za-z/_+-]+$",
        message = "Timezone must be a valid timezone identifier (e.g., America/New_York, UTC, Europe/London)"
    )
    private String timezone;
    
    @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
    @Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$",
        message = "Language must be a valid ISO 639-1 language code (e.g., en, en-US, es, fr)"
    )
    private String language;
}
