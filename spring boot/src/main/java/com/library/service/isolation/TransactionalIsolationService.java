package com.library.service.isolation;

import com.library.cache.RedisCacheHelper;
import com.library.entity.admin.AdminUser;
import com.library.entity.user.NormalUser;
import com.library.repository.admin.AdminUserRepository;
import com.library.repository.user.NormalUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TransactionalIsolationService {

    private static final String CACHE_ADMINS = "admins";
    private static final String CACHE_USERS = "users";
    private static final String KEY_ADMINS_ALL = "all";
    private static final String KEY_USERS_ALL = "all_v2";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private NormalUserRepository normalUserRepository;

    @Autowired
    private DynamicTableService dynamicTableService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisCacheHelper cache;

    /** 增：创建管理员后同步写入缓存 */
    @Transactional(transactionManager = "chainedTransactionManager", rollbackFor = Exception.class)
    public AdminUser createAdminWithTables(AdminUser adminUser, Long createdBy) {
        log.info("开始创建管理员: username={}", adminUser.getUsername());

        adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword()));
        adminUser.setCreatedBy(createdBy);
        adminUser.setStatus("ACTIVE");
        adminUser.setVisible(1);
        adminUser.setTableName("");
        if (adminUser.getPermissions() == null) {
            adminUser.setPermissions("");
        }

        AdminUser savedAdmin = adminUserRepository.save(adminUser);
        savedAdmin.setTableName("admin_" + savedAdmin.getId());
        savedAdmin = adminUserRepository.save(savedAdmin);

        dynamicTableService.createAdminTables(savedAdmin.getId());

        cacheAfterAddAdmin(savedAdmin);
        log.info("管理员创建成功: adminId={}", savedAdmin.getId());
        return savedAdmin;
    }

    /** 删：软删除管理员并清除对应缓存 */
    @Transactional(transactionManager = "chainedTransactionManager", rollbackFor = Exception.class)
    public void deleteAdminWithTables(Long adminId) {
        log.info("开始软删除管理员: adminId={}", adminId);

        adminUserRepository.findById(adminId).ifPresent(admin -> {
            admin.setVisible(0);
            adminUserRepository.save(admin);
            cacheAfterDeleteAdmin(adminId, admin.getUsername(), admin.getCreatedBy());
        });

        log.info("管理员软删除成功: adminId={}", adminId);
    }

    /** 增：创建用户后同步写入缓存 */
    @Transactional(transactionManager = "chainedTransactionManager", rollbackFor = Exception.class)
    public NormalUser createUserWithTables(NormalUser normalUser, Long createdBy, String creatorType) {
        log.info("开始创建用户: username={}", normalUser.getUsername());

        normalUser.setPassword(passwordEncoder.encode(normalUser.getPassword()));
        normalUser.setCreatedBy(createdBy);
        normalUser.setCreatorType(creatorType);
        normalUser.setStatus("ACTIVE");
        normalUser.setVisible(1);

        NormalUser savedUser = normalUserRepository.save(normalUser);
        savedUser.setTableName("user_" + savedUser.getId());
        savedUser = normalUserRepository.save(savedUser);

        dynamicTableService.createUserTables(savedUser.getId());

        cacheAfterAddUser(savedUser);
        log.info("用户创建成功并创建专属表: userId={}", savedUser.getId());
        return savedUser;
    }

    /** 删：软删除用户并清除对应缓存 */
    @Transactional(transactionManager = "chainedTransactionManager", rollbackFor = Exception.class)
    public void deleteUserWithTables(Long userId) {
        log.info("开始软删除用户: userId={}", userId);

        normalUserRepository.findById(userId).ifPresent(user -> {
            user.setVisible(0);
            normalUserRepository.save(user);
            cacheAfterDeleteUser(userId, user.getUsername(), user.getCreatedBy());
        });

        log.info("用户软删除成功: userId={}", userId);
    }

    private void cacheAfterAddAdmin(AdminUser saved) {
        cache.put(CACHE_ADMINS, "id_" + saved.getId(), saved);
        cache.put(CACHE_ADMINS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_ADMINS, KEY_ADMINS_ALL);
        if (saved.getCreatedBy() != null) {
            cache.evict(CACHE_ADMINS, "createdBy_" + saved.getCreatedBy());
        }
    }

    private void cacheAfterDeleteAdmin(Long id, String username, Long createdBy) {
        cache.evict(CACHE_ADMINS, "id_" + id);
        cache.evict(CACHE_ADMINS, "name_" + username);
        cache.evict(CACHE_USERS, "auth_" + username);
        cache.evict(CACHE_ADMINS, KEY_ADMINS_ALL);
        if (createdBy != null) {
            cache.evict(CACHE_ADMINS, "createdBy_" + createdBy);
        }
    }

    private void cacheAfterAddUser(NormalUser saved) {
        cache.put(CACHE_USERS, "id_" + saved.getId(), saved);
        cache.put(CACHE_USERS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_USERS, KEY_USERS_ALL);
        if (saved.getCreatedBy() != null) {
            cache.evict(CACHE_USERS, "createdBy_" + saved.getCreatedBy());
        }
    }

    private void cacheAfterDeleteUser(Long id, String username, Long createdBy) {
        cache.evict(CACHE_USERS, "id_" + id);
        cache.evict(CACHE_USERS, "name_" + username);
        cache.evict(CACHE_USERS, "auth_" + username);
        cache.evict(CACHE_USERS, KEY_USERS_ALL);
        if (createdBy != null) {
            cache.evict(CACHE_USERS, "createdBy_" + createdBy);
        }
    }
}
