package com.library.controller.admin;

import com.library.dto.ApiResponse;
import com.library.dto.AuditUserSummary;
import com.library.entity.admin.AuditLog;
import com.library.security.annotation.RequireSuperAdmin;
import com.library.service.admin.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    @RequireSuperAdmin
    public ApiResponse<Page<AuditLog>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ApiResponse.success("获取成功", auditLogService.findAll(pageable));
    }

    @GetMapping("/search")
    @RequireSuperAdmin
    public ApiResponse<Page<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) String operatorType,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operationStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<AuditLog> result;

        if (startTime != null && endTime != null) {
            result = auditLogService.findByTimeRange(startTime, endTime, pageable);
        } else if (operatorType != null || operationType != null || operationStatus != null) {
            result = auditLogService.findByConditions(operatorType, operationType, operationStatus, pageable);
        } else {
            result = auditLogService.findAll(pageable);
        }

        return ApiResponse.success("获取成功", result);
    }

    @GetMapping("/user-summary")
    @RequireSuperAdmin
    public ApiResponse<List<AuditUserSummary>> getUserAuditSummary() {
        return ApiResponse.success("获取成功", auditLogService.getAuditUserSummaryList());
    }

    @GetMapping("/user-history")
    @RequireSuperAdmin
    public ApiResponse<Page<AuditLog>> getUserAuditHistory(
            @RequestParam String operatorType,
            @RequestParam Long operatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success("获取成功",
                auditLogService.getLogsByOperator(operatorType, operatorId, pageable));
    }

    @GetMapping("/{id}")
    @RequireSuperAdmin
    public ApiResponse<AuditLog> getAuditLogById(@PathVariable Long id) {
        return auditLogService.findById(id)
                .map(log -> ApiResponse.success("获取成功", log))
                .orElse(ApiResponse.notFound("审计日志不存在"));
    }

    @DeleteMapping("/clean-old")
    @RequireSuperAdmin
    public ApiResponse<Void> cleanOldLogs(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate) {
        auditLogService.deleteOldLogs(beforeDate);
        return ApiResponse.success("清理旧审计日志成功", null);
    }
}
