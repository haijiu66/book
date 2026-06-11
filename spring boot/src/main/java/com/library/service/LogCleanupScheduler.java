package com.library.service;

import com.library.service.admin.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@EnableScheduling
public class LogCleanupScheduler {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private LoginLogService loginLogService;

    /**
     * 每天凌晨 3:00 自动清理 90 天前的审计日志和登录日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoCleanOldLogs() {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        log.info("定时清理任务开始：清理 {} 之前的日志", ninetyDaysAgo);
        try {
            auditLogService.deleteOldLogs(ninetyDaysAgo);
            log.info("审计日志清理完成");
        } catch (Exception e) {
            log.error("审计日志清理失败", e);
        }
        try {
            loginLogService.deleteOldLogs(ninetyDaysAgo);
            log.info("登录日志清理完成");
        } catch (Exception e) {
            log.error("登录日志清理失败", e);
        }
    }
}
