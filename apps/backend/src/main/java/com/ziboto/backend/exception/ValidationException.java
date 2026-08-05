package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when validation fails.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>Request data fails validation rules</li>
 *   <li>Business logic validation fails</li>
 *   <li>Data integrity constraints are violated</li>
 *   <li>Required fields are missing</li>
 * </ul>
 * 
 * <p>HTTP Status: 400 Bad Request</p>
 */
public class ValidationException extends BaseException {
    
    public ValidationException() {
        super(ErrorCode.VALIDATION_ERROR);
    }
    
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
    
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(ErrorCode.VALIDATION_ERROR, message, cause);
    }
}
