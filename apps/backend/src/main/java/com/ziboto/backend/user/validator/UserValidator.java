package com.ziboto.backend.user.validator;

import com.ziboto.backend.exception.ValidationException;
import com.ziboto.backend.user.dto.ProfileUpdateRequest;
import com.ziboto.backend.user.dto.UpdateProfilePatchRequest;
import com.ziboto.backend.user.dto.UpdateProfileRequest;
import com.ziboto.backend.user.dto.UpdateUserRequest;
import com.ziboto.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validator for user-related business rules.
 * 
 * <p>Provides validation beyond basic JSR-380 annotations,
 * including database-level uniqueness checks and business logic validation.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {
    
    private final UserRepository userRepository;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    
    /**
     * Validates email uniqueness when updating profile.
     * 
     * @param userId current user ID
     * @param email new email address
     * @throws ValidationException if email already exists for another user
     */
    public void validateEmailUniqueness(Long userId, String email) {
        if (StringUtils.hasText(email)) {
            userRepository.findByEmail(email).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                    log.warn("Email already exists for another user: {}", email);
                    throw new ValidationException("Email already exists");
                }
            });
        }
    }
    
    /**
     * Validates username format and uniqueness.
     * 
     * @param username username to validate
     * @throws ValidationException if username is invalid or already exists
     */
    public void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ValidationException("Username cannot be empty");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(
                "Username must be 3-50 characters and contain only letters, numbers, hyphens, and underscores"
            );
        }
        
        if (userRepository.existsByUsername(username)) {
            log.warn("Username already exists: {}", username);
            throw new ValidationException("Username already exists");
        }
    }
    
    /**
     * Validates email format and uniqueness.
     * 
     * @param email email to validate
     * @throws ValidationException if email is invalid or already exists
     */
    public void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email cannot be empty");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email format");
        }
        
        if (userRepository.existsByEmail(email)) {
            log.warn("Email already exists: {}", email);
            throw new ValidationException("Email already exists");
        }
    }
    
    /**
     * Validates update profile request.
     * 
     * @param userId current user ID
     * @param request profile update request
     * @throws ValidationException if validation fails
     */
    public void validateProfileUpdate(Long userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        // Validate avatar URL format if provided
        if (StringUtils.hasText(request.getAvatarUrl())) {
            validateUrl(request.getAvatarUrl(), "Avatar URL");
        }
        
        // Validate timezone if provided
        if (StringUtils.hasText(request.getTimezone())) {
            validateTimezone(request.getTimezone());
        }
        
        // Validate language if provided
        if (StringUtils.hasText(request.getLanguage())) {
            validateLanguage(request.getLanguage());
        }
    }
    
    /**
     * Validates profile update request with new ProfileUpdateRequest.
     * 
     * @param request profile update request
     * @throws ValidationException if validation fails
     */
    public void validateProfileUpdateRequest(ProfileUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        // Validate avatar URL format if provided
        if (StringUtils.hasText(request.getAvatarUrl())) {
            validateUrl(request.getAvatarUrl(), "Avatar URL");
        }
        
        // Validate timezone if provided
        if (StringUtils.hasText(request.getTimezone())) {
            validateTimezone(request.getTimezone());
        }
        
        // Validate language if provided
        if (StringUtils.hasText(request.getLanguage())) {
            validateLanguage(request.getLanguage());
        }
    }
    
    /**
     * Validates patch profile update request.
     * 
     * @param request patch profile update request
     * @throws ValidationException if validation fails
     */
    public void validatePatchProfileUpdate(UpdateProfilePatchRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        // Validate avatar URL format if provided
        if (StringUtils.hasText(request.getAvatarUrl())) {
            validateUrl(request.getAvatarUrl(), "Avatar URL");
        }
        
        // Validate timezone if provided
        if (StringUtils.hasText(request.getTimezone())) {
            validateTimezone(request.getTimezone());
        }
        
        // Validate language if provided
        if (StringUtils.hasText(request.getLanguage())) {
            validateLanguage(request.getLanguage());
        }
    }
    
    /**
     * Validates timezone string.
     * 
     * @param timezone timezone to validate
     * @throws ValidationException if timezone is invalid
     */
    public void validateTimezone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return;
        }
        
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("Invalid timezone: {}", timezone);
            throw new ValidationException(
                "Invalid timezone: " + timezone + ". Must be a valid timezone identifier (e.g., America/New_York, UTC, Europe/London)"
            );
        }
    }
    
    /**
     * Validates language code.
     * 
     * @param language language code to validate
     * @throws ValidationException if language code is invalid
     */
    public void validateLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return;
        }
        
        // Check if it's a valid ISO 639-1 language code
        String[] parts = language.split("-");
        String languageCode = parts[0].toLowerCase();
        
        // Get all available language codes
        List<String> validLanguages = Arrays.asList(Locale.getISOLanguages());
        
        if (!validLanguages.contains(languageCode)) {
            log.warn("Invalid language code: {}", language);
            throw new ValidationException(
                "Invalid language code: " + language + ". Must be a valid ISO 639-1 language code (e.g., en, en-US, es, fr)"
            );
        }
        
        // If there's a country code, validate it too
        if (parts.length > 1) {
            String countryCode = parts[1].toUpperCase();
            List<String> validCountries = Arrays.asList(Locale.getISOCountries());
            
            if (!validCountries.contains(countryCode)) {
                log.warn("Invalid country code in language: {}", language);
                throw new ValidationException(
                    "Invalid country code in language: " + language + ". Country code must be a valid ISO 3166-1 alpha-2 code"
                );
            }
        }
    }
    
    /**
     * Validates update user request for admin operations.
     * 
     * @param userId user ID being updated
     * @param request update request
     * @throws ValidationException if validation fails
     */
    public void validateUserUpdate(Long userId, UpdateUserRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        // Validate email uniqueness if provided
        if (StringUtils.hasText(request.getEmail())) {
            validateEmailUniqueness(userId, request.getEmail());
        }
        
        // Validate avatar URL format if provided
        if (StringUtils.hasText(request.getAvatarUrl())) {
            validateUrl(request.getAvatarUrl(), "Avatar URL");
        }
    }
    
    /**
     * Validates URL format.
     * 
     * @param url URL to validate
     * @param fieldName name of the field for error message
     * @throws ValidationException if URL is invalid
     */
    private void validateUrl(String url, String fieldName) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        
        // Basic URL validation
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ValidationException(fieldName + " must start with http:// or https://");
        }
        
        if (url.length() > 500) {
            throw new ValidationException(fieldName + " must not exceed 500 characters");
        }
    }
    
    /**
     * Validates user ID.
     * 
     * @param userId user ID to validate
     * @throws ValidationException if user ID is invalid
     */
    public void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Invalid user ID");
        }
    }
    
    /**
     * Validates that at least one field is provided for update.
     * 
     * @param request update request
     * @throws ValidationException if no fields are provided
     */
    public void validateUpdateFieldsPresent(UpdateProfileRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        boolean hasUpdates = StringUtils.hasText(request.getFirstName()) ||
                           StringUtils.hasText(request.getLastName()) ||
                           StringUtils.hasText(request.getAvatarUrl()) ||
                           StringUtils.hasText(request.getTimezone()) ||
                           StringUtils.hasText(request.getLanguage());
        
        if (!hasUpdates) {
            throw new ValidationException("At least one field must be provided for update");
        }
    }
    
    /**
     * Validates that at least one field is provided for profile update.
     * 
     * @param request profile update request
     * @throws ValidationException if no fields are provided
     */
    public void validateProfileUpdateFieldsPresent(ProfileUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        boolean hasUpdates = StringUtils.hasText(request.getFirstName()) ||
                           StringUtils.hasText(request.getLastName()) ||
                           StringUtils.hasText(request.getAvatarUrl()) ||
                           StringUtils.hasText(request.getTimezone()) ||
                           StringUtils.hasText(request.getLanguage());
        
        if (!hasUpdates) {
            throw new ValidationException("At least one field must be provided for update");
        }
    }
    
    /**
     * Validates that at least one field is provided for patch update.
     * 
     * @param request patch update request
     * @throws ValidationException if no fields are provided
     */
    public void validatePatchUpdateFieldsPresent(UpdateProfilePatchRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        boolean hasUpdates = StringUtils.hasText(request.getFirstName()) ||
                           StringUtils.hasText(request.getLastName()) ||
                           StringUtils.hasText(request.getAvatarUrl()) ||
                           StringUtils.hasText(request.getTimezone()) ||
                           StringUtils.hasText(request.getLanguage());
        
        if (!hasUpdates) {
            throw new ValidationException("At least one field must be provided for update");
        }
    }
    
    /**
     * Validates that at least one field is provided for admin update.
     * 
     * @param request update request
     * @throws ValidationException if no fields are provided
     */
    public void validateAdminUpdateFieldsPresent(UpdateUserRequest request) {
        if (request == null) {
            throw new ValidationException("Update request cannot be null");
        }
        
        boolean hasUpdates = StringUtils.hasText(request.getEmail()) ||
                           StringUtils.hasText(request.getFirstName()) ||
                           StringUtils.hasText(request.getLastName()) ||
                           StringUtils.hasText(request.getAvatarUrl());
        
        if (!hasUpdates) {
            throw new ValidationException("At least one field must be provided for update");
        }
    }
}
