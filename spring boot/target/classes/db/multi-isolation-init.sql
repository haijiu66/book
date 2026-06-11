-- ==========================================
-- 多级别用户数据隔离系统 - 数据库初始化脚本
-- ==========================================

-- 1. 创建 admin_db 数据库（管理员数据库）
CREATE DATABASE IF NOT EXISTS admin_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE admin_db;

-- 1.1 创建 super_admin 表（超级管理员表）
CREATE TABLE IF NOT EXISTS super_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.2 创建 admin_user 表（管理员用户表）
CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    created_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.3 创建 audit_log 表（审计日志表）
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_type VARCHAR(20) NOT NULL,
    operator_id BIGINT,
    operator_name VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    operation_detail TEXT NOT NULL,
    target_type VARCHAR(20),
    target_id BIGINT,
    operation_status VARCHAR(20) NOT NULL,
    error_message TEXT,
    ip_address VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.4 创建 login_log 表（登录日志表）
CREATE TABLE IF NOT EXISTS login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(50),
    user_agent TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.5 创建 token_blacklist 表（Token黑名单表）
CREATE TABLE IF NOT EXISTS token_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    username VARCHAR(50) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1.6 初始化超级管理员（密码：123456，BCrypt加密）
INSERT INTO super_admin (username, password, name) VALUES
('superadmin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统超级管理员');

-- 1.7 初始化普通管理员（密码：123456，BCrypt加密）
INSERT INTO admin_user (username, password, name, table_name, created_by) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin_1_data', 1);

-- ==========================================

-- 2. 创建 user_db 数据库（普通用户数据库）
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;

-- 2.1 创建 normal_user 表（普通用户登录信息表）
CREATE TABLE IF NOT EXISTS normal_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    table_name VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2 初始化普通用户（密码：123456，BCrypt加密）
INSERT INTO normal_user (username, password, name, table_name) VALUES
('reader', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '普通读者', 'user_1_data');

-- ==========================================

-- 3. 创建 admin_data_db 数据库（管理员个人数据数据库）
CREATE DATABASE IF NOT EXISTS admin_data_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE admin_data_db;

-- 备注：管理员专属数据表会在管理员创建时动态创建

-- ==========================================

-- 4. 创建 user_data_db 数据库（普通用户个人数据数据库）
CREATE DATABASE IF NOT EXISTS user_data_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_data_db;

-- 备注：用户专属数据表会在用户注册/创建时动态创建

-- ==========================================

-- 5. 创建备份目录表（用于备份管理）
USE admin_db;
CREATE TABLE IF NOT EXISTS backup_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_name VARCHAR(100) NOT NULL,
    backup_type VARCHAR(20) NOT NULL,
    backup_path TEXT NOT NULL,
    backup_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================

-- 6. 初始化 library_db（图书管理主数据库）
CREATE DATABASE IF NOT EXISTS library_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_db;

-- 6.1 用户表（兼容旧版系统）
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'READER',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.2 图书表
CREATE TABLE IF NOT EXISTS book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    publisher VARCHAR(100),
    publish_date DATE,
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock INT NOT NULL DEFAULT 0,
    available INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.3 借阅表
CREATE TABLE IF NOT EXISTS borrow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_book_id (book_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.4 电子书表（在线阅读功能）
-- 资源文件存储在 src/main/resources/Book 目录下
CREATE TABLE IF NOT EXISTS ebook (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100),
    resource_name VARCHAR(200) NOT NULL COMMENT '资源文件名（存储在resources/Book目录）',
    file_type VARCHAR(20) NOT NULL COMMENT 'txt/docx',
    file_size BIGINT COMMENT '文件大小(字节)',
    cover_path VARCHAR(500) COMMENT '封面图片路径',
    description TEXT COMMENT '书籍简介',
    chapter_count INT DEFAULT 0 COMMENT '章节总数',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    upload_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_book_id (book_id),
    INDEX idx_status (status),
    INDEX idx_upload_user_id (upload_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.5 章节表（在线阅读功能）
-- 章节内容直接缓存到数据库，首次上传时存储，后续直接读取
CREATE TABLE IF NOT EXISTS chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ebook_id BIGINT NOT NULL,
    chapter_index INT NOT NULL COMMENT '章节序号，从0开始',
    title VARCHAR(500) NOT NULL COMMENT '章节标题',
    content LONGTEXT COMMENT '章节内容缓存',
    content_cached TINYINT(1) DEFAULT 1 COMMENT '内容是否已缓存',
    word_count INT COMMENT '字数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ebook_id (ebook_id),
    INDEX idx_chapter_index (ebook_id, chapter_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.6 阅读进度表（在线阅读功能）
CREATE TABLE IF NOT EXISTS reading_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ebook_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    chapter_index INT NOT NULL,
    scroll_position INT DEFAULT 0 COMMENT '滚动位置(像素)',
    last_read_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    total_read_time BIGINT DEFAULT 0 COMMENT '累计阅读时间(秒)',
    UNIQUE KEY uk_user_ebook (user_id, ebook_id),
    INDEX idx_user_id (user_id),
    INDEX idx_ebook_id (ebook_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6.7 初始化图书数据
INSERT INTO book (isbn, title, author, publisher, publish_date, category, price, stock, available) VALUES
('9787111111111', 'Java编程思想', 'Bruce Eckel', '机械工业出版社', '2007-06-01', '计算机', 108.00, 10, 10),
('9787115279460', '深入理解Java虚拟机', '周志明', '人民邮电出版社', '2019-06-01', '计算机', 129.00, 5, 5),
('9787115417394', 'Spring Boot实战', 'Craig Walls', '人民邮电出版社', '2016-04-01', '计算机', 89.00, 8, 8),
('9787115424840', 'Vue.js设计与实现', '霍春阳', '人民邮电出版社', '2022-05-01', '计算机', 109.00, 12, 12);

-- 6.8 初始化兼容旧版的用户数据（library_db.user表）
INSERT INTO user (username, password, name, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN'),
('reader', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '普通读者', 'READER');

-- ==========================================

-- 初始化完成提示
SELECT '数据库初始化完成！' AS message;
SELECT '超级管理员账号：superadmin / 123456' AS note1;
SELECT '管理员账号：admin / 123456' AS note2;
SELECT '普通用户账号：reader / 123456' AS note3;
