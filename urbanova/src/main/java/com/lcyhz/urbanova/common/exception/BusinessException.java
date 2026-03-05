package com.lcyhz.urbanova.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    private final Object details;

    public BusinessException(int status, String code, String message) {
        this(status, code, message, null);
    }

    public BusinessException(int status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
}

