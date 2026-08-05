package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when an account is locked.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>Account is locked due to failed login attempts</li>
 *   <li>Account is suspended by administrator</li>
 *   <li>Account is locked due to security policy violation</li>
 * </ul>
 * 
 * <p>HTTP Status: 403 Forbidden</p>
 */
public class AccountLockedException extends BaseException {
    
    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED);
    }
    
    public AccountLockedException(String message) {
        super(ErrorCode.ACCOUNT_LOCKED, message);
    }
    
    public AccountLockedException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public AccountLockedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public AccountLockedException(String message, Throwable cause) {
        super(ErrorCode.ACCOUNT_LOCKED, message, cause);
    }
}
