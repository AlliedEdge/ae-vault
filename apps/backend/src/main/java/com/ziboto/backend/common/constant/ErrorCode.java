package com.ziboto.backend.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Standardized error codes for the application.
 * 
 * <p>Error code ranges:</p>
 * <ul>
 *   <li><b>1000-1099:</b> General errors (validation, not found, server errors)</li>
 *   <li><b>2000-2099:</b> Authentication and authorization errors</li>
 *   <li><b>3000-3099:</b> User-related errors</li>
 *   <li><b>4000-4099:</b> Storage and file errors</li>
 *   <li><b>5000-5099:</b> Bucket-related errors</li>
 *   <li><b>6000-6099:</b> Rate limiting and security errors</li>
 *   <li><b>7000-7099:</b> Session and token errors</li>
 * </ul>
 * 
 * <p>Each error code includes:</p>
 * <ul>
 *   <li>Unique numeric code for client-side error handling</li>
 *   <li>Human-readable message</li>
 *   <li>Corresponding HTTP status code</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // ==================== General Errors (1000-1099) ====================
    
    INTERNAL_SERVER_ERROR(1000, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(1001, "Validation error", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACCESS(1003, "Unauthorized access", HttpStatus.UNAUTHORIZED),
    FORBIDDEN_ACCESS(1004, "Forbidden access", HttpStatus.FORBIDDEN),
    BAD_REQUEST(1005, "Bad request", HttpStatus.BAD_REQUEST),
    CONFLICT(1006, "Resource conflict", HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED(1007, "HTTP method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(1008, "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    SERVICE_UNAVAILABLE(1009, "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    
    // ==================== Authentication Errors (2000-2099) ====================
    
    INVALID_CREDENTIALS(2000, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(2001, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2002, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_MISSING(2003, "Token is missing", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(2004, "Account is disabled", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(2005, "Account is temporarily locked", HttpStatus.FORBIDDEN),
    TOKEN_REVOKED(2006, "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_BLACKLISTED(2007, "Token has been blacklisted", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(2008, "Invalid or expired refresh token", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_REQUIRED(2009, "Password reset is required", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(2010, "Email address is not verified", HttpStatus.FORBIDDEN),
    MFA_REQUIRED(2011, "Multi-factor authentication is required", HttpStatus.UNAUTHORIZED),
    INVALID_MFA_CODE(2012, "Invalid MFA code", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(2013, "Session has expired", HttpStatus.UNAUTHORIZED),
    CONCURRENT_SESSION_LIMIT(2014, "Maximum concurrent sessions exceeded", HttpStatus.FORBIDDEN),
    
    // ==================== User Errors (3000-3099) ====================
    
    USER_NOT_FOUND(3000, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(3001, "User already exists", HttpStatus.CONFLICT),
    USER_EMAIL_EXISTS(3002, "Email already in use", HttpStatus.CONFLICT),
    USER_USERNAME_EXISTS(3003, "Username already in use", HttpStatus.CONFLICT),
    INVALID_USER_DATA(3004, "Invalid user data", HttpStatus.BAD_REQUEST),
    WEAK_PASSWORD(3005, "Password does not meet security requirements", HttpStatus.BAD_REQUEST),
    PASSWORD_RECENTLY_USED(3006, "Password was recently used", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(3007, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_FORMAT(3008, "Invalid username format", HttpStatus.BAD_REQUEST),
    USER_SUSPENDED(3009, "User account is suspended", HttpStatus.FORBIDDEN),
    USER_DELETED(3010, "User account has been deleted", HttpStatus.GONE),
    
    // ==================== Storage/File Errors (4000-4099) ====================
    
    FILE_NOT_FOUND(4000, "File not found", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(4001, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DOWNLOAD_FAILED(4002, "File download failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(4003, "File deletion failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE(4004, "File size exceeds maximum allowed", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_FILE_TYPE(4005, "Invalid file type", HttpStatus.BAD_REQUEST),
    STORAGE_QUOTA_EXCEEDED(4006, "Storage quota exceeded", HttpStatus.INSUFFICIENT_STORAGE),
    FILE_NAME_INVALID(4007, "Invalid file name", HttpStatus.BAD_REQUEST),
    FILE_ALREADY_EXISTS(4008, "File already exists", HttpStatus.CONFLICT),
    FILE_LOCKED(4009, "File is locked by another process", HttpStatus.CONFLICT),
    
    // ==================== Bucket Errors (5000-5099) ====================
    
    BUCKET_NOT_FOUND(5000, "Bucket not found", HttpStatus.NOT_FOUND),
    BUCKET_ALREADY_EXISTS(5001, "Bucket already exists", HttpStatus.CONFLICT),
    BUCKET_NOT_EMPTY(5002, "Bucket is not empty", HttpStatus.CONFLICT),
    INVALID_BUCKET_NAME(5003, "Invalid bucket name", HttpStatus.BAD_REQUEST),
    BUCKET_ACCESS_DENIED(5004, "Access to bucket denied", HttpStatus.FORBIDDEN),
    BUCKET_QUOTA_EXCEEDED(5005, "Bucket quota exceeded", HttpStatus.INSUFFICIENT_STORAGE),
    
    // ==================== Rate Limiting & Security Errors (6000-6099) ====================
    
    RATE_LIMIT_EXCEEDED(6000, "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    LOGIN_RATE_LIMIT_EXCEEDED(6001, "Too many login attempts", HttpStatus.TOO_MANY_REQUESTS),
    SIGNUP_RATE_LIMIT_EXCEEDED(6002, "Too many signup attempts", HttpStatus.TOO_MANY_REQUESTS),
    API_RATE_LIMIT_EXCEEDED(6003, "API rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    TOKEN_REFRESH_RATE_LIMIT_EXCEEDED(6004, "Too many token refresh attempts", HttpStatus.TOO_MANY_REQUESTS),
    FAILED_LOGIN_ATTEMPTS_EXCEEDED(6005, "Account locked due to failed login attempts", HttpStatus.FORBIDDEN),
    IP_BLOCKED(6006, "Your IP address has been blocked", HttpStatus.FORBIDDEN),
    SUSPICIOUS_ACTIVITY_DETECTED(6007, "Suspicious activity detected", HttpStatus.FORBIDDEN),
    SECURITY_VIOLATION(6008, "Security policy violation", HttpStatus.FORBIDDEN),
    INVALID_ORIGIN(6009, "Invalid request origin", HttpStatus.FORBIDDEN),
    CSRF_TOKEN_INVALID(6010, "Invalid CSRF token", HttpStatus.FORBIDDEN),
    
    // ==================== Session & Token Management Errors (7000-7099) ====================
    
    SESSION_NOT_FOUND(7000, "Session not found", HttpStatus.NOT_FOUND),
    SESSION_INVALID(7001, "Invalid session", HttpStatus.UNAUTHORIZED),
    SESSION_TERMINATED(7002, "Session has been terminated", HttpStatus.UNAUTHORIZED),
    DEVICE_NOT_AUTHORIZED(7003, "Device is not authorized", HttpStatus.FORBIDDEN),
    LOCATION_CHANGE_DETECTED(7004, "Unusual location change detected", HttpStatus.FORBIDDEN),
    TOKEN_FINGERPRINT_MISMATCH(7005, "Token fingerprint mismatch", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_ROTATION_FAILED(7006, "Refresh token rotation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    SESSION_HIJACKING_DETECTED(7007, "Potential session hijacking detected", HttpStatus.FORBIDDEN);
    
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    
    /**
     * Get HTTP status code value.
     * 
     * @return HTTP status code (e.g., 400, 401, 404)
     */
    public int getHttpStatusValue() {
        return httpStatus.value();
    }
    
    /**
     * Check if error is a client error (4xx).
     * 
     * @return true if error is caused by client
     */
    public boolean isClientError() {
        return httpStatus.is4xxClientError();
    }
    
    /**
     * Check if error is a server error (5xx).
     * 
     * @return true if error is caused by server
     */
    public boolean isServerError() {
        return httpStatus.is5xxServerError();
    }
    
    /**
     * Check if error is security-related.
     * 
     * @return true if error is security-related
     */
    public boolean isSecurityError() {
        return code >= 2000 && code < 3000 || code >= 6000 && code < 8000;
    }
}
