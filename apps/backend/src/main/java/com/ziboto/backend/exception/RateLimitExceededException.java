package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when a rate limit is exceeded.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>Login attempts exceed the configured threshold</li>
 *   <li>Signup attempts exceed the configured threshold</li>
 *   <li>API requests exceed the rate limit</li>
 *   <li>Token refresh attempts exceed the limit</li>
 * </ul>
 * 
 * <p>HTTP Status: 429 Too Many Requests</p>
 */
public class RateLimitExceededException extends BaseException {
    
    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
    
    public RateLimitExceededException(String message) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message);
    }
    
    public RateLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public RateLimitExceededException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, message, cause);
    }
}
