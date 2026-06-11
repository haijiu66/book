package com.library.service.admin;

import com.library.cache.RedisCacheHelper;
import com.library.entity.admin.SuperAdmin;
import com.library.repository.admin.SuperAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SuperAdminService {

    private static final String CACHE_SUPER_ADMINS = "superAdmins";
    private static final String CACHE_USERS = "users";
    private static final String KEY_ALL = "all";

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisCacheHelper cache;

    /** 查：全部超级管理员 — 先缓存后数据库 */
    public List<SuperAdmin> findAll() {
        return cache.getOrLoad(CACHE_SUPER_ADMINS, KEY_ALL, superAdminRepository::findAll);
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Optional<SuperAdmin> findById(Long id) {
        SuperAdmin cached = cache.get(CACHE_SUPER_ADMINS, "id_" + id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<SuperAdmin> found = superAdminRepository.findById(id);
        found.ifPresent(admin -> cache.put(CACHE_SUPER_ADMINS, "id_" + id, admin));
        return found;
    }

    /** 查：按用户名 — 先缓存后数据库 */
    public Optional<SuperAdmin> findByUsername(String username) {
        SuperAdmin cached = cache.get(CACHE_SUPER_ADMINS, "name_" + username);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<SuperAdmin> found = superAdminRepository.findByUsername(username);
        found.ifPresent(admin -> cache.put(CACHE_SUPER_ADMINS, "name_" + username, admin));
        return found;
    }

    /** 增/改：保存后同步写入缓存 */
    public SuperAdmin save(SuperAdmin superAdmin) {
        boolean isNew = superAdmin.getId() == null;
        if (superAdmin.getPassword() != null && !superAdmin.getPassword().startsWith("$2a$")) {
            superAdmin.setPassword(passwordEncoder.encode(superAdmin.getPassword()));
        }
        SuperAdmin saved = superAdminRepository.save(superAdmin);
        if (isNew) {
            cacheAfterAddSuperAdmin(saved);
        } else {
            cacheAfterUpdateSuperAdmin(saved);
        }
        return saved;
    }

    /** 删：物理删除并清除对应缓存 */
    public void deleteById(Long id) {
        superAdminRepository.findById(id).ifPresent(admin -> {
            superAdminRepository.deleteById(id);
            cacheAfterDeleteSuperAdmin(id, admin.getUsername());
        });
    }

    /** 改：更新登录信息后同步缓存 */
    public void updateLastLoginInfo(Long id, String ip) {
        superAdminRepository.findById(id).ifPresent(admin -> {
            admin.setLastLoginTime(LocalDateTime.now());
            admin.setLastLoginIp(ip);
            SuperAdmin saved = superAdminRepository.save(admin);
            cacheAfterUpdateSuperAdmin(saved);
        });
    }

    public boolean existsByUsername(String username) {
        return superAdminRepository.existsByUsername(username);
    }

    private void cacheAfterAddSuperAdmin(SuperAdmin saved) {
        cache.put(CACHE_SUPER_ADMINS, "id_" + saved.getId(), saved);
        cache.put(CACHE_SUPER_ADMINS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_SUPER_ADMINS, KEY_ALL);
    }

    private void cacheAfterUpdateSuperAdmin(SuperAdmin saved) {
        cache.put(CACHE_SUPER_ADMINS, "id_" + saved.getId(), saved);
        cache.put(CACHE_SUPER_ADMINS, "name_" + saved.getUsername(), saved);
        cache.put(CACHE_USERS, "auth_SUPER_ADMIN_" + saved.getUsername(), saved);
        cache.evict(CACHE_SUPER_ADMINS, KEY_ALL);
    }

    private void cacheAfterDeleteSuperAdmin(Long id, String username) {
        cache.evict(CACHE_SUPER_ADMINS, "id_" + id);
        cache.evict(CACHE_SUPER_ADMINS, "name_" + username);
        cache.evict(CACHE_USERS, "auth_SUPER_ADMIN_" + username);
        cache.evict(CACHE_SUPER_ADMINS, KEY_ALL);
    }
}
