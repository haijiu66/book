package com.library.service.isolation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Slf4j
@Service
public class DynamicTableService {

    @Autowired
    @Qualifier("adminDataDataSource")
    private DataSource adminDataDataSource;

    @Autowired
    @Qualifier("userDataDataSource")
    private DataSource userDataDataSource;

    public void createAdminTables(Long adminId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(adminDataDataSource);
        String bookTableName = "admin_book_" + adminId;
        String borrowTableName = "admin_borrow_" + adminId;

        log.info("开始创建管理员专属表: adminId={}", adminId);

        createAdminBookTable(jdbcTemplate, bookTableName);
        createAdminBorrowTable(jdbcTemplate, borrowTableName);

        log.info("管理员专属表创建完成: adminId={}", adminId);
    }

    public void dropAdminTables(Long adminId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(adminDataDataSource);
        String bookTableName = "admin_book_" + adminId;
        String borrowTableName = "admin_borrow_" + adminId;

        log.info("开始删除管理员专属表: adminId={}", adminId);

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + borrowTableName);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + bookTableName);

        log.info("管理员专属表删除完成: adminId={}", adminId);
    }

    public void createUserTables(Long userId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(userDataDataSource);
        String bookTableName = "user_book_" + userId;
        String borrowTableName = "user_borrow_" + userId;

        log.info("开始创建用户专属表: userId={}", userId);

        createUserBookTable(jdbcTemplate, bookTableName);
        createUserBorrowTable(jdbcTemplate, borrowTableName);

        log.info("用户专属表创建完成: userId={}", userId);
    }

    public void dropUserTables(Long userId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(userDataDataSource);
        String bookTableName = "user_book_" + userId;
        String borrowTableName = "user_borrow_" + userId;

        log.info("开始删除用户专属表: userId={}", userId);

        jdbcTemplate.execute("DROP TABLE IF EXISTS " + borrowTableName);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + bookTableName);

        log.info("用户专属表删除完成: userId={}", userId);
    }

    private void createAdminBookTable(JdbcTemplate jdbcTemplate, String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "isbn VARCHAR(20) UNIQUE NOT NULL, " +
                "title VARCHAR(200) NOT NULL, " +
                "author VARCHAR(100) NOT NULL, " +
                "publisher VARCHAR(100), " +
                "publish_date DATE, " +
                "category VARCHAR(50), " +
                "price DECIMAL(10,2), " +
                "stock INT NOT NULL DEFAULT 0, " +
                "available INT NOT NULL DEFAULT 0, " +
                "description TEXT, " +
                "cover_image VARCHAR(500), " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_isbn (isbn), " +
                "INDEX idx_title (title), " +
                "INDEX idx_category (category)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        jdbcTemplate.execute(sql);
    }

    private void createAdminBorrowTable(JdbcTemplate jdbcTemplate, String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "book_id BIGINT NOT NULL, " +
                "user_id BIGINT NOT NULL, " +
                "borrow_date DATETIME NOT NULL, " +
                "due_date DATETIME NOT NULL, " +
                "return_date DATETIME, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'BORROWED', " +
                "fine DECIMAL(10,2) DEFAULT 0, " +
                "remark TEXT, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_book_id (book_id), " +
                "INDEX idx_user_id (user_id), " +
                "INDEX idx_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        jdbcTemplate.execute(sql);
    }

    private void createUserBookTable(JdbcTemplate jdbcTemplate, String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "book_id BIGINT NOT NULL, " +
                "source_admin_id BIGINT NOT NULL, " +
                "isbn VARCHAR(20) NOT NULL, " +
                "title VARCHAR(200) NOT NULL, " +
                "author VARCHAR(100) NOT NULL, " +
                "category VARCHAR(50), " +
                "is_favorite BOOLEAN DEFAULT FALSE, " +
                "read_status VARCHAR(20) DEFAULT 'UNREAD', " +
                "rating INT, " +
                "comment TEXT, " +
                "borrow_count INT DEFAULT 0, " +
                "last_borrow_time DATETIME, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_book_id (book_id), " +
                "INDEX idx_is_favorite (is_favorite), " +
                "INDEX idx_read_status (read_status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        jdbcTemplate.execute(sql);
    }

    private void createUserBorrowTable(JdbcTemplate jdbcTemplate, String tableName) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "book_id BIGINT NOT NULL, " +
                "source_admin_id BIGINT NOT NULL, " +
                "borrow_date DATETIME NOT NULL, " +
                "due_date DATETIME NOT NULL, " +
                "return_date DATETIME, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'BORROWED', " +
                "fine DECIMAL(10,2) DEFAULT 0, " +
                "remark TEXT, " +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_book_id (book_id), " +
                "INDEX idx_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        jdbcTemplate.execute(sql);
    }
}
