package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.entity.User;
import com.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final String CACHE_USERS = "users";
    private static final String KEY_ALL = "legacy_all";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisCacheHelper cache;

    /** 查：全部用户 — 先缓存后数据库 */
    public List<User> getAllUsers() {
        return cache.getOrLoad(CACHE_USERS, KEY_ALL, userRepository::findAll);
    }

    /** 查：按 ID — 先缓存后数据库 */
    public User getUserById(Long id) {
        return cache.getOrLoad(CACHE_USERS, "legacy_id_" + id,
                () -> userRepository.findById(id).orElse(null));
    }

    /** 查：按用户名 — 先缓存后数据库 */
    public User findByUsername(String username) {
        return cache.getOrLoad(CACHE_USERS, "legacy_name_" + username,
                () -> userRepository.findByUsername(username).orElse(null));
    }

    public User getUserByUsername(String username) {
        return findByUsername(username);
    }

    /** 增/改：保存后同步写入缓存 */
    public User save(User user) {
        boolean isNew = user.getId() == null;
        User saved = userRepository.save(user);
        if (isNew) {
            cacheAfterAddUser(saved);
        } else {
            cacheAfterUpdateUser(saved);
        }
        return saved;
    }

    /** 删：删除并清除对应缓存 */
    public void delete(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            userRepository.deleteById(id);
            cacheAfterDeleteUser(id, user.getUsername());
        });
    }

    private void cacheAfterAddUser(User saved) {
        cache.put(CACHE_USERS, "legacy_id_" + saved.getId(), saved);
        cache.put(CACHE_USERS, "legacy_name_" + saved.getUsername(), saved);
        cache.evict(CACHE_USERS, KEY_ALL);
    }

    private void cacheAfterUpdateUser(User saved) {
        cache.put(CACHE_USERS, "legacy_id_" + saved.getId(), saved);
        cache.put(CACHE_USERS, "legacy_name_" + saved.getUsername(), saved);
        cache.evict(CACHE_USERS, KEY_ALL);
    }

    private void cacheAfterDeleteUser(Long id, String username) {
        cache.evict(CACHE_USERS, "legacy_id_" + id);
        cache.evict(CACHE_USERS, "legacy_name_" + username);
        cache.evict(CACHE_USERS, KEY_ALL);
    }
}
