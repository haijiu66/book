package com.library.service.admin;

import com.library.cache.RedisCacheHelper;
import com.library.dto.AuditUserSummary;
import com.library.entity.admin.AuditLog;
import com.library.repository.admin.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private static final String CACHE_AUDIT_LOGS = "auditLogs";
    private static final String KEY_SUMMARY = "user_summary";
    private static final String KEY_COUNT = "count";

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RedisCacheHelper cache;

    /** 查：分页全部 — 先缓存后数据库 */
    public Page<AuditLog> findAll(Pageable pageable) {
        String key = RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key, () -> auditLogRepository.findAll(pageable));
    }

    /** 查：按 ID — 先缓存后数据库 */
    public Optional<AuditLog> findById(Long id) {
        AuditLog cached = cache.get(CACHE_AUDIT_LOGS, "id_" + id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<AuditLog> found = auditLogRepository.findById(id);
        found.ifPresent(log -> cache.put(CACHE_AUDIT_LOGS, "id_" + id, log));
        return found;
    }

    /** 查：按操作者分页 — 先缓存后数据库 */
    public Page<AuditLog> findByOperator(String operatorType, Long operatorId, Pageable pageable) {
        String key = "operator_" + operatorType + "_" + operatorId + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key,
                () -> auditLogRepository.findByOperatorTypeAndOperatorId(operatorType, operatorId, pageable));
    }

    /** 查：按操作类型分页 — 先缓存后数据库 */
    public Page<AuditLog> findByOperationType(String operationType, Pageable pageable) {
        String key = "opType_" + operationType + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key,
                () -> auditLogRepository.findByOperationType(operationType, pageable));
    }

    /** 查：按操作状态分页 — 先缓存后数据库 */
    public Page<AuditLog> findByOperationStatus(String operationStatus, Pageable pageable) {
        String key = "opStatus_" + operationStatus + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key,
                () -> auditLogRepository.findByOperationStatus(operationStatus, pageable));
    }

    /** 查：按时间范围分页 — 先缓存后数据库 */
    public Page<AuditLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        String key = "range_" + startTime + "_" + endTime + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key,
                () -> auditLogRepository.findByCreateTimeBetween(startTime, endTime, pageable));
    }

    /** 查：多条件分页 — 先缓存后数据库 */
    public Page<AuditLog> findByConditions(String operatorType, String operationType,
                                            String operationStatus, Pageable pageable) {
        String key = "cond_" + operatorType + "_" + operationType + "_" + operationStatus
                + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key,
                () -> auditLogRepository.findByConditions(operatorType, operationType, operationStatus, pageable));
    }

    /** 增：保存审计日志后同步写入缓存 */
    public AuditLog save(AuditLog auditLog) {
        AuditLog saved = auditLogRepository.save(auditLog);
        cacheAfterAddLog(saved);
        return saved;
    }

    /** 查：操作者汇总 — 先缓存后数据库 */
    public List<AuditUserSummary> getAuditUserSummaryList() {
        return cache.getOrLoad(CACHE_AUDIT_LOGS, KEY_SUMMARY, this::loadAuditUserSummaryList);
    }

    /** 查：操作者历史分页 — 先缓存后数据库 */
    public Page<AuditLog> getLogsByOperator(String operatorType, Long operatorId, Pageable pageable) {
        String key = "history_" + operatorType + "_" + operatorId + "_" + RedisCacheHelper.pageKey(pageable);
        return cache.getOrLoad(CACHE_AUDIT_LOGS, key,
                () -> auditLogRepository.findByOperatorTypeAndOperatorIdOrderByCreateTimeDesc(
                        operatorType, operatorId, pageable));
    }

    /** 删：清理旧日志并清除缓存 */
    public void deleteOldLogs(LocalDateTime beforeDate) {
        List<AuditLog> oldLogs = auditLogRepository.findByCreateTimeBefore(beforeDate);
        for (AuditLog log : oldLogs) {
            auditLogRepository.deleteById(log.getId());
            cache.evict(CACHE_AUDIT_LOGS, "id_" + log.getId());
        }
        cache.evict(CACHE_AUDIT_LOGS, KEY_SUMMARY);
        cache.evict(CACHE_AUDIT_LOGS, KEY_COUNT);
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "page_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "operator_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "history_");
    }

    /** 查：总数 — 先缓存后数据库 */
    public long count() {
        Long cached = cache.get(CACHE_AUDIT_LOGS, KEY_COUNT);
        if (cached != null) {
            return cached;
        }
        long count = auditLogRepository.count();
        cache.put(CACHE_AUDIT_LOGS, KEY_COUNT, count);
        return count;
    }

    private List<AuditUserSummary> loadAuditUserSummaryList() {
        List<Object[]> rows = auditLogRepository.findOperatorSummaries();
        return rows.stream().map(row -> {
            LocalDateTime lastTime = null;
            if (row[3] != null) {
                if (row[3] instanceof LocalDateTime) {
                    lastTime = (LocalDateTime) row[3];
                } else if (row[3] instanceof java.sql.Timestamp) {
                    lastTime = ((java.sql.Timestamp) row[3]).toLocalDateTime();
                }
            }
            return new AuditUserSummary(
                    (String) row[0],
                    row[1] != null ? ((Number) row[1]).longValue() : null,
                    (String) row[2],
                    lastTime,
                    row[4] != null ? ((Number) row[4]).longValue() : 0L
            );
        }).collect(Collectors.toList());
    }

    private void cacheAfterAddLog(AuditLog saved) {
        cache.put(CACHE_AUDIT_LOGS, "id_" + saved.getId(), saved);
        cache.evict(CACHE_AUDIT_LOGS, KEY_SUMMARY);
        cache.evict(CACHE_AUDIT_LOGS, KEY_COUNT);
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "page_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "operator_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "history_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "opType_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "opStatus_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "range_");
        cache.evictByKeyPrefix(CACHE_AUDIT_LOGS, "cond_");
    }
}
