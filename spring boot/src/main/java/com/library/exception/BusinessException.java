package com.library.exception;

import lombok.Getter;

/**
 * 业务异常 — 由 GlobalExceptionHandler 统一捕获后返回 HTTP 400。
 * 其他 RuntimeException（如 NPE）由兜底 handler 返回 500，不会误归类为业务错误。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 应用级错误码，如 "BOOK_NOT_FOUND"、"INSUFFICIENT_STOCK" */
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
