package com.library.service.admin;

import com.library.cache.RedisCacheHelper;
import com.library.entity.admin.AdminUser;
import com.library.repository.admin.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {

    private static final String CACHE_ADMINS = "admins";
    private static final String CACHE_USERS = "users";
    private static final String KEY_ALL = "all";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisCacheHelper cache;

    /** 查：全部管理员 — 先缓存后数据库 */
    public List<AdminUser> findAll() {
        return cache.getOrLoad(CACHE_ADMINS, KEY_ALL, adminUserRepository::findAll);
    }

    public List<AdminUser> findByCreatedBy(Long createdBy) {
        String key = "createdBy_" + createdBy;
        return cache.getOrLoad(CACHE_ADMINS, key, () -> adminUserRepository.findByCreatedBy(createdBy));
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Optional<AdminUser> findById(Long id) {
        AdminUser cached = cache.get(CACHE_ADMINS, "id_" + id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<AdminUser> found = adminUserRepository.findById(id);
        found.ifPresent(admin -> cache.put(CACHE_ADMINS, "id_" + id, admin));
        return found;
    }

    /** 查：按用户名 — 先缓存后数据库 */
    public Optional<AdminUser> findByUsername(String username) {
        AdminUser cached = cache.get(CACHE_ADMINS, "name_" + username);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<AdminUser> found = adminUserRepository.findByUsername(username);
        found.ifPresent(admin -> cache.put(CACHE_ADMINS, "name_" + username, admin));
        return found;
    }

    /** 增/改：保存后同步写入缓存 */
    public AdminUser save(AdminUser adminUser) {
        boolean isNew = adminUser.getId() == null;
        if (adminUser.getPassword() != null && !adminUser.getPassword().isEmpty()
                && !adminUser.getPassword().startsWith("$2a$")) {
            adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword()));
        }
        AdminUser saved = adminUserRepository.save(adminUser);
        if (isNew) {
            cacheAfterAddAdmin(saved);
        } else {
            cacheAfterUpdateAdmin(saved);
        }
        return saved;
    }

    /** 删：软删除 visible=0，并删除对应缓存 */
    public void deleteById(Long id) {
        adminUserRepository.findById(id).ifPresent(admin -> {
            admin.setVisible(0);
            adminUserRepository.save(admin);
            cacheAfterDeleteAdmin(id, admin.getUsername(), admin.getCreatedBy());
        });
    }

    public void updateLastLoginInfo(Long id, String ip) {
        adminUserRepository.findById(id).ifPresent(admin -> {
            admin.setLastLoginTime(LocalDateTime.now());
            admin.setLastLoginIp(ip);
            AdminUser saved = adminUserRepository.save(admin);
            cacheAfterUpdateAdmin(saved);
        });
    }

    public boolean existsByUsername(String username) {
        return adminUserRepository.existsByUsername(username);
    }

    private void cacheAfterAddAdmin(AdminUser saved) {
        cache.put(CACHE_ADMINS, "id_" + saved.getId(), saved);
        cache.put(CACHE_ADMINS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_ADMINS, KEY_ALL);
        if (saved.getCreatedBy() != null) {
            cache.evict(CACHE_ADMINS, "createdBy_" + saved.getCreatedBy());
        }
    }

    private void cacheAfterUpdateAdmin(AdminUser saved) {
        cache.put(CACHE_ADMINS, "id_" + saved.getId(), saved);
        cache.put(CACHE_ADMINS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_ADMINS, KEY_ALL);
        cache.evict(CACHE_USERS, "auth_ADMIN_" + saved.getUsername());
        if (saved.getCreatedBy() != null) {
            cache.evict(CACHE_ADMINS, "createdBy_" + saved.getCreatedBy());
        }
    }

    private void cacheAfterDeleteAdmin(Long id, String username, Long createdBy) {
        cache.evict(CACHE_ADMINS, "id_" + id);
        cache.evict(CACHE_ADMINS, "name_" + username);
        cache.evict(CACHE_USERS, "auth_ADMIN_" + username);
        cache.evict(CACHE_ADMINS, KEY_ALL);
        if (createdBy != null) {
            cache.evict(CACHE_ADMINS, "createdBy_" + createdBy);
        }
    }
}
