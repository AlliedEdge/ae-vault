package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when a resource conflict occurs.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>Attempting to create a resource that already exists</li>
 *   <li>Username or email already taken</li>
 *   <li>Concurrent modification detected</li>
 *   <li>Resource state conflict</li>
 * </ul>
 * 
 * <p>HTTP Status: 409 Conflict</p>
 */
public class ConflictException extends BaseException {
    
    public ConflictException() {
        super(ErrorCode.CONFLICT);
    }
    
    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
    
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(ErrorCode.CONFLICT, message, cause);
    }
}
