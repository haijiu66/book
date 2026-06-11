package com.library.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时修复 reading_progress 表的唯一约束：
 * 将旧的 (user_id, ebook_id) 约束替换为 (user_id, user_type, ebook_id)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseConstraintMigration implements CommandLineRunner {

    private final DataSource primaryDataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = primaryDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 尝试删除旧的2列唯一约束（如果存在）
            try {
                stmt.execute("ALTER TABLE reading_progress DROP INDEX uk_user_ebook");
                log.info("[约束迁移] 已删除旧约束 uk_user_ebook (user_id, ebook_id)");
            } catch (Exception e) {
                // 约束不存在，忽略
                log.info("[约束迁移] 旧约束 uk_user_ebook 不存在或已删除，跳过");
            }

            // 确保 user_type 列存在且有默认值
            try {
                stmt.execute("ALTER TABLE reading_progress " +
                    "MODIFY COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'READER'");
                log.info("[约束迁移] user_type 列已更新");
            } catch (Exception e) {
                log.info("[约束迁移] user_type 列调整: {}", e.getMessage());
            }

            // 添加新的3列唯一约束（如果不存在）
            try {
                stmt.execute("ALTER TABLE reading_progress " +
                    "ADD CONSTRAINT uk_user_type_ebook UNIQUE (user_id, user_type, ebook_id)");
                log.info("[约束迁移] 已添加新约束 uk_user_type_ebook (user_id, user_type, ebook_id)");
            } catch (Exception e) {
                // 约束已存在，忽略
                log.info("[约束迁移] 新约束已存在，跳过");
            }

        } catch (Exception e) {
            log.error("[约束迁移] 迁移失败: {}", e.getMessage(), e);
        }
    }
}
