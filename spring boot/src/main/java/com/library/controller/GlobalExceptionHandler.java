package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 统一打印日志并返回 ApiResponse
 * Controller 层不再需要 try-catch，异常全部抛到这里统一处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 认证相关异常（登录失败等）— 401 */
    @ExceptionHandler(BadCredentialsException.class)
    public ApiResponse<Void> handleBadCredentials(BadCredentialsException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ApiResponse.error(401, e.getMessage() != null ? e.getMessage() : "用户名或密码错误");
    }

    @ExceptionHandler(LockedException.class)
    public ApiResponse<Void> handleLocked(LockedException e) {
        log.warn("账号已锁定: {}", e.getMessage());
        return ApiResponse.error(403, "账号已锁定");
    }

    @ExceptionHandler(DisabledException.class)
    public ApiResponse<Void> handleDisabled(DisabledException e) {
        log.warn("账号已禁用: {}", e.getMessage());
        return ApiResponse.error(403, "账号已禁用");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> handleAuth(AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage());
        return ApiResponse.error(401, e.getMessage());
    }

    /** 权限不足 — 403 */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return ApiResponse.forbidden("权限不足");
    }

    /** 业务异常 — 400 */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        if (e.getErrorCode() != null) {
            return ApiResponse.error(e.getErrorCode(), 400, e.getMessage());
        }
        return ApiResponse.badRequest(e.getMessage());
    }

    /** 兜底 — 500 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return ApiResponse.error("服务器内部错误");
    }
}
