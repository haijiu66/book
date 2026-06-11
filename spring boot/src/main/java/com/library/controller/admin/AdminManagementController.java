package com.library.controller.admin;

import com.library.dto.ApiResponse;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import com.library.repository.admin.AuditLogRepository;
import com.library.repository.admin.LoginLogRepository;
import com.library.repository.admin.SuperAdminRepository;
import com.library.repository.user.NormalUserRepository;
import com.library.security.annotation.AuditLog;
import com.library.security.annotation.RequireSuperAdmin;
import com.library.service.LoginLogService;
import com.library.service.admin.AdminUserService;
import com.library.service.isolation.TransactionalIsolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/admin-management")
@CrossOrigin(origins = "*")
public class AdminManagementController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private TransactionalIsolationService transactionalIsolationService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private NormalUserRepository normalUserRepository;

    @GetMapping("/dashboard-stats")
    @RequireSuperAdmin
    public ApiResponse<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("adminCount", adminUserService.findAll().size());
        stats.put("auditCount", auditLogRepository.count());
        stats.put("todayLogins", loginLogRepository
                .findByCreateTimeBetween(
                    LocalDate.now().atStartOfDay(),
                    LocalDate.now().plusDays(1).atStartOfDay())
                .size());
        stats.put("onlineUsers", loginLogService.getUserLoginSummaryList().size());
        return ApiResponse.success("获取成功", stats);
    }

    @GetMapping
    @RequireSuperAdmin
    public ApiResponse<List<AdminUser>> getAllAdmins() {
        return ApiResponse.success("获取成功", adminUserService.findAll());
    }

    @GetMapping("/{id}")
    @RequireSuperAdmin
    public ApiResponse<AdminUser> getAdminById(@PathVariable Long id) {
        return adminUserService.findById(id)
                .map(admin -> ApiResponse.success("获取成功", admin))
                .orElse(ApiResponse.notFound("管理员不存在"));
    }

    @PostMapping
    @RequireSuperAdmin
    @AuditLog(operationType = "CREATE_ADMIN", targetType = "ADMIN", description = "创建管理员")
    public ApiResponse<AdminUser> createAdmin(@RequestBody AdminUser adminUser, Authentication authentication) {
        // 跨三表检查用户名唯一性，防止与普通用户或超级管理员重名
        if (adminUserService.existsByUsername(adminUser.getUsername())
                || superAdminRepository.existsByUsername(adminUser.getUsername())
                || normalUserRepository.existsByUsername(adminUser.getUsername())) {
            return ApiResponse.conflict("用户名已存在");
        }

        SuperAdmin currentAdmin = (SuperAdmin) authentication.getPrincipal();
        AdminUser createdAdmin = transactionalIsolationService.createAdminWithTables(
                adminUser, currentAdmin.getId());
        return ApiResponse.success("创建成功", createdAdmin);
    }

    @PutMapping("/{id}")
    @RequireSuperAdmin
    @AuditLog(operationType = "UPDATE_ADMIN", targetType = "ADMIN", description = "更新管理员信息")
    public ApiResponse<AdminUser> updateAdmin(@PathVariable Long id, @RequestBody AdminUser adminUser) {
        return adminUserService.findById(id)
                .map(existingAdmin -> {
                    if (adminUser.getName() != null) existingAdmin.setName(adminUser.getName());
                    if (adminUser.getPhone() != null) existingAdmin.setPhone(adminUser.getPhone());
                    if (adminUser.getEmail() != null) existingAdmin.setEmail(adminUser.getEmail());
                    if (adminUser.getStatus() != null) existingAdmin.setStatus(adminUser.getStatus());
                    if (adminUser.getPermissions() != null) existingAdmin.setPermissions(adminUser.getPermissions());
                    if (adminUser.getPassword() != null && !adminUser.getPassword().isEmpty()) {
                        existingAdmin.setPassword(adminUser.getPassword());
                    }
                    return ApiResponse.success("更新成功", adminUserService.save(existingAdmin));
                })
                .orElse(ApiResponse.notFound("管理员不存在"));
    }

    @DeleteMapping("/{id}")
    @RequireSuperAdmin
    @AuditLog(operationType = "DELETE_ADMIN", targetType = "ADMIN", description = "删除管理员")
    public ApiResponse<Void> deleteAdmin(@PathVariable Long id) {
        if (!adminUserService.findById(id).isPresent()) {
            return ApiResponse.notFound("管理员不存在");
        }
        transactionalIsolationService.deleteAdminWithTables(id);
        return ApiResponse.success("删除成功", null);
    }
}
