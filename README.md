
# 图书管理系统 管理文档

## 一、项目概述
基于 **Spring Boot 3.2 + Vue 3** 的图书管理系统。五数据库隔离 + 三级用户 + **Redis 全业务缓存**
| 层级 | 技术栈 |
|------|--------|
| 后端 | Java 17, Spring Boot 3.2.0, Spring Security, Spring Data JPA, JdbcTemplate, MySQL 8, Redis, JWT (jjwt 0.11.5), Apache POI 5.2.5 |
| 前端 | Vue 3.4, Vite 5, Pinia 2.1.7, Vue Router 4.2.5, Element Plus 2.4.4 (zh-cn), Axios 1.6.2, AnimeJS |

---

## 二、数据库架构

### 2.1 五数据库

| 数据源 | 数据源Bean | 用途 |
|--------|------------|------|
| `library_db` | `primaryDataSource` | 核心: book, ebook, category, borrow, chapter, reading_progress, user_favorite |
| `admin_db` | `adminDataSource` | 管理: super_admin, admin_user, audit_log, login_log, token_blacklist |
| `user_db` | `userDataSource` | 普通用户 normal_user |
| `admin_data_db` | `adminDataDataSource` | 管理员隔离数据(动态表) |
| `user_data_db` | `userDataDataSource` | 用户隔离数据(动态表) |

### 2.2 核心表
| 表名 | 关键字段 | 备注 |
|------|---------|------|
| `book` | id, isbn(unique), title, author, publisher, stock, available, cover_path, price, **visible** | @SQLRestriction 软删除 |
| `ebook` | id, title, author, resource_name, file_type, chapter_count, file_size, **visible** | 同上 |
| `category` | id, name(unique), description, **parent_id** | 支持树形父子分类 |
| `book_categories` | book_id(FK), category_id(FK) | 多对多 |
| `ebook_categories` | ebook_id(FK), category_id(FK) | 多对多 |
| `borrow` | id, user_id, book_id, borrow_date, due_date, return_date, status | 状态: BORROWED/RETURNED/OVERDUE/CANCELLED |
| `reading_progress` | id, user_id, user_type, ebook_id, chapter_id, chapter_index, scroll_position | 唯一约束: (user_id,user_type,ebook_id) |
| `user_favorite` | id, user_id, user_type, target_type, target_id | 唯一约束: (user_id,user_type,target_type,target_id) |
| `super_admin` | id, username(unique), password, name, status | - |
| `admin_user` | id, username(unique), password, name, **permissions**, **visible** | permissions=逗号分隔权限码 |
| `normal_user` | id, username(unique), password, name, phone, email, **visible** | @SQLRestriction |
| `audit_log` | id, operator_type/id/name, operation_type, target_type/id, operation_detail, ip_address, execution_time | AOP 自动记录 |
| `login_log` | id, user_id, username, user_type, ip_address, status, error_message, user_agent | - |
| `token_blacklist` | id, token, username, expire_time | 登出后将 token 加入 |
| `role` | id, role_code(unique), role_name, description, status | (已废弃，Entity 和 Repository 已删除) |
| `permission` | id, permission_code(unique), permission_name, resource_type, resource_url | (已废弃，Entity 和 Repository 已删除) |
| `role_permission` | role_id, permission_id | (已废弃，Entity 和 Repository 已删除) |

### 2.3 电子书独立章节表

每本电子书独立建表（JdbcTemplate 操作，不经过 JPA）：

```sql
CREATE TABLE ebook_chapters_{ebookId} (
    id BIGINT PRIMARY KEY,          -- = ebookId * 1000000 + chapterIndex
    chapter_index INT NOT NULL,
    title VARCHAR(500),
    content LONGTEXT,
    content_cached BOOLEAN DEFAULT TRUE,
    word_count INT DEFAULT 0
);
```

上传时 `CREATE TABLE`，删除时 `DROP TABLE`。
---

## 三、缓存架构
### 3.1 配置

Redis `localhost:6379`，密码 `myredis123`。
使用 `GenericJackson2JsonRedisSerializer` + `activateDefaultTyping(NON_FINAL)` + `BasicPolymorphicTypeValidator`（仅允许 `com.library.entity/dto` 包和标准集合）。
### 3.2 各区域TTL

| 区域 | TTL | 缓存内容 |
|------|-----|---------|
| `books` | 5min | 图书列表/按分类（搜索直查数据库） |
| `book` | 10min | 单本图书 |
| `ebooks` | 5min | 电子书列表/分类 |
| `ebook` | 10min | 电子书详情/章节/进度 |
| `categories` | 30min | 分类列表 |
| `ranking` | 10min | 排行数据 |
| `users` | 5min | 用户列表/认证（按角色隔离 key） |
| `admins` | 5min | 管理员列表 |
| `borrows` | 3min | 借阅记录 |
| `blacklist` | 5min | token 黑名单 |
| `favorites` | 3min | 收藏 |

### 3.3 缓存 key 设计

认证缓存 key 包含角色前缀，防止同名不同角色的用户互相覆盖。
```
auth_READER_zhangsan     ← NormalUser
auth_ADMIN_zhangsan      ← AdminUser
auth_SUPER_ADMIN_root    ← SuperAdmin
```

## 四、安全体系
### 4.1 API 认证

`SecurityConfig` 采用白名单模式：
- **公开接口**（无需 token）：登录、注册、GET 浏览接口
- **其余接口**：全部 `.anyRequest().authenticated()` 要求认证

通过 `JwtAuthenticationFilter` 拦截请求，解析 JWT token 并设置 SecurityContext。
### 4.2 三级角色 + 按角色查询
| 角色 | 存储位置 | 说明 |
|------|--------|------|
| 超级管理员 `SUPER_ADMIN` | `super_admin` | 最高权限，独立登录接口 `/super-admin/login` |
| 管理员 `ADMIN` | `admin_user` | 由超管配置权限 |
| 普通用户 `READER` | `normal_user` | 浏览/借阅/收藏/在线阅读 |

登录时前端传 `role` 字段，后端使用 `LoginRoleContext(ThreadLocal)` 将角色传递给 `MultiUserDetailsServiceImpl`，**只查对应表**，同名不同角色的用户互不干扰。
### 4.3 权限注解

| 注解 | 允许角色 |
|------|---------|
| `@RequireSuperAdmin` | 超管 |
| `@RequireAdmin` | 超管 + 管理员 |
| `@PreAuthorize("@permissionChecker.hasPermission('CODE')")` | 超管 + 有权限的管理员 |

### 4.4 整体安全措施

| 措施 | 说明 |
|------|------|
| JWT 24h + 黑名单 | Token 过期自动清理，登出立即失效 |
| CORS 安全加固 | 限制 origin 为 `localhost:*`，method 白名单 |
| 悲观写锁 | `@Lock(PESSIMISTIC_WRITE)` 防止并发超卖 |
| 借阅天数限制 | 借阅上限 120 天，续借上限从借阅日起最长365天 |
| 库存负数保护 | 减少库存时，新库存若低于已借出数量则拒绝 |
| 归属校验 | 所有借阅操作校验当前用户是否为记录所有者或管理员 |
| 文件类型校验 | `ImageFileValidator`：大小/Content-Type/扩展名/魔数 4 重校验 |
| 全局异常处理 | `GlobalExceptionHandler` 统一处理 7 种异常类型，`BusinessException` 返回 400 + 应用层 errorCode |
| 语义化错误码 | `ApiResponse.badRequest(400)` / `forbidden(403)` / `notFound(404)` / `conflict(409)` |

---

## 五、系统架构
### 5.1 后端分层

```
Controller       ← @RestController, 接收请求/返回 ApiResponse
Service          ← 业务逻辑 + Redis 缓存管理
Repository       ← Spring Data JPA, 数据库操作
Entity           ← JPA 实体, @SQLRestriction 软删除
DTO              ← 数据传输对象, 缓存 DTO 包装
Security         ← JWT, 认证过滤器, 权限检查
Cache            ← RedisCacheHelper, CacheDtoMapper
Config           ← 数据源, Redis, 事务管理器
Util             ← 工具类 (ImageFileValidator, PasswordUtil)
```

### 5.2 前端分层

```
views/           ← 页面组件 (Login/Register + 三级角色各模块)
components/      ← 公共组件 (Layout, ConfirmDialog)
stores/          ← Pinia 状态管理 (auth, book, borrow, ebook)
api/             ← Axios 封装 (book, borrow, ebook, auth, etc.)
router/          ← Vue Router 路由 + 导航守卫
utils/           ← request.js Axios 实例 + 拦截器
directives/      ← 自定义指令 (permission, role)
styles/          ← 全局主题 CSS
```

### 5.3 请求处理流
```
前端请求 → Axios 拦截器 (添加 Bearer token)
         → Spring Security FilterChain
         → JwtAuthenticationFilter (校验 token + 黑名单)
         → Controller (检查 @RequireAdmin/@PreAuthorize)
         → Service (业务逻辑 + 缓存)
         → Repository (JPA + @Lock 悲观锁)
         → MySQL/Redis
         → 返回 ApiResponse (code, success, message, data)
         → GlobalExceptionHandler (异常时统一拦截+打印日志)
         → Axios 拦截器 (401跳转登录/403弹权限不足/500弹服务器错误)
```

---

## 六、定时任务
| 任务 | 时间 | 说明 |
|------|------|------|
| `LogCleanupScheduler` | 每日 3:00 | 清理 90 天前审计日志和登录日志 |
| `JwtUtil.cleanExpiredTokens()` | 每日 2:00 | 清理过期 token 黑名单 |

---

## 七、配置
```yaml
# springboot/src/main/resources/application.yml
server.port: 8080
spring:
  datasource:        # 5 数据源 (library_db/admin_db/user_db/admin_data_db/user_data_db)
  jpa.hibernate.ddl-auto: update
  servlet.multipart.max-file-size: 50MB
  data.redis:
    host: localhost
    port: 6379
    password: myredis123
jwt:
  secret: library-management-system-secret-key-for-jwt-token-generation
  expiration: 86400000    # 24h
```

```javascript
// vue3/vite.config.js
server: { port: 5174, proxy: { '/api': 'http://localhost:8080' } }
```

---

## 八、启动
```bash
# 1. 启动 MySQL 5 个库 + Redis
# 2. 启动后端
cd springboot && mvn spring-boot:run

# 3. 启动前端
cd vue3 && pnpm install && pnpm run dev
# 访问 http://localhost:5174
```

预置账号:

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | superadmin | 123456 |
| 管理员 | admin | 123456 |
| 普通用户 | reader | 123456 |

---

## 九、关键设计决策
| 决策 | 原因 |
|------|------|
| 电子书独立章节表 | 每本小说一张表，数据隔离，删除时 DROP |
| JdbcTemplate 操作章节 | 动态表名无法用 JPA 实体映射 |
| 章节 ID = ebookId*1M+index | 全局唯一，无需序列号 |
| Redis 全业务缓存 | 高频查询加速，写入自动清除 |
| @JsonSerialize(as=HashSet) | 防止 Hibernate PersistentSet 类型泄露到 Redis |
| 软删除(visible) | 数据完整保留，误删可恢复 |
| @SQLRestriction | Hibernate 自动过滤 visible=0 记录 |
| 条件缓存认证 | `loadUserByUsername` + `isTokenBlacklisted` 缓存到 Redis |
| LoginRoleContext(ThreadLocal) | 同名用户可按不同角色查对应表，互不干扰 |
| 悲观写锁 | `@Lock(PESSIMISTIC_WRITE)` + `SELECT ... FOR UPDATE` 防止超卖 |
| @PreAuthorize 细粒度权限 | 管理员分类增删权限可由超管动态分配 |
| 文件魔数校验 | 防止修改扩展名绕过文件类型白名单 |
| 全局异常处理器 | Controller 不再需要 try-catch，统一日志+统一返回格式 |
| 语义化错误码 | 400/403/404/409/500 分别对应不同错误场景 |
| 缓存 key 含角色前缀 | auth_READER_zhangsan 和 auth_ADMIN_zhangsan 互不冲突 |
| UTF-8 BOM 下载 | Windows 记事本正确识别中文 |
| Element Plus 中文 | 全局 zh-cn 语言包 |
| sessionStorage | 关闭浏览器自动清除登录状态 |
| BusinessException | 业务异常与编程异常分离，支持应用层 errorCode |
| 审计日志文件回退 | DB 写入失败时自动回退到 `logs/audit_fallback/` JSON 文件 |
| CORS 白名单 | 仅允许 `localhost:*` 来源，禁止通配符 |
| 日志文件滚动策略 | `logging.file` + `rollingpolicy`，按大小/时间自动轮转 |
