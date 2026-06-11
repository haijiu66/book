package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.security.annotation.*;
import com.library.security.enums.OperationType;
import com.library.security.enums.Role;
import com.library.security.enums.TargetType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限注解示例控制器
 * 展示项目中所有权限注解和审计日志注解的使用方式
 */
@RestController
@RequestMapping("/api/examples")
public class AnnotationExampleController {

    // ==================== 权限注解示例 ====================

    /**
     * 示例1: @RequireSuperAdmin - 仅超级管理员可访问
     */
    @GetMapping("/super-admin")
    @RequireSuperAdmin
    @AuditLog(operation = OperationType.QUERY, target = TargetType.AUDIT_LOG, description = "超级管理员示例接口")
    public ApiResponse<Map<String, Object>> superAdminExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "只有超级管理员可以访问此接口");
        data.put("role", "SUPER_ADMIN");
        return ApiResponse.success("访问成功", data);
    }

    /**
     * 示例2: @RequireAdmin - 管理员和超级管理员可访问
     */
    @GetMapping("/admin")
    @RequireAdmin
    @AuditLog(operation = OperationType.QUERY, target = TargetType.ADMIN, description = "管理员示例接口")
    public ApiResponse<Map<String, Object>> adminExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "管理员和超级管理员都可以访问此接口");
        data.put("roles", new String[]{"SUPER_ADMIN", "ADMIN"});
        return ApiResponse.success("访问成功", data);
    }

    /**
     * 示例3: @RequireReader - 仅读者可访问
     */
    @GetMapping("/reader")
    @RequireReader
    @AuditLog(operation = OperationType.QUERY, target = TargetType.USER, description = "读者示例接口")
    public ApiResponse<Map<String, Object>> readerExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "只有读者可以访问此接口");
        data.put("role", "READER");
        return ApiResponse.success("访问成功", data);
    }

    /**
     * 示例4: @RequireRole - 自定义角色（字符串方式）
     */
    @GetMapping("/custom-role")
    @RequireRole(roles = {"SUPER_ADMIN", "READER"})
    @AuditLog(operation = OperationType.QUERY, description = "自定义角色示例接口")
    public ApiResponse<Map<String, Object>> customRoleExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "超级管理员和读者可以访问此接口");
        data.put("roles", new String[]{"SUPER_ADMIN", "READER"});
        return ApiResponse.success("访问成功", data);
    }

    /**
     * 示例5: @RequireRoles - 自定义角色（枚举方式，推荐）
     */
    @GetMapping("/custom-roles")
    @RequireRoles(roles = {Role.SUPER_ADMIN, Role.READER})
    @AuditLog(operation = OperationType.QUERY, description = "自定义角色枚举示例接口")
    public ApiResponse<Map<String, Object>> customRolesExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "超级管理员和读者可以访问此接口（使用枚举方式）");
        data.put("roles", new Role[]{Role.SUPER_ADMIN, Role.READER});
        return ApiResponse.success("访问成功", data);
    }

    /**
     * 示例6: @RequireLogin - 仅需登录
     */
    @GetMapping("/login-required")
    @RequireLogin
    @AuditLog(operation = OperationType.QUERY, description = "登录示例接口")
    public ApiResponse<Map<String, Object>> loginRequiredExample() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "只要登录就可以访问此接口");
        data.put("note", "无需特定角色");
        return ApiResponse.success("访问成功", data);
    }

    // ==================== 审计日志注解示例 ====================

    /**
     * 示例7: @AuditLog - 枚举方式（推荐）
     */
    @PostMapping("/audit-enum")
    @RequireAdmin
    @AuditLog(
        operation = OperationType.CREATE_BOOK,
        target = TargetType.BOOK,
        description = "创建图书（枚举方式）"
    )
    public ApiResponse<Map<String, Object>> auditEnumExample(@RequestBody Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "审计日志使用枚举方式记录");
        data.put("request", request);
        return ApiResponse.success("操作成功", data);
    }

    /**
     * 示例8: @AuditLog - 字符串方式（向后兼容）
     */
    @PostMapping("/audit-string")
    @RequireAdmin
    @AuditLog(
        operationType = "CREATE_BOOK",
        targetType = "BOOK",
        description = "创建图书（字符串方式）"
    )
    public ApiResponse<Map<String, Object>> auditStringExample(@RequestBody Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "审计日志使用字符串方式记录");
        data.put("request", request);
        return ApiResponse.success("操作成功", data);
    }

    // ==================== 综合示例 ====================

    /**
     * 示例9: 组合使用多个注解
     */
    @PostMapping("/combined")
    @RequireSuperAdmin
    @AuditLog(
        operation = OperationType.CREATE_ADMIN,
        target = TargetType.ADMIN,
        description = "创建管理员（综合示例）"
    )
    public ApiResponse<Map<String, Object>> combinedExample(@RequestBody Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "组合使用权限注解和审计日志注解");
        data.put("permission", "RequireSuperAdmin");
        data.put("audit", "AuditLog with enum");
        data.put("request", request);
        return ApiResponse.success("操作成功", data);
    }
}
