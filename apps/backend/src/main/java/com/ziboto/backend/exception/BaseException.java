package com.ziboto.backend.exception;

import com.ziboto.backend.common.constant.ErrorCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Object[] args;
    
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public BaseException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }
}
