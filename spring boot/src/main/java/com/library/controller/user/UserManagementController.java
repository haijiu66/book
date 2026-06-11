package com.library.controller.user;

import com.library.dto.ApiResponse;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import com.library.entity.user.NormalUser;
import com.library.repository.admin.AdminUserRepository;
import com.library.repository.admin.SuperAdminRepository;
import com.library.security.annotation.AuditLog;
import com.library.security.annotation.RequireAdmin;
import com.library.service.isolation.TransactionalIsolationService;
import com.library.service.user.NormalUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user-management")
@CrossOrigin(origins = "*")
public class UserManagementController {

    @Autowired
    private NormalUserService normalUserService;

    @Autowired
    private TransactionalIsolationService transactionalIsolationService;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @GetMapping
    @RequireAdmin
    public ApiResponse<List<NormalUser>> getAllUsers(Authentication authentication) {
        // 管理员与超级管理员都可查看全部普通用户
        return ApiResponse.success("获取成功", normalUserService.findAll());
    }

    @GetMapping("/{id}")
    @RequireAdmin
    public ApiResponse<NormalUser> getUserById(@PathVariable Long id) {
        return normalUserService.findById(id)
                .map(user -> ApiResponse.success("获取成功", user))
                .orElse(ApiResponse.notFound("用户不存在"));
    }

    @PostMapping
    @RequireAdmin
    @AuditLog(operationType = "CREATE_USER", targetType = "USER", description = "创建用户")
    public ApiResponse<NormalUser> createUser(@RequestBody NormalUser normalUser, Authentication authentication) {
        // 跨三表检查用户名唯一性，防止与管理员或超级管理员重名
        if (normalUserService.existsByUsername(normalUser.getUsername())
                || adminUserRepository.existsByUsername(normalUser.getUsername())
                || superAdminRepository.existsByUsername(normalUser.getUsername())) {
            return ApiResponse.conflict("用户名已存在");
        }

        Object principal = authentication.getPrincipal();
        Long creatorId;
        String creatorType;

        if (principal instanceof SuperAdmin) {
            creatorId = ((SuperAdmin) principal).getId();
            creatorType = "SUPER_ADMIN";
        } else {
            creatorId = ((AdminUser) principal).getId();
            creatorType = "ADMIN";
        }

        NormalUser createdUser = transactionalIsolationService.createUserWithTables(
                normalUser, creatorId, creatorType);
        return ApiResponse.success("创建成功", createdUser);
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "UPDATE_USER", targetType = "USER", description = "更新用户信息")
    public ApiResponse<NormalUser> updateUser(@PathVariable Long id, @RequestBody NormalUser updateRequest) {
        return normalUserService.findById(id)
                .map(existingUser -> {
                    if (updateRequest.getName() != null) existingUser.setName(updateRequest.getName());
                    if (updateRequest.getPhone() != null) existingUser.setPhone(updateRequest.getPhone());
                    if (updateRequest.getEmail() != null) existingUser.setEmail(updateRequest.getEmail());
                    if (updateRequest.getStatus() != null) existingUser.setStatus(updateRequest.getStatus());
                    if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
                        existingUser.setPassword(updateRequest.getPassword());
                    }
                    return ApiResponse.success("更新成功", normalUserService.save(existingUser));
                })
                .orElse(ApiResponse.notFound("用户不存在"));
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @AuditLog(operationType = "DELETE_USER", targetType = "USER", description = "删除用户")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        if (!normalUserService.findById(id).isPresent()) {
            return ApiResponse.notFound("用户不存在");
        }
        transactionalIsolationService.deleteUserWithTables(id);
        return ApiResponse.success("删除成功", null);
    }
}
