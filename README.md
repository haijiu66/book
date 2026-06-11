 1|# 图书管理系统 — 管理文档
 2|
 3|## 一、项目概述
 4|
 5|基于 **Spring Boot 3.2 + Vue 3** 的图书管理系统。五数据库隔离 + 三级用户 + **Redis 全业务缓存**。
 6|
 7|| 层级 | 技术栈 |
 8||------|--------|
 9|| 后端 | Java 17, Spring Boot 3.2.0, Spring Security, Spring Data JPA, JdbcTemplate, MySQL 8, Redis, JWT (jjwt 0.11.5), Apache POI 5.2.5 |
10|| 前端 | Vue 3.4, Vite 5, Pinia 2.1.7, Vue Router 4.2.5, Element Plus 2.4.4 (zh-cn), Axios 1.6.2, AnimeJS |
11|
12|---
13|
14|## 二、数据库架构
15|
16|### 2.1 五数据库
17|
18|| 数据库 | 数据源 Bean | 用途 |
19||--------|------------|------|
20|| `library_db` | `primaryDataSource` | 核心: book, ebook, category, borrow, chapter, reading_progress, user_favorite |
21|| `admin_db` | `adminDataSource` | 管理: super_admin, admin_user, audit_log, login_log, token_blacklist |
22|| `user_db` | `userDataSource` | 普通用户: normal_user |
23|| `admin_data_db` | `adminDataDataSource` | 管理员隔离数据(动态表) |
24|| `user_data_db` | `userDataDataSource` | 用户隔离数据(动态表) |
25|
26|### 2.2 核心表
27|
28|| 表 | 关键字段 | 备注 |
29||----|---------|------|
30|| `book` | id, isbn(unique), title, author, publisher, stock, available, cover_path, price, **visible** | @SQLRestriction 软删除 |
31|| `ebook` | id, title, author, resource_name, file_type, chapter_count, file_size, **visible** | 同上 |
32|| `category` | id, name(unique), description, **parent_id** | 支持树形父子分类 |
33|| `book_categories` | book_id(FK), category_id(FK) | 多对多 |
34|| `ebook_categories` | ebook_id(FK), category_id(FK) | 多对多 |
35|| `borrow` | id, user_id, book_id, borrow_date, due_date, return_date, status | 状态:BORROWED/RETURNED/OVERDUE/CANCELLED |
36|| `reading_progress` | id, user_id, user_type, ebook_id, chapter_id, chapter_index, scroll_position | 唯一约束: (user_id,user_type,ebook_id) |
37|| `user_favorite` | id, user_id, user_type, target_type, target_id | 唯一约束: (user_id,user_type,target_type,target_id) |
38|| `super_admin` | id, username(unique), password, name, status | - |
39|| `admin_user` | id, username(unique), password, name, **permissions**, **visible** | permissions=逗号分隔权限码 |
40|| `normal_user` | id, username(unique), password, name, phone, email, **visible** | @SQLRestriction |
41|| `audit_log` | id, operator_type/id/name, operation_type, target_type/id, operation_detail, ip_address, execution_time | AOP 自动记录 |
42|| `login_log` | id, user_id, username, user_type, ip_address, status, error_message, user_agent | - |
43|| `token_blacklist` | id, token, username, expire_time | 登出后将 token 加入 |
44|| `role` | id, role_code(unique), role_name, description, status | (已废弃，Entity 及 Repository 已删除) |
45|| `permission` | id, permission_code(unique), permission_name, resource_type, resource_url | (已废弃，Entity 及 Repository 已删除) |
46|| `role_permission` | role_id, permission_id | (已废弃，Entity 及 Repository 已删除) |
47|
48|### 2.3 电子书独立章节表
49|
50|每本电子书独立建表（JdbcTemplate 操作，不经过 JPA）：
51|
52|```sql
53|CREATE TABLE ebook_chapters_{ebookId} (
54|    id BIGINT PRIMARY KEY,          -- = ebookId * 1000000 + chapterIndex
55|    chapter_index INT NOT NULL,
56|    title VARCHAR(500),
57|    content LONGTEXT,
58|    content_cached BOOLEAN DEFAULT TRUE,
59|    word_count INT DEFAULT 0
60|);
61|```
62|
63|上传时 `CREATE TABLE`，删除时 `DROP TABLE`。
64|
65|---
66|
67|## 三、缓存架构
68|
69|### 3.1 配置
70|
71|Redis `localhost:6379`，密码 `myredis123`。
72|
73|使用 `GenericJackson2JsonRedisSerializer` + `activateDefaultTyping(NON_FINAL)` + `BasicPolymorphicTypeValidator`（仅允许 `com.library.entity/dto` 包和标准集合）。
74|
75|### 3.2 各区域 TTL
76|
77|| 区域 | TTL | 缓存内容 |
78||------|-----|---------|
79|| `books` | 5min | 图书列表/按分类（搜索直查数据库） |
80|| `book` | 10min | 单本图书 |
81|| `ebooks` | 5min | 电子书列表/分类 |
82|| `ebook` | 10min | 电子书详情/章节/进度 |
83|| `categories` | 30min | 分类列表 |
84|| `ranking` | 10min | 排行数据 |
85|| `users` | 5min | 用户列表/认证（按角色隔离 key） |
86|| `admins` | 5min | 管理员列表 | 100|## 四、安全体系
   101|
   102|### 4.1 API 认证
   103|
   104|`SecurityConfig` 采用白名单模式：
   105|- **公开接口**（无需 token）：登录、注册、GET 浏览接口
   106|- **其余接口**：全部 `.anyRequest().authenticated()` 要求认证
   107|
   108|通过 `JwtAuthenticationFilter` 拦截请求，解析 JWT token 并设置 SecurityContext。
   109|
   110|### 4.2 三级角色 + 按角色查表
   111|
   112|| 角色 | 存储表 | 说明 |
   113||------|--------|------|
   114|| 超级管理员 `SUPER_ADMIN` | `super_admin` | 最高权限，独立登录页 `/super-admin/login` |
   115|| 管理员 `ADMIN` | `admin_user` | 由超管配置权限 |
   116|| 普通用户 `READER` | `normal_user` | 浏览/借阅/收藏/在线阅读 |
   117|
   118|登录时前端传 `role` 字段，后端使用 `LoginRoleContext(ThreadLocal)` 将角色传入 `MultiUserDetailsServiceImpl`，**只查对应表**，同名不同角色的用户互不干扰。
   119|
   120|### 4.3 权限注解
   121|
   122|| 注解 | 允许角色 |
   123||------|---------|
   124|| `@RequireSuperAdmin` | 超管 |
   125|| `@RequireAdmin` | 超管 + 管理员 |
   126|| `@PreAuthorize("@permissionChecker.hasPermission('CODE')")` | 超管 + 有权限的管理员 |
   127|
   128|### 4.4 整体安全措施
   129|
   130|| 措施 | 说明 |
   131||------|------|
   132|| JWT 24h + 黑名单 | Token 过期自动清理，登出立即失效 |
   133|| CORS 安全加固 | 限制 origin 到 `localhost:*`，method 白名单 |
   134|| 悲观写锁 | `@Lock(PESSIMISTIC_WRITE)` 防止并发超卖 |
   135|| 借阅天数限制 | 借阅上限 120 天，续借上限从借阅日起最长 365 天 |
   136|| 库存负数保护 | 减少库存时，新库存若低于已借出数量则拒绝 |
   137|| 归属校验 | 所有借阅操作校验当前用户是否为记录所有者或管理员 |
   138|| 文件类型校验 | `ImageFileValidator`：大小/Content-Type/扩展名/魔数 4 层 |
   139|| 全局异常处理 | `GlobalExceptionHandler` 统一处理 7 种异常类型，`BusinessException` 返回 400 + 应用级 errorCode |
   140|| 语义化错误码 | `ApiResponse.badRequest(400)` / `forbidden(403)` / `notFound(404)` / `conflict(409)` |
   141|
   142|---
   143|
   144|## 五、系统架构
   145|
   146|### 5.1 后端分层
   147|
   148|```
   149|Controller       → @RestController, 接收请求/返回 ApiResponse
   150|Service          → 业务逻辑 + Redis 缓存管理
   151|Repository       → Spring Data JPA, 数据库操作
   152|Entity           → JPA 实体, @SQLRestriction 软删除
   153|DTO              → 数据传输对象, 缓存 DTO 包装
   154|Security         → JWT, 认证过滤器, 权限检查
   155|Cache            → RedisCacheHelper, CacheDtoMapper
   156|Config           → 数据源, Redis, 事务管理器
   157|Util             → 工具类(ImageFileValidator, PasswordUtil)
   158|```
   159|
   160|### 5.2 前端分层
   161|
   162|```
   163|views/           → 页面组件 (Login/Register + 三级角色各模块)
   164|components/      → 公共组件 (Layout, ConfirmDialog)
   165|stores/          → Pinia 状态管理 (auth, book, borrow, ebook)
   166|api/             → Axios 封装 (book, borrow, ebook, auth, etc.)
   167|router/          → Vue Router 路由 + 导航守卫
   168|utils/           → request.js Axios 实例 + 拦截器
   169|directives/      → 自定义指令 (permission, role)
   170|styles/          → 全局主题 CSS
   171|```
   172|
   173|### 5.3 请求处理链
   174|
   175|```
   176|前端请求 → Axios 拦截器(加 Bearer token)
   177|         → Spring Security FilterChain
   178|         → JwtAuthenticationFilter(校验 token + 黑名单)
   179|         → Controller(检查 @RequireAdmin/@PreAuthorize)
   180|         → Service(业务逻辑 + 缓存)
   181|         → Repository(JPA + @Lock 悲观锁)
   182|         → MySQL/Redis
   183|         ← 返回 ApiResponse(code, success, message, data)
   184|         ← GlobalExceptionHandler(异常时统一拦截+打日志)
   185|         ← Axios 拦截器(401跳转登录/403弹权限不足/500弹服务器错误)
   186|```
   187|
   188|---
   189|
   190|## 六、定时任务
   191|
   192|| 任务 | 时间 | 说明 |
   193||------|------|------|
   194|| `LogCleanupScheduler` | 每日 3:00 | 清理 90 天前审计日志和登录日志 |
   195|| `JwtUtil.cleanExpiredTokens()` | 每日 2:00 | 清理过期 token 黑名单 |
   196|
   197|---
   198|
   199|## 七、配置
   200|
   201|```yaml
   202|# springboot/src/main/resources/application.yml
   203|server.port: 8080
   204|spring:
   205|  datasource:        # 5 数据源 (library_db/admin_db/user_db/admin_data_db/user_data_db)
   206|  jpa.hibernate.ddl-auto: update
   207|  servlet.multipart.max-file-size: 50MB
   208|  data.redis:
   209|    host: localhost
   210|    port: 6379
   211|    password: myredis123
   212|jwt:
   213|  secret: library-management-system-secret-key-for-jwt-token-generation
   214|  expiration: 86400000    # 24h
   215|```
   216|
   217|```javascript
   218|// vue3/vite.config.js
   219|server: { port: 5174, proxy: { '/api': 'http://localhost:8080' } }
   220|```
   221|
   222|---
   223|
   224|## 八、启动
   225|
   226|```bash
   227|# 1. 启动 MySQL 5 个库 + Redis
   228|# 2. 启动后端
   229|cd springboot && mvn spring-boot:run
   230|
   231|# 3. 启动前端
   232|cd vue3 && pnpm install && pnpm run dev
   233|# → http://localhost:5174
   234|```
   235|
   236|预置账号:
   237|
   238|| 角色 | 用户名 | 密码 |
   239||------|--------|------|
   240|| 超级管理员 | superadmin | 123456 |
   241|| 管理员 | admin | 123456 |
   242|| 普通用户 | reader | 123456 |
   243|
   244|---
   245|
   246|## 九、关键设计决策
   247|
   248|| 决策 | 原因 |
   249||------|------|
   250|| 电子书独立章节表 | 每本小说一张表，数据隔离，删除即 DROP |
   251|| JdbcTemplate 操作章节 | 动态表名无法用 JPA 实体映射 |
   252|| 章节 ID = ebookId*1M+index | 全局唯一，无需序列表 |
   253|| Redis 全业务缓存 | 高频查询加速，写入自动清除 |
   254|| @JsonSerialize(as=HashSet) | 防止 Hibernate PersistentSet 类型泄露到 Redis |
   255|| 软删除(visible) | 数据完整保留，误删可恢复 |
   256|| @SQLRestriction | Hibernate 自动过滤 visible=0 记录 |
   257|| 条件缓存认证 | `loadUserByUsername` + `isTokenBlacklisted` 缓存到 Redis |
   258|| LoginRoleContext(ThreadLocal) | 同名用户可按不同角色查对应表，互不干扰 |
   259|| 悲观写锁 | `@Lock(PESSIMISTIC_WRITE)` + `SELECT ... FOR UPDATE` 防止超卖 |
   260|| @PreAuthorize 细粒度权限 | 管理员分类增删权限可由超管动态分配 |
   261|| 文件魔数校验 | 防止修改扩展名绕过文件类型白名单 |
   262|| 全局异常处理器 | Controller 不再需要 try-catch，统一日志+统一返回格式 |
   263|| 语义化错误码 | 400/403/404/409/500 分别对应不同错误场景 |
   264|| 缓存 key 含角色前缀 | auth_READER_zhangsan 和 auth_ADMIN_zhangsan 互不冲突 |
   265|| UTF-8 BOM 下载 | Windows 记事本正确识别中文 |
   266|| Element Plus 中文 | 全局 zh-cn 语言包 |
   267|| sessionStorage | 关闭浏览器自动清除登录态 |
   268|| BusinessException | 业务异常与编程异常分离，支持应用级 errorCode |
   269|| 审计日志文件回退 | DB 写入失败时自动回退到 `logs/audit_fallback/` JSON 文件 |
   270|| CORS 白名单 | 仅允许 `localhost:*` 来源，禁止通配符 |
   271|| 日志文件滚动策略 | `logging.file` + `rollingpolicy`，按大小/时间自动轮转 |
   272|
87|| `borrows` | 3min | 借阅记录 |
88|| `blacklist` | 5min | token 黑名单 |
89|| `favorites` | 3min | 收藏 |
90|
91|### 3.3 缓存 key 设计
92|
93|认证缓存 key 包含角色前缀，防止同名不同角色的用户互相覆盖：
94|```
95|auth_READER_zhangsan     → NormalUser
96|auth_ADMIN_zhangsan      → AdminUser
97|auth_SUPER_ADMIN_root    → SuperAdmin
98|```
99|
