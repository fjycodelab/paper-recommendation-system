# Plan 001: 登录认证与项目基础骨架 - 技术方案

> 创建日期: 2026-05-24
> 状态: 已完成；2026-05-25 追加 Spring Security 认证改造已实现
> 对应 spec: [docs/specs/001-auth-and-foundation.md](../specs/001-auth-and-foundation.md)

## 概述

本 plan 先建立可运行的四块骨架: `java-service/`、`python-infer/`、`frontend/`、`sql/`。Java 服务作为统一业务入口连接 MySQL 和 Redis，使用正式 Spring Boot 分层包结构和 MyBatis-Plus 访问用户表，完成注册、登录、token 鉴权和健康检查；Python 服务只提供可替换的推理服务健康检查；前端使用 Vue/Vite + Element Plus 提供注册、登录和综合仪表盘。

2026-05-25 追加约束: Spec 001 登录、当前用户和管理员接口鉴权已迁移到 Spring Security；Redis 简单 token、24 小时有效期和前端 Bearer token 交互保持不变。

Spec 001 的重点是打通真实基础链路，而不是提前实现论文 CRUD、爬虫、推荐模型或复杂权限系统。

## 涉及的既有代码评估

### 当前状态

- `docs/specs/001-auth-and-foundation.md`: 已通过，需求边界清楚，管理员初始化、token 时效、前端组件库和本机存储约定已确认；2026-05-25 追加 Spring Security 认证框架约束。
- `docs/architecture.md`: 已定义 Java/Python/前端解耦、MySQL 事实数据源、Redis 缓存和临时状态边界。
- `java-service/`: 已规范为正式 Spring Boot 包结构，`auth` 领域按 `controller/service/mapper/entity/dto/vo` 分层，用户表已使用 MyBatis-Plus `UserMapper`。
- `python-infer/`: 尚不存在。
- `frontend/`: 尚不存在。
- `sql/`: 尚不存在。

### 重构建议

- [x] **改动前重构**: Java 后端已规范为正式 Spring Boot 包结构，用户表持久层使用 MyBatis-Plus Mapper。
- [ ] **改动中重构**: 如果脚手架生成过多样例代码，只保留本 spec 必需文件。
- [x] **改动后重构**: Spring Security 改造完成后补充验收记录，确认旧手写鉴权入口被移除或只保留为服务层能力。

### 不重构的部分及理由

- `docs/architecture.md`: 作为当前架构基线保留，不在实现 plan 中改写架构范围。
- `docs/project-brief.md`: 项目定义已稳定，本 plan 只落地 Spec 001。

## 涉及的文件

### 新增

- `java-service/pom.xml` - Spring Boot 服务依赖和构建配置。
- `java-service/src/main/java/.../Application.java` - Java 服务启动入口。
- `java-service/src/main/java/.../auth/controller/` - 注册、登录、当前用户、管理员验证入口。
- `java-service/src/main/java/.../auth/service/` - 用户注册、登录校验、Redis token 签发和当前用户加载。
- `java-service/src/main/java/.../auth/mapper/` - MyBatis-Plus 用户表 Mapper。
- `java-service/src/main/java/.../auth/entity/` - MyBatis-Plus 用户实体。
- `java-service/src/main/java/.../auth/dto/` - 注册、登录和 token 载荷请求对象。
- `java-service/src/main/java/.../auth/vo/` - 登录、当前用户和管理员接口响应对象。
- `java-service/src/main/java/.../config/` - MyBatis-Plus 配置、密码编码器配置和 Spring Security 配置。
- `java-service/src/main/java/.../health/` - MySQL、Redis、Python 服务状态健康检查。
- `java-service/src/main/resources/application.yml` - 默认配置骨架，只放可提交的占位和环境变量读取。
- `java-service/src/test/java/.../` - Java 单元测试和 Web 层测试。
- `python-infer/requirements.txt` - FastAPI 服务最小依赖。
- `python-infer/app/main.py` - Python 健康检查入口和后续推理接口占位。
- `frontend/package.json` - Vue/Vite + Element Plus 前端依赖和脚本。
- `frontend/src/` - 注册页、登录页、仪表盘和 API 调用代码。
- `sql/001-auth-and-foundation.sql` - 创建项目数据库、用户表、索引和初始化管理员账号。
- `README.md` 或模块 README - 启动顺序、本机配置、验证命令。
- `.gitignore` - 排除构建产物、依赖目录、本地配置和密钥文件。

### 修改

- `docs/progress.md` - 标记 Spec 001 review 通过，记录 plan review 状态和任务拆解。
- `docs/lessons/` - Spec 完成后再按实际踩坑补充。
- `docs/decisions/` - 如果 Redis 本机安装方式或 token 设计形成长期约束，完成后补 ADR。

## 本机配置约定

### MySQL

- 本机初始化账号: `root`
- 本机初始化密码: `root`
- 默认端口: `3306`
- 项目数据库名: `paper_recommendation`
- 提交策略: SQL 可以提交；真实本地运行配置走环境变量或被忽略的本地配置文件。

### Redis

- 本机目录: `D:\study software\redis`
- 数据目录: `D:\study software\redis\data`
- 默认端口: `6379`
- 建议配置:
  - `bind 127.0.0.1`
  - `protected-mode yes`
  - `appendonly yes`
  - `dir ./data`

Redis 目录包含空格，实施时优先把 `redis.conf` 放在 `D:\study software\redis`，用相对 `dir ./data` 降低路径转义风险。若本机还没有 `redis-server.exe` 或等价启动方式，先确认安装来源和启动方式，再写配置。

### 认证默认值

- 初始化管理员账号: `fjy`
- 初始密码: `123456`
- 角色: `ADMIN`
- token 有效期: 24 小时
- Redis token key 建议: `auth:token:{token}`
- Redis token value 建议: 用户 id、账号、角色、过期时间的 JSON。
- 登录入口: `/api/auth/login` 仍为 JSON API，由业务服务校验账号密码后签发 Redis token。
- 鉴权入口: Spring Security Bearer token 过滤器从 Redis 读取 token payload 并建立认证上下文。

## 数据模型

### MySQL: `users`

```sql
CREATE DATABASE IF NOT EXISTS paper_recommendation
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE paper_recommendation;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users_username (username),
  KEY idx_users_role (role),
  KEY idx_users_status (status)
);
```

初始化 SQL 插入 `fjy` 时必须写入密码摘要，不写入明文 `123456`。实际实现时用 Java 服务一致的密码算法生成摘要，避免 SQL 初始化账号和登录校验算法不一致。

Java 实体使用 MyBatis-Plus 映射 `users` 表，必须包含无参构造、setter、`@TableName("users")`、`@TableId` 和必要的 `@TableField`。用户表访问统一通过 `UserMapper`，不新增 `UserRepository` 或 `JdbcTemplate` 查询。

### Redis: token

```text
key: auth:token:{token}
ttl: 24h
value:
{
  "userId": 1,
  "username": "fjy",
  "role": "ADMIN",
  "expiresAt": "ISO-8601 datetime"
}
```

## 接口设计

### Java 服务

```text
GET /api/health
Response: 200 {
  "status": "UP|DEGRADED|DOWN",
  "mysql": "UP|DOWN",
  "redis": "UP|DOWN",
  "pythonInfer": "UP|DOWN"
}

POST /api/auth/register
Body: { "username": string, "password": string }
Response: 200 { "id": number, "username": string, "role": "USER" }

POST /api/auth/login
Body: { "username": string, "password": string }
Response: 200 { "token": string, "expiresInSeconds": 86400, "user": { ... } }

GET /api/auth/me
Header: Authorization: Bearer {token}
Response: 200 { "id": number, "username": string, "role": string }

GET /api/admin/ping
Header: Authorization: Bearer {token}
Response: 200 { "ok": true } | 403 { "message": "无权限" }
```

### Spring Security 约束

- `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/health` 放行。
- `GET /api/auth/me` 和后续业务接口需要已登录认证。
- `/api/admin/**` 需要管理员角色。
- 前端继续使用 `Authorization: Bearer {token}`，不切换到 Cookie session 或表单登录。
- Redis token 仍是服务端状态，TTL 仍为 24 小时；本次不引入 JWT。
- 401/403 由 Spring Security 的 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 统一成现有 API 错误响应格式。

### Python 服务

```text
GET /health
Response: 200 {
  "status": "UP",
  "service": "python-infer",
  "modelReady": false
}
```

### 前端页面

- `/login`: 登录页面。
- `/register`: 注册页面。
- `/dashboard`: 登录后的综合仪表盘。

仪表盘展示四个区域: 热门论文、用户偏好标签、推荐结果、服务状态。Spec 001 阶段允许热门论文、偏好标签和推荐结果展示空状态或样例占位；服务状态应调用 Java 健康检查。

## 任务拆解

> 每个任务只做一个可验证切片。执行时一次只实现一个 T，不跨任务偷跑。

- [x] **T1**: 创建目录骨架、忽略规则和数据库初始化 SQL
  - 文件: `.gitignore`, `sql/001-auth-and-foundation.sql`, `README.md`
  - 内容: 创建 `java-service/`、`python-infer/`、`frontend/`、`sql/`；SQL 创建 `paper_recommendation`、`users` 表和 `fjy` 管理员账号。
  - 验证: 使用本机 MySQL 执行 SQL 后，能查到 `paper_recommendation.users` 和账号 `fjy`，且密码字段不是明文。

- [x] **T2**: 创建 Java Spring Boot 服务并打通 MySQL/Redis 健康检查
  - 文件: `java-service/pom.xml`, `java-service/src/main/**`, `java-service/src/test/**`
  - 内容: 服务启动、读取 MySQL/Redis 配置、提供 `/api/health`。
  - 验证: Java 测试通过；Redis 未启动时健康检查能明确显示 `redis: DOWN`，而不是吞异常。

- [x] **T3**: 实现 Java 用户模型、密码摘要和注册接口
  - 文件: `java-service/src/main/java/.../auth/**`, `java-service/src/test/java/.../auth/**`
  - 内容: `users` 表 MyBatis-Plus 映射、注册参数校验、账号唯一校验、密码摘要入库。
  - 验证: 空账号、空密码、重复账号、成功注册的测试通过；数据库不保存明文密码。

- [x] **T4**: 实现登录、24 小时 Redis token 和当前用户接口
  - 文件: `java-service/src/main/java/.../auth/**`, `java-service/src/test/java/.../auth/**`
  - 内容: 登录校验、生成简单 token、写入 Redis、TTL 24 小时、`/api/auth/me` 鉴权。
  - 验证: `fjy/123456` 能登录；token 在 Redis 中存在且 TTL 接近 24 小时；缺失、错误、过期 token 被拒绝。

- [x] **T4.5**: 将登录鉴权接入 Spring Security
  - 文件: `java-service/pom.xml`, `java-service/src/main/java/.../config/**`, `java-service/src/main/java/.../auth/**`, `java-service/src/test/java/.../auth/**`
  - 内容: 新增 Spring Security 依赖和 `SecurityConfig`；JSON 登录继续走 `/api/auth/login`；Bearer token 过滤器从 Redis 加载 token payload 并建立认证上下文；`/api/auth/me` 读取 Spring Security 当前用户；`/api/admin/**` 通过角色规则限制。
  - 验证: 未登录访问 `/api/auth/me` 返回 401；错误 token 返回 401；普通用户访问 `/api/admin/ping` 返回 403；管理员访问成功；注册和登录接口仍可匿名访问；Redis token TTL 仍为 24 小时；`mvn test` 通过。

- [x] **T5**: 实现管理员角色区分的最小受保护接口
  - 文件: `java-service/src/main/java/.../auth/**`, `java-service/src/test/java/.../auth/**`
  - 内容: 普通用户和管理员角色判断，提供 `/api/admin/ping` 作为权限验证入口。
  - 验证: `ADMIN` 可以访问，`USER` 返回 403。

- [x] **T6**: 创建 Python FastAPI 推理服务骨架
  - 文件: `python-infer/requirements.txt`, `python-infer/app/main.py`, `python-infer/README.md`
  - 内容: 提供 `/health`，预留后续 embedding 接口位置，不加载真实模型。
  - 验证: Python 服务启动后 `/health` 返回 UP；Java 健康检查能反映 Python 服务 UP/DOWN。

- [x] **T7**: 创建 Vue/Vite + Element Plus 前端登录链路和仪表盘
  - 文件: `frontend/package.json`, `frontend/src/**`
  - 内容: 注册页、登录页、token 本地保存、刷新恢复 `/api/auth/me`、仪表盘四块区域。
  - 验证: 前端构建通过；能用 `fjy/123456` 登录并进入仪表盘；Python 未启动时服务状态显示异常但页面可用。

- [x] **T8**: 补齐启动文档和端到端验收
  - 文件: `README.md`, 各模块 README, `docs/progress.md`
  - 内容: 写明 MySQL 初始化、Redis 本机目录配置、Java/Python/前端启动顺序和验证命令。
  - 验证: 按 README 从空库执行到登录仪表盘；记录测试结果；更新 progress。

## 关键技术决策

### 决策 1: 管理员账号通过 SQL 初始化

**理由**: 第一版不做完整管理员管理接口，SQL 初始化最小、可控、便于本机验收。  
**代价**: 初始密码需要在文档中说明，部署到其他环境时必须更换。  
**应对**: 数据库存储密码摘要；README 标注这是本机 MVP 初始账号；后续权限系统单独开 spec。

### 决策 2: 简单 token 存 Redis，TTL 24 小时

**理由**: 比 JWT + refresh token 简单，符合 MVP 登录需求，并能通过 Redis 主动失效；Spring Security 只负责认证过滤链、当前用户上下文和 401/403 处理。  
**代价**: Redis 不可用会影响登录和鉴权。  
**应对**: 健康检查暴露 Redis 状态；登录和鉴权错误必须有明确日志，不吞异常。

### 决策 2.5: 登录鉴权使用 Spring Security

**理由**: 认证和授权属于 Spring Boot 后端的标准横切能力，接入 Spring Security 后，后续论文、标签、行为等接口可以复用统一的 `SecurityContext` 和角色规则，避免每个 Controller 手写 token 检查。  
**代价**: 需要新增 security 配置、过滤器和测试；JSON API 登录、Redis token 与默认表单登录机制需要明确隔离。  
**应对**: 禁用表单登录和 CSRF；显式配置 permitAll、authenticated 和 ADMIN 规则；用自定义 Bearer token 过滤器桥接 Redis token。

### 决策 3: 前端采用 Vue/Vite + Element Plus

**理由**: 快速实现表单、提示、布局和仪表盘区域，减少手写 UI 代码。  
**代价**: 引入前端依赖和构建链路。  
**应对**: 前端范围只做登录、注册、仪表盘，不提前做复杂状态管理和管理台。

### 决策 4: Redis 本机数据放在 `D:\study software\redis`

**理由**: Redis 数据不应放进项目仓库，用户已指定本机软件目录。  
**代价**: 路径包含空格，Windows Redis 配置和启动命令容易出错。  
**应对**: 优先使用相对 `dir ./data`；README 中所有命令对路径加引号；实施时先验证 `redis-server.exe` 是否存在。

## 潜在风险

1. **Redis Windows 安装和路径带空格导致启动失败** - 应对: 先检测 `D:\study software\redis` 下的可执行文件和配置支持；配置文件放同目录并使用相对数据目录。
2. **多服务一次性铺太大导致联调困难** - 应对: 按 T1-T8 单任务推进，每个任务有独立验证，不提前做论文业务和推荐模型。
3. **本机 MySQL `root/root` 写进代码形成私有配置泄漏** - 应对: SQL 初始化可执行，代码配置只读取环境变量或本地 ignored 配置，仓库只提交示例。

## 不在本 plan 范围内

- JWT、refresh token、OAuth、找回密码、验证码。
- 完整 RBAC 权限系统。
- 论文 CRUD、爬虫实时抓取、推荐模型推理和用户行为分析。
- 前端管理台、复杂路由、复杂状态管理。
- 生产级 Redis/MySQL 部署、监控告警和备份策略。

## Plan Review 关注点

- `paper_recommendation` 作为项目数据库名是否可以。
- `fjy` 是否确定为管理员角色。
- Redis 是否优先复用 `D:\study software\redis` 下已有程序；如果没有，再确认安装方式。
- T1 是否先只做 SQL 和目录，还是同时生成 Java/Python/前端脚手架。

## 修订历史

- 2026-05-25: T4.5 完成，新增 Spring Security + Redis Bearer token 过滤器，Controller 不再手写 Authorization header 鉴权；`mvn test` 通过，结果为 `Tests run: 75, Failures: 0, Errors: 0, Skipped: 4`。
- 2026-05-25: 追加 Spec 001 认证改造计划: 正式 Spring Boot 分层结构和 MyBatis-Plus 用户表访问作为基线，登录鉴权待迁移到 Spring Security + Redis token。
- 2026-05-25: T8 完成，启动文档、端到端验收记录和 Windows 本机 lesson 已补齐。
- 2026-05-25: T7 完成，Vue/Vite + Element Plus 登录、注册、刷新恢复和仪表盘已通过浏览器验证。
- 2026-05-24: T6 完成，Python FastAPI `/health` 服务已启动验证，Java 健康检查可识别 `pythonInfer: UP`。
- 2026-05-24: T5 完成，新增 `/api/admin/ping` 并验证 ADMIN/USER 权限分流。
- 2026-05-24: T4 完成，`fjy/123456` 登录、24 小时 Redis token 和 `/api/auth/me` 已落地。
- 2026-05-24: T3 完成，Java 注册接口、BCrypt 密码摘要和 auth 测试已落地。
- 2026-05-24: 初稿，根据 Spec 001 review 决策整理。
