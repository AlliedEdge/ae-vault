package com.ziboto.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile information.
 * 
 * <p>Used when an authenticated user updates their own profile.
 * All fields are optional - only provided fields will be updated.</p>
 * 
 * <p>Email updates should use a separate endpoint with verification.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
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
