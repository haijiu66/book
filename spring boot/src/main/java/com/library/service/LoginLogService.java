package com.library.service;

import com.library.cache.RedisCacheHelper;
import com.library.dto.UserLoginSummary;
import com.library.entity.admin.AdminUser;
import com.library.entity.admin.LoginLog;
import com.library.entity.admin.SuperAdmin;
import com.library.entity.user.NormalUser;
import com.library.repository.admin.AdminUserRepository;
import com.library.repository.admin.LoginLogRepository;
import com.library.repository.admin.SuperAdminRepository;
import com.library.repository.user.NormalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoginLogService {

    private static final String CACHE_LOGIN_LOGS = "loginLogs";
    private static final String KEY_SUMMARY = "user_summary";

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private NormalUserRepository normalUserRepository;

    @Autowired
    private RedisCacheHelper cache;

    /** 增：记录登录成功，写入后失效列表缓存 */
    public void recordLoginSuccess(Long userId, String username, String userType,
                                   String ipAddress, String userAgent) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType != null ? userType : "UNKNOWN");
        log.setStatus("SUCCESS");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        LoginLog saved = loginLogRepository.save(log);
        cacheAfterAddLog(saved);
    }

    /** 增：记录登录失败，写入后失效列表缓存 */
    public void recordLoginFail(Long userId, String username, String userType,
                               String ipAddress, String userAgent, String failReason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setUserType(userType != null ? userType : "UNKNOWN");
        log.setStatus("FAIL");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setErrorMessage(failReason);
        LoginLog saved = loginLogRepository.save(log);
        cacheAfterAddLog(saved);
    }

    /** 查：全部日志 — 先缓存后数据库 */
    public List<LoginLog> getAllLogs() {
        return cache.getOrLoad(CACHE_LOGIN_LOGS, "all", loginLogRepository::findAll);
    }

    /** 查：分页日志 — 先缓存后数据库 */
    public Page<LoginLog> getLogsPaginated(Pageable pageable) {
        String key = RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_LOGIN_LOGS, key, () -> loginLogRepository.findAll(pageable));
    }

    /** 查：按用户名 — 先缓存后数据库 */
    public List<LoginLog> getLogsByUsername(String username) {
        return cache.getOrLoad(CACHE_LOGIN_LOGS, "username_" + username,
                () -> loginLogRepository.findByUsername(username));
    }

    /** 查：按用户名分页 — 先缓存后数据库 */
    public Page<LoginLog> getLogsByUsernamePaginated(String username, Pageable pageable) {
        String key = "username_" + username + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_LOGIN_LOGS, key,
                () -> loginLogRepository.findByUsername(username, pageable));
    }

    /** 查：按用户类型 — 先缓存后数据库 */
    public List<LoginLog> getLogsByUserType(String userType) {
        return cache.getOrLoad(CACHE_LOGIN_LOGS, "userType_" + userType,
                () -> loginLogRepository.findByUserType(userType));
    }

    /** 查：按时间范围 — 先缓存后数据库 */
    public List<LoginLog> getLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        String key = "range_" + startTime + "_" + endTime;
        return cache.getOrLoad(CACHE_LOGIN_LOGS, key,
                () -> loginLogRepository.findByCreateTimeBetween(startTime, endTime));
    }

    /** 查：用户历史分页 — 先缓存后数据库 */
    public Page<LoginLog> getLogsByUser(Long userId, String userType, Pageable pageable) {
        String key = "user_" + userId + "_" + userType + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_LOGIN_LOGS, key,
                () -> loginLogRepository.findByUserIdAndUserType(userId, userType, pageable));
    }

    /** 查：按 ID — 先缓存后数据库 */
    public LoginLog getLogById(Long id) {
        return cache.getOrLoad(CACHE_LOGIN_LOGS, "id_" + id,
                () -> loginLogRepository.findById(id).orElse(null));
    }

    /** 删：删除单条日志并清除对应缓存 */
    public void deleteLog(Long id) {
        loginLogRepository.findById(id).ifPresent(log -> {
            loginLogRepository.deleteById(id);
            cacheAfterDeleteLog(id, log.getUsername(), log.getUserId(), log.getUserType());
        });
    }

    /** 删：清理旧日志并清除列表缓存 */
    public void deleteOldLogs(LocalDateTime beforeDate) {
        loginLogRepository.deleteByCreateTimeBefore(beforeDate);
        cache.evictAll(CACHE_LOGIN_LOGS);
    }

    /** 查：用户登录汇总 — 先缓存后数据库 */
    public List<UserLoginSummary> getUserLoginSummaryList() {
        return cache.getOrLoad(CACHE_LOGIN_LOGS, KEY_SUMMARY, this::loadUserLoginSummaryList);
    }

    private List<UserLoginSummary> loadUserLoginSummaryList() {
        List<UserLoginSummary> result = new ArrayList<>();

        for (SuperAdmin sa : superAdminRepository.findAll()) {
            result.add(buildSummary(sa.getId(), sa.getUsername(), "SUPER_ADMIN",
                    sa.getName(), sa.getStatus()));
        }
        for (AdminUser au : adminUserRepository.findAll()) {
            result.add(buildSummary(au.getId(), au.getUsername(), "ADMIN",
                    au.getName(), au.getStatus()));
        }
        for (NormalUser nu : normalUserRepository.findAll()) {
            result.add(buildSummary(nu.getId(), nu.getUsername(), "READER",
                    nu.getName(), nu.getStatus()));
        }

        result.sort((a, b) -> {
            if (a.getLastLoginTime() == null && b.getLastLoginTime() == null) return 0;
            if (a.getLastLoginTime() == null) return 1;
            if (b.getLastLoginTime() == null) return -1;
            return b.getLastLoginTime().compareTo(a.getLastLoginTime());
        });

        return result;
    }

    private UserLoginSummary buildSummary(Long userId, String username, String userType,
                                           String name, String accountStatus) {
        LoginLog lastLog = loginLogRepository
                .findTopByUserIdAndUserTypeOrderByCreateTimeDesc(userId, userType)
                .orElse(null);

        LocalDateTime lastLoginTime = null;
        String lastLoginIp = null;
        String lastLoginStatus = null;

        if (lastLog != null) {
            lastLoginTime = lastLog.getCreateTime();
            lastLoginIp = lastLog.getIpAddress();
            lastLoginStatus = lastLog.getStatus();
        }

        return new UserLoginSummary(userId, username, userType, name,
                lastLoginTime, lastLoginIp, lastLoginStatus, accountStatus);
    }

    private void cacheAfterAddLog(LoginLog saved) {
        cache.put(CACHE_LOGIN_LOGS, "id_" + saved.getId(), saved);
        cache.evict(CACHE_LOGIN_LOGS, KEY_SUMMARY);
        cache.evictByKeyPrefix(CACHE_LOGIN_LOGS, "page_");
        cache.evictByKeyPrefix(CACHE_LOGIN_LOGS, "username_");
        cache.evictByKeyPrefix(CACHE_LOGIN_LOGS, "user_");
        cache.evict(CACHE_LOGIN_LOGS, "all");
    }

    private void cacheAfterDeleteLog(Long id, String username, Long userId, String userType) {
        cache.evict(CACHE_LOGIN_LOGS, "id_" + id);
        cache.evict(CACHE_LOGIN_LOGS, KEY_SUMMARY);
        cache.evictByKeyPrefix(CACHE_LOGIN_LOGS, "page_");
        cache.evictByKeyPrefix(CACHE_LOGIN_LOGS, "username_");
        cache.evict(CACHE_LOGIN_LOGS, "username_" + username);
        cache.evictByKeyPrefix(CACHE_LOGIN_LOGS, "user_" + userId + "_" + userType);
        cache.evict(CACHE_LOGIN_LOGS, "all");
    }
}
