package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when authentication fails or is required.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>Invalid credentials provided</li>
 *   <li>Authentication token is missing</li>
 *   <li>User is not authenticated</li>
 *   <li>Session has expired</li>
 * </ul>
 * 
 * <p>HTTP Status: 401 Unauthorized</p>
 */
public class UnauthorizedException extends BaseException {
    
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED_ACCESS);
    }
    
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, message);
    }
    
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, message, cause);
    }
}
