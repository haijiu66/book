package com.library.controller.admin;

import com.library.dto.ApiResponse;
import com.library.dto.UserLoginSummary;
import com.library.entity.admin.LoginLog;
import com.library.security.annotation.RequireSuperAdmin;
import com.library.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/login-logs")
@CrossOrigin(origins = "*")
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @GetMapping
    @RequireSuperAdmin
    public ApiResponse<Page<LoginLog>> getAllLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ApiResponse.success("获取成功", loginLogService.getLogsPaginated(pageable));
    }

    @GetMapping("/{id}")
    @RequireSuperAdmin
    public ApiResponse<LoginLog> getLoginLogById(@PathVariable Long id) {
        LoginLog log = loginLogService.getLogById(id);
        if (log != null) {
            return ApiResponse.success("获取成功", log);
        }
        return ApiResponse.notFound("登录日志不存在");
    }

    @GetMapping("/search")
    @RequireSuperAdmin
    public ApiResponse<Page<LoginLog>> searchLoginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String loginStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        if (username != null) {
            return ApiResponse.success("获取成功", loginLogService.getLogsByUsernamePaginated(username, pageable));
        }
        
        return ApiResponse.success("获取成功", loginLogService.getLogsPaginated(pageable));
    }

    @GetMapping("/user-summary")
    @RequireSuperAdmin
    public ApiResponse<List<UserLoginSummary>> getUserLoginSummary() {
        return ApiResponse.success("获取成功", loginLogService.getUserLoginSummaryList());
    }

    @GetMapping("/user-history")
    @RequireSuperAdmin
    public ApiResponse<Page<LoginLog>> getUserLoginHistory(
            @RequestParam Long userId,
            @RequestParam String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return ApiResponse.success("获取成功",
                loginLogService.getLogsByUser(userId, userType, pageable));
    }

    @DeleteMapping("/{id}")
    @RequireSuperAdmin
    public ApiResponse<Void> deleteLoginLog(@PathVariable Long id) {
        loginLogService.deleteLog(id);
        return ApiResponse.success("删除成功", null);
    }

    @DeleteMapping("/batch")
    @RequireSuperAdmin
    public ApiResponse<Void> batchDeleteLoginLogs(@RequestBody Object requestBody) {
        List<Long> ids = extractIds(requestBody);
        if (ids.isEmpty()) {
            return ApiResponse.badRequest("待删除日志ID不能为空");
        }
        for (Long id : ids) {
            loginLogService.deleteLog(id);
        }
        return ApiResponse.success("批量删除成功", null);
    }

    @DeleteMapping("/clean-old")
    @RequireSuperAdmin
    public ApiResponse<Void> cleanOldLogs(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate) {
        loginLogService.deleteOldLogs(beforeDate);
        return ApiResponse.success("清理旧日志成功", null);
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractIds(Object requestBody) {
        if (requestBody instanceof List<?> rawList) {
            List<Long> ids = new ArrayList<>();
            for (Object item : rawList) {
                ids.add(Long.valueOf(String.valueOf(item)));
            }
            return ids;
        }

        if (requestBody instanceof Map<?, ?> rawMap) {
            Object idsObj = rawMap.get("ids");
            if (idsObj instanceof List<?> rawList) {
                List<Long> ids = new ArrayList<>();
                for (Object item : rawList) {
                    ids.add(Long.valueOf(String.valueOf(item)));
                }
                return ids;
            }
        }

        return List.of();
    }
}
