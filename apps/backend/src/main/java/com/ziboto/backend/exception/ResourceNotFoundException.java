package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>User not found</li>
 *   <li>File not found</li>
 *   <li>Bucket not found</li>
 *   <li>Any resource lookup fails</li>
 * </ul>
 * 
 * <p>HTTP Status: 404 Not Found</p>
 */
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
    
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
    
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, cause);
    }
}
