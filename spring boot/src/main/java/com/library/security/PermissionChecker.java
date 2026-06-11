package com.library.security;

import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("permissionChecker")
public class PermissionChecker {

    /**
     * 检查当前用户是否拥有指定权限。
     * SuperAdmin 拥有所有权限，AdminUser 需要显式分配权限。
     */
    public boolean hasPermission(String permissionCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof SuperAdmin) {
            return true;
        }

        if (principal instanceof AdminUser adminUser) {
            return adminUser.hasPermission(permissionCode);
        }

        return false;
    }
}
