package com.library.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String errorCode;
    private Boolean success;
    private String message;
    private T data;

    public ApiResponse(Integer code, Boolean success, String message, T data) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, true, "操作成功", data);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, true, message, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, false, message, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, Integer httpCode, String message) {
        ApiResponse<T> response = new ApiResponse<>(httpCode, false, message, null);
        response.setErrorCode(errorCode);
        return response;
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, false, message, null);
    }

    /** 参数校验错误 — 400 */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, false, message, null);
    }

    /** 权限不足 — 403 */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, false, message, null);
    }

    /** 资源不存在 — 404 */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, false, message, null);
    }

    /** 业务冲突 — 409（如重复操作、状态不符） */
    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, false, message, null);
    }
}
