package com.library.config;

import com.library.entity.admin.SuperAdmin;
import com.library.repository.admin.SuperAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSelfCheckRunner implements CommandLineRunner {

    private final SuperAdminRepository superAdminRepository;

    @Override
    public void run(String... args) {
        List<SuperAdmin> superAdmins = superAdminRepository.findAll();

        if (superAdmins.isEmpty()) {
            log.warn("[启动自检] admin_db.super_admin 表中未查询到超级管理员账号，请检查初始化脚本是否已执行。");
            return;
        }

        log.info("[启动自检] super_admin 账号数量: {}", superAdmins.size());

        for (SuperAdmin superAdmin : superAdmins) {
            String status = superAdmin.getStatus();
            boolean statusEmpty = status == null || status.isBlank();
            log.info(
                "[启动自检] super_admin username='{}', status='{}', statusEmpty={}",
                superAdmin.getUsername(),
                status,
                statusEmpty
            );
        }
    }
}
