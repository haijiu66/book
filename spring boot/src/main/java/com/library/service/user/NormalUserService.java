package com.library.service.user;

import com.library.cache.RedisCacheHelper;
import com.library.entity.user.NormalUser;
import com.library.repository.user.NormalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NormalUserService {

    private static final String CACHE_USERS = "users";
    private static final String KEY_ALL = "all_v2";

    @Autowired
    private NormalUserRepository normalUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisCacheHelper cache;

    /** 查：全部用户 — 先缓存后数据库 */
    public List<NormalUser> findAll() {
        return cache.getOrLoad(CACHE_USERS, KEY_ALL, normalUserRepository::findAll);
    }

    public List<NormalUser> findByCreatedBy(Long createdBy) {
        String key = "createdBy_" + createdBy;
        return cache.getOrLoad(CACHE_USERS, key, () -> normalUserRepository.findByCreatedBy(createdBy));
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Optional<NormalUser> findById(Long id) {
        NormalUser cached = cache.get(CACHE_USERS, "id_" + id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<NormalUser> found = normalUserRepository.findById(id);
        found.ifPresent(user -> cache.put(CACHE_USERS, "id_" + id, user));
        return found;
    }

  /** 查：按用户名 — 先缓存后数据库 */
    public Optional<NormalUser> findByUsername(String username) {
        NormalUser cached = cache.get(CACHE_USERS, "name_" + username);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<NormalUser> found = normalUserRepository.findByUsername(username);
        found.ifPresent(user -> cache.put(CACHE_USERS, "name_" + username, user));
        return found;
    }

    /** 增/改：保存后同步写入缓存 */
    public NormalUser save(NormalUser normalUser) {
        boolean isNew = normalUser.getId() == null;
        if (normalUser.getPassword() != null && !normalUser.getPassword().isEmpty()
                && !normalUser.getPassword().startsWith("$2a$")) {
            normalUser.setPassword(passwordEncoder.encode(normalUser.getPassword()));
        }
        NormalUser saved = normalUserRepository.save(normalUser);
        if (isNew) {
            cacheAfterAddUser(saved);
        } else {
            cacheAfterUpdateUser(saved);
        }
        return saved;
    }

    /** 删：软删除 visible=0，并删除对应缓存 */
    public void deleteById(Long id) {
        normalUserRepository.findById(id).ifPresent(user -> {
            user.setVisible(0);
            normalUserRepository.save(user);
            cacheAfterDeleteUser(id, user.getUsername(), user.getCreatedBy());
        });
    }

    public void updateLastLoginInfo(Long id, String ip) {
        normalUserRepository.findById(id).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(ip);
            NormalUser saved = normalUserRepository.save(user);
            cacheAfterUpdateUser(saved);
        });
    }

    public boolean existsByUsername(String username) {
        return normalUserRepository.existsByUsername(username);
    }

    private void cacheAfterAddUser(NormalUser saved) {
        cache.put(CACHE_USERS, "id_" + saved.getId(), saved);
        cache.put(CACHE_USERS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_USERS, KEY_ALL);
        if (saved.getCreatedBy() != null) {
            cache.evict(CACHE_USERS, "createdBy_" + saved.getCreatedBy());
        }
    }

    private void cacheAfterUpdateUser(NormalUser saved) {
        cache.put(CACHE_USERS, "id_" + saved.getId(), saved);
        cache.put(CACHE_USERS, "name_" + saved.getUsername(), saved);
        cache.evict(CACHE_USERS, KEY_ALL);
        cache.evict(CACHE_USERS, "auth_READER_" + saved.getUsername());
        if (saved.getCreatedBy() != null) {
            cache.evict(CACHE_USERS, "createdBy_" + saved.getCreatedBy());
        }
    }

    private void cacheAfterDeleteUser(Long id, String username, Long createdBy) {
        cache.evict(CACHE_USERS, "id_" + id);
        cache.evict(CACHE_USERS, "name_" + username);
        cache.evict(CACHE_USERS, "auth_READER_" + username);
        cache.evict(CACHE_USERS, KEY_ALL);
        if (createdBy != null) {
            cache.evict(CACHE_USERS, "createdBy_" + createdBy);
        }
    }
}
