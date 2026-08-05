package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for centralized error handling.
 * 
 * <p>Handles all exceptions thrown by the application and converts them
 * into standardized JSON error responses with appropriate HTTP status codes.</p>
 * 
 * <h2>Exception Categories:</h2>
 * <ul>
 *   <li><b>Custom Exceptions:</b> BaseException and its subclasses</li>
 *   <li><b>Validation Exceptions:</b> @Valid annotation failures</li>
 *   <li><b>Authentication Exceptions:</b> Spring Security errors</li>
 *   <li><b>Authorization Exceptions:</b> Access denied errors</li>
 *   <li><b>File Upload Exceptions:</b> Size and format errors</li>
 *   <li><b>HTTP Exceptions:</b> Method not allowed, unsupported media type</li>
 *   <li><b>General Exceptions:</b> Catch-all for unexpected errors</li>
 * </ul>
 * 
 * <h2>Security Features:</h2>
 * <ul>
 *   <li>Comprehensive logging of security-related exceptions</li>
 *   <li>IP address and request path logging for security audits</li>
 *   <li>Stack trace suppression in production</li>
 *   <li>Standardized error response format</li>
 *   <li>Rate limiting exception handling</li>
 *   <li>Account lockout exception handling</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // ==================== Custom Application Exceptions ====================
    
    /**
     * Handle all custom BaseException and its subclasses.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with appropriate status code
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Object>> handleBaseException(
            BaseException ex,
            WebRequest request) {
        
        String path = getRequestPath(request);
        String ipAddress = getClientIpAddress(request);
        
        // Log with appropriate level based on error type
        if (ex.getErrorCode().isSecurityError()) {
            log.warn("Security exception [{}] - Path: {}, IP: {}, Message: {}", 
                    ex.getErrorCode().name(), path, ipAddress, ex.getMessage());
        } else if (ex.getErrorCode().isServerError()) {
            log.error("Server exception [{}] - Path: {}, Message: {}", 
                    ex.getErrorCode().name(), path, ex.getMessage(), ex);
        } else {
            log.debug("Client exception [{}] - Path: {}, Message: {}", 
                    ex.getErrorCode().name(), path, ex.getMessage());
        }
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(response);
    }
    
    // ==================== Security Exceptions ====================
    
    /**
     * Handle rate limit exceeded exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 429 status
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimitExceededException(
            RateLimitExceededException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String path = getRequestPath(request);
        
        log.warn("Rate limit exceeded - IP: {}, Path: {}, Message: {}", 
                ipAddress, path, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60") // Suggest retry after 60 seconds
                .body(response);
    }
    
    /**
     * Handle account locked exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 403 status
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountLockedException(
            AccountLockedException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        
        log.warn("Account locked attempt - IP: {}, Message: {}", ipAddress, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
    
    /**
     * Handle invalid token exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 401 status
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidTokenException(
            InvalidTokenException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        
        log.warn("Invalid token - IP: {}, Message: {}", ipAddress, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle unauthorized exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 401 status
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String path = getRequestPath(request);
        
        log.warn("Unauthorized access attempt - IP: {}, Path: {}, Message: {}", 
                ipAddress, path, ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle conflict exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 409 status
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflictException(
            ConflictException ex,
            WebRequest request) {
        
        log.debug("Conflict exception - Message: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    
    /**
     * Handle validation exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 400 status
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            ValidationException ex,
            WebRequest request) {
        
        log.debug("Validation exception - Message: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle resource not found exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        log.debug("Resource not found - Message: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
    
    // ==================== Spring Validation Exceptions ====================
    
    /**
     * Handle @Valid annotation validation failures.
     * Returns detailed field-level validation errors.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.debug("Method argument validation failed - Path: {}", getRequestPath(request));
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Object> response = ApiResponse.error(
                "Validation failed for one or more fields",
                errors
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle constraint violation exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {
        
        log.debug("Constraint violation - Path: {}", getRequestPath(request));
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Object> response = ApiResponse.error(
                "Validation constraint violated",
                errors
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle missing request parameter exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 400 status
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            WebRequest request) {
        
        log.debug("Missing request parameter: {}", ex.getParameterName());
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        ApiResponse<Object> response = ApiResponse.error(message);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle method argument type mismatch exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        
        log.debug("Method argument type mismatch: {}", ex.getName());
        
        String message = String.format("Invalid value for parameter '%s'", ex.getName());
        ApiResponse<Object> response = ApiResponse.error(message);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle HTTP message not readable exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        
        log.debug("HTTP message not readable - Path: {}", getRequestPath(request));
        
        ApiResponse<Object> response = ApiResponse.error(
                "Malformed JSON request or invalid request body"
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    // ==================== Spring Security Exceptions ====================
    
    /**
     * Handle Spring Security authentication exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 401 status
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        log.warn("Authentication failed - IP: {}, Message: {}", ipAddress, ex.getMessage());
        
        String message = ex.getMessage() != null && !ex.getMessage().isEmpty() 
                ? ex.getMessage() 
                : "Authentication failed";
        
        ApiResponse<Object> response = ApiResponse.error(message);
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle bad credentials exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 401 status
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        log.warn("Bad credentials - IP: {}", ipAddress);
        
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid username or password"
        );
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle account disabled exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 403 status
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        log.warn("Disabled account login attempt - IP: {}", ipAddress);
        
        ApiResponse<Object> response = ApiResponse.error(
                "Account is disabled. Please contact support."
        );
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
    
    /**
     * Handle account locked exceptions from Spring Security.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 403 status
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLockedException(
            LockedException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        log.warn("Locked account login attempt - IP: {}", ipAddress);
        
        ApiResponse<Object> response = ApiResponse.error(
                "Account is locked. Please contact support or try again later."
        );
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
    
    /**
     * Handle access denied exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 403 status
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String path = getRequestPath(request);
        
        log.warn("Access denied - IP: {}, Path: {}", ipAddress, path);
        
        ApiResponse<Object> response = ApiResponse.error(
                "You do not have permission to access this resource"
        );
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
    
    // ==================== File Upload Exceptions ====================
    
    /**
     * Handle max upload size exceeded exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 413 status
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            WebRequest request) {
        
        log.warn("Max upload size exceeded - IP: {}", getClientIpAddress(request));
        
        ApiResponse<Object> response = ApiResponse.error(
                "File size exceeds maximum allowed size"
        );
        
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(response);
    }
    
    /**
     * Handle multipart exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 400 status
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMultipartException(
            MultipartException ex,
            WebRequest request) {
        
        log.warn("Multipart request error - IP: {}, Message: {}", 
                getClientIpAddress(request), ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid file upload request"
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    // ==================== HTTP Exceptions ====================
    
    /**
     * Handle method not allowed exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 405 status
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {
        
        log.debug("Method not allowed - Path: {}, Method: {}", 
                getRequestPath(request), ex.getMethod());
        
        String message = String.format("HTTP method '%s' is not supported for this endpoint", 
                ex.getMethod());
        ApiResponse<Object> response = ApiResponse.error(message);
        
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("Allow", String.join(", ", ex.getSupportedMethods()))
                .body(response);
    }
    
    /**
     * Handle unsupported media type exceptions.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 415 status
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request) {
        
        log.debug("Unsupported media type - Path: {}, Content-Type: {}", 
                getRequestPath(request), ex.getContentType());
        
        ApiResponse<Object> response = ApiResponse.error(
                "Content-Type is not supported. Please use application/json"
        );
        
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(response);
    }
    
    // ==================== Global Exception Handler ====================
    
    /**
     * Handle all unhandled exceptions.
     * This is the catch-all handler for unexpected errors.
     * 
     * @param ex the exception
     * @param request web request
     * @return error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        String path = getRequestPath(request);
        String ipAddress = getClientIpAddress(request);
        
        log.error("Unhandled exception - Path: {}, IP: {}, Exception: {}", 
                path, ipAddress, ex.getClass().getSimpleName(), ex);
        
        // Don't expose internal error details in production
        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected error occurred. Please try again later."
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Extract request path from WebRequest.
     * 
     * @param request web request
     * @return request path
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
            return servletRequest.getRequestURI();
        }
        return "unknown";
    }
    
    /**
     * Extract client IP address from WebRequest.
     * Checks proxy headers first, then falls back to remote address.
     * 
     * @param request web request
     * @return client IP address
     */
    private String getClientIpAddress(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
            
            String ipAddress = servletRequest.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = servletRequest.getHeader("X-Real-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = servletRequest.getRemoteAddr();
            }
            
            // X-Forwarded-For can contain multiple IPs, take the first one
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            
            return ipAddress != null ? ipAddress : "unknown";
        }
        return "unknown";
    }
}
