package com.ziboto.backend.common.constant;

/**
 * Centralized validation messages for consistent error responses.
 * 
 * <p>This class provides standardized validation messages used across
 * DTO validation annotations to ensure consistent user-facing error messages.</p>
 * 
 * <h2>Categories:</h2>
 * <ul>
 *   <li><b>Required Fields:</b> Messages for missing required fields</li>
 *   <li><b>Username Validation:</b> Username format and constraints</li>
 *   <li><b>Email Validation:</b> Email format and uniqueness</li>
 *   <li><b>Password Validation:</b> Password strength requirements</li>
 *   <li><b>Name Validation:</b> First/last name constraints</li>
 *   <li><b>File Validation:</b> File upload constraints</li>
 *   <li><b>Bucket Validation:</b> Bucket name constraints</li>
 *   <li><b>General Constraints:</b> Size, format, and pattern messages</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>
 * {@code
 * @NotBlank(message = ValidationMessages.USERNAME_REQUIRED)
 * @Size(min = 3, max = 50, message = ValidationMessages.USERNAME_SIZE)
 * private String username;
 * }
 * </pre>
 */
public final class ValidationMessages {
    
    // Prevent instantiation
    private ValidationMessages() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== Required Fields ====================
    
    public static final String REQUIRED = "This field is required";
    public static final String NOT_BLANK = "This field cannot be blank";
    public static final String NOT_NULL = "This field cannot be null";
    public static final String NOT_EMPTY = "This field cannot be empty";
    
    // ==================== Username Validation ====================
    
    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String USERNAME_SIZE = "Username must be between 3 and 50 characters";
    public static final String USERNAME_PATTERN = "Username can only contain letters, numbers, underscores, and hyphens";
    public static final String USERNAME_INVALID = "Username format is invalid";
    public static final String USERNAME_TAKEN = "Username is already taken";
    public static final String USERNAME_RESERVED = "Username is reserved and cannot be used";
    public static final String USERNAME_MIN_LENGTH = "Username must be at least 3 characters";
    public static final String USERNAME_MAX_LENGTH = "Username cannot exceed 50 characters";
    
    // ==================== Email Validation ====================
    
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID = "Email address is not valid";
    public static final String EMAIL_SIZE = "Email must not exceed 100 characters";
    public static final String EMAIL_TAKEN = "Email address is already registered";
    public static final String EMAIL_FORMAT = "Please provide a valid email address";
    public static final String EMAIL_DOMAIN_INVALID = "Email domain is not allowed";
    
    // ==================== Password Validation ====================
    
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_SIZE = "Password must be between 8 and 100 characters";
    public static final String PASSWORD_STRENGTH = "Password must contain at least one uppercase letter, one lowercase letter, and one number";
    public static final String PASSWORD_WEAK = "Password is too weak. Please use a stronger password";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters";
    public static final String PASSWORD_MAX_LENGTH = "Password cannot exceed 100 characters";
    public static final String PASSWORD_NO_UPPERCASE = "Password must contain at least one uppercase letter";
    public static final String PASSWORD_NO_LOWERCASE = "Password must contain at least one lowercase letter";
    public static final String PASSWORD_NO_DIGIT = "Password must contain at least one number";
    public static final String PASSWORD_NO_SPECIAL = "Password must contain at least one special character";
    public static final String PASSWORD_COMMON = "Password is too common. Please choose a different password";
    public static final String PASSWORD_MISMATCH = "Passwords do not match";
    public static final String PASSWORD_SAME_AS_OLD = "New password must be different from old password";
    public static final String PASSWORD_RECENTLY_USED = "This password was recently used. Please choose a different password";
    public static final String CURRENT_PASSWORD_REQUIRED = "Current password is required";
    public static final String NEW_PASSWORD_REQUIRED = "New password is required";
    public static final String CONFIRM_PASSWORD_REQUIRED = "Password confirmation is required";
    
    // ==================== Name Validation ====================
    
    public static final String FIRST_NAME_SIZE = "First name must not exceed 100 characters";
    public static final String LAST_NAME_SIZE = "Last name must not exceed 100 characters";
    public static final String DISPLAY_NAME_SIZE = "Display name must not exceed 100 characters";
    public static final String NAME_PATTERN = "Name can only contain letters, spaces, hyphens, and apostrophes";
    public static final String NAME_INVALID = "Name format is invalid";
    
    // ==================== File Validation ====================
    
    public static final String FILE_REQUIRED = "File is required";
    public static final String FILE_TOO_LARGE = "File size exceeds maximum allowed size";
    public static final String FILE_TYPE_INVALID = "File type is not allowed";
    public static final String FILE_NAME_REQUIRED = "File name is required";
    public static final String FILE_NAME_SIZE = "File name must not exceed 255 characters";
    public static final String FILE_NAME_INVALID = "File name contains invalid characters";
    public static final String FILE_EXTENSION_INVALID = "File extension is not allowed";
    public static final String FILE_EMPTY = "File cannot be empty";
    public static final String FILE_CORRUPTED = "File appears to be corrupted";
    
    // ==================== Bucket Validation ====================
    
    public static final String BUCKET_NAME_REQUIRED = "Bucket name is required";
    public static final String BUCKET_NAME_SIZE = "Bucket name must be between 3 and 63 characters";
    public static final String BUCKET_NAME_PATTERN = "Bucket name can only contain lowercase letters, numbers, and hyphens";
    public static final String BUCKET_NAME_INVALID = "Bucket name format is invalid";
    public static final String BUCKET_DESCRIPTION_SIZE = "Bucket description must not exceed 500 characters";
    
    // ==================== Token Validation ====================
    
    public static final String TOKEN_REQUIRED = "Token is required";
    public static final String TOKEN_INVALID = "Token is invalid";
    public static final String TOKEN_EXPIRED = "Token has expired";
    public static final String REFRESH_TOKEN_REQUIRED = "Refresh token is required";
    public static final String ACCESS_TOKEN_REQUIRED = "Access token is required";
    
    // ==================== Authentication Validation ====================
    
    public static final String USERNAME_OR_EMAIL_REQUIRED = "Username or email is required";
    public static final String CREDENTIALS_REQUIRED = "Credentials are required";
    public static final String LOGIN_IDENTIFIER_REQUIRED = "Username or email is required for login";
    
    // ==================== Date/Time Validation ====================
    
    public static final String DATE_REQUIRED = "Date is required";
    public static final String DATE_FUTURE = "Date must be in the future";
    public static final String DATE_PAST = "Date must be in the past";
    public static final String DATE_INVALID = "Date format is invalid";
    public static final String DATE_RANGE_INVALID = "Start date must be before end date";
    
    // ==================== Numeric Validation ====================
    
    public static final String NUMBER_POSITIVE = "Value must be positive";
    public static final String NUMBER_NON_NEGATIVE = "Value must be non-negative";
    public static final String NUMBER_MIN = "Value must be at least {min}";
    public static final String NUMBER_MAX = "Value must not exceed {max}";
    public static final String NUMBER_RANGE = "Value must be between {min} and {max}";
    
    // ==================== Collection Validation ====================
    
    public static final String LIST_EMPTY = "List cannot be empty";
    public static final String LIST_TOO_LARGE = "List contains too many items";
    public static final String LIST_SIZE = "List must contain between {min} and {max} items";
    
    // ==================== URL Validation ====================
    
    public static final String URL_REQUIRED = "URL is required";
    public static final String URL_INVALID = "URL format is invalid";
    public static final String URL_PROTOCOL_INVALID = "URL must use HTTP or HTTPS protocol";
    
    // ==================== Phone Validation ====================
    
    public static final String PHONE_INVALID = "Phone number format is invalid";
    public static final String PHONE_SIZE = "Phone number must not exceed 20 characters";
    
    // ==================== Address Validation ====================
    
    public static final String ADDRESS_SIZE = "Address must not exceed 255 characters";
    public static final String CITY_SIZE = "City name must not exceed 100 characters";
    public static final String STATE_SIZE = "State name must not exceed 100 characters";
    public static final String COUNTRY_SIZE = "Country name must not exceed 100 characters";
    public static final String POSTAL_CODE_SIZE = "Postal code must not exceed 20 characters";
    public static final String POSTAL_CODE_INVALID = "Postal code format is invalid";
    
    // ==================== General Constraints ====================
    
    public static final String SIZE_MIN = "Must be at least {min} characters";
    public static final String SIZE_MAX = "Must not exceed {max} characters";
    public static final String SIZE_RANGE = "Must be between {min} and {max} characters";
    public static final String PATTERN_INVALID = "Format is invalid";
    public static final String VALUE_INVALID = "Value is invalid";
    public static final String FORMAT_INVALID = "Format is invalid";
    
    // ==================== Boolean Validation ====================
    
    public static final String BOOLEAN_REQUIRED = "This field must be true or false";
    public static final String MUST_BE_TRUE = "This field must be true";
    public static final String MUST_BE_FALSE = "This field must be false";
    public static final String TERMS_ACCEPTANCE_REQUIRED = "You must accept the terms and conditions";
    
    // ==================== UUID Validation ====================
    
    public static final String UUID_INVALID = "Invalid UUID format";
    public static final String ID_INVALID = "Invalid ID format";
    public static final String ID_REQUIRED = "ID is required";
    
    // ==================== Content Validation ====================
    
    public static final String CONTENT_REQUIRED = "Content is required";
    public static final String DESCRIPTION_SIZE = "Description must not exceed 500 characters";
    public static final String COMMENT_SIZE = "Comment must not exceed 1000 characters";
    public static final String MESSAGE_SIZE = "Message must not exceed 5000 characters";
    public static final String TITLE_REQUIRED = "Title is required";
    public static final String TITLE_SIZE = "Title must be between 1 and 200 characters";
    
    // ==================== Role/Permission Validation ====================
    
    public static final String ROLE_REQUIRED = "Role is required";
    public static final String ROLE_INVALID = "Invalid role";
    public static final String PERMISSION_REQUIRED = "Permission is required";
    public static final String PERMISSION_INVALID = "Invalid permission";
    
    // ==================== Status Validation ====================
    
    public static final String STATUS_REQUIRED = "Status is required";
    public static final String STATUS_INVALID = "Invalid status value";
    
    // ==================== Pagination Validation ====================
    
    public static final String PAGE_NUMBER_INVALID = "Page number must be non-negative";
    public static final String PAGE_SIZE_INVALID = "Page size must be positive";
    public static final String PAGE_SIZE_TOO_LARGE = "Page size exceeds maximum allowed";
    public static final String SORT_FIELD_INVALID = "Invalid sort field";
    public static final String SORT_DIRECTION_INVALID = "Sort direction must be ASC or DESC";
    
    // ==================== Search Validation ====================
    
    public static final String SEARCH_QUERY_REQUIRED = "Search query is required";
    public static final String SEARCH_QUERY_TOO_SHORT = "Search query must be at least 2 characters";
    public static final String SEARCH_QUERY_TOO_LONG = "Search query must not exceed 200 characters";
    
    // ==================== Tag Validation ====================
    
    public static final String TAG_INVALID = "Tag format is invalid";
    public static final String TAG_SIZE = "Tag must be between 2 and 50 characters";
    public static final String TOO_MANY_TAGS = "Maximum number of tags exceeded";
    
    // ==================== Color Validation ====================
    
    public static final String COLOR_INVALID = "Color format is invalid (must be hex format: #RRGGBB)";
    
    // ==================== JSON Validation ====================
    
    public static final String JSON_INVALID = "Invalid JSON format";
    public static final String JSON_REQUIRED = "JSON data is required";
}
