package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when a token is invalid.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>JWT token signature is invalid</li>
 *   <li>JWT token format is malformed</li>
 *   <li>JWT token has expired</li>
 *   <li>Token has been revoked or blacklisted</li>
 *   <li>Refresh token is invalid</li>
 * </ul>
 * 
 * <p>HTTP Status: 401 Unauthorized</p>
 */
public class InvalidTokenException extends BaseException {
    
    public InvalidTokenException() {
        super(ErrorCode.TOKEN_INVALID);
    }
    
    public InvalidTokenException(String message) {
        super(ErrorCode.TOKEN_INVALID, message);
    }
    
    public InvalidTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public InvalidTokenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(ErrorCode.TOKEN_INVALID, message, cause);
    }
}
