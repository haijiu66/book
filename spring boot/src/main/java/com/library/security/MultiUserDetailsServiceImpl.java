package com.library.security;

import com.library.cache.RedisCacheHelper;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.SuperAdmin;
import com.library.entity.user.NormalUser;
import com.library.repository.admin.AdminUserRepository;
import com.library.repository.admin.SuperAdminRepository;
import com.library.repository.user.NormalUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Primary
public class MultiUserDetailsServiceImpl implements UserDetailsService {

    private static final String CACHE_USERS = "users";

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private NormalUserRepository normalUserRepository;

    @Autowired
    private RedisCacheHelper cache;

    /** 查：用户认证信息 — 先缓存后数据库，按选定角色只查对应表 */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String selectedRole = LoginRoleContext.get();

        // 缓存 key 包含角色，防止同名不同角色的用户互相覆盖缓存
        String cacheKey = (selectedRole != null && !selectedRole.isBlank())
                ? "auth_" + selectedRole + "_" + username
                : "auth__" + username;

        UserDetails cached = cache.get(CACHE_USERS, cacheKey);
        if (cached != null) {
            return cached;
        }

        log.info("查库加载用户: username={}, selectedRole={}", username, selectedRole);

        // 按选定角色只查对应表，不查其他表
        if ("READER".equals(selectedRole)) {
            Optional<NormalUser> normalUser = normalUserRepository.findByUsername(username);
            if (normalUser.isPresent()) {
                log.info("找到普通用户: username={}", username);
                cache.put(CACHE_USERS, cacheKey, normalUser.get());
                return normalUser.get();
            }
            log.warn("普通用户未找到: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if ("ADMIN".equals(selectedRole)) {
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
            if (adminUser.isPresent()) {
                log.info("找到管理员: username={}", username);
                cache.put(CACHE_USERS, cacheKey, adminUser.get());
                return adminUser.get();
            }
            log.warn("管理员未找到: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if ("SUPER_ADMIN".equals(selectedRole)) {
            Optional<SuperAdmin> superAdmin = superAdminRepository.findByUsername(username);
            if (superAdmin.isPresent()) {
                log.info("找到超级管理员: username={}", username);
                cache.put(CACHE_USERS, cacheKey, superAdmin.get());
                return superAdmin.get();
            }
            log.warn("超级管理员未找到: username={}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 无角色指定时，按默认优先级搜索所有表（兼容旧调用方）
        Optional<SuperAdmin> superAdmin = superAdminRepository.findByUsername(username);
        if (superAdmin.isPresent()) {
            log.info("找到超级管理员: username={}", username);
            cache.put(CACHE_USERS, cacheKey, superAdmin.get());
            return superAdmin.get();
        }

        Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
        if (adminUser.isPresent()) {
            log.info("找到管理员: username={}", username);
            cache.put(CACHE_USERS, cacheKey, adminUser.get());
            return adminUser.get();
        }

        Optional<NormalUser> normalUser = normalUserRepository.findByUsername(username);
        if (normalUser.isPresent()) {
            log.info("找到普通用户: username={}", username);
            cache.put(CACHE_USERS, cacheKey, normalUser.get());
            return normalUser.get();
        }

        log.warn("用户未找到: username={}", username);
        throw new UsernameNotFoundException("用户不存在: " + username);
    }
}
