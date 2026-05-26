# Spec 001: 登录认证与项目基础骨架

> 创建日期: 2026-05-24
> 状态: 已完成；2026-05-25 追加认证框架约束已实现
> 对应 plan: [docs/plans/001-auth-and-foundation-plan.md](../plans/001-auth-and-foundation-plan.md)

## 目标

让用户可以注册、登录并进入综合仪表盘，同时建立 Java 业务服务、Python 算法服务和前端展示三块互相独立的最小可运行骨架。

这个 spec 完成后，项目应具备后续论文管理、用户行为记录、推荐算法和前端展示继续扩展的基础，而不是只停留在空目录。

## 用户故事

1. 作为新用户，我想要注册账号，以便使用论文推荐与科研行为分析系统。
2. 作为已注册用户，我想要通过账号密码登录，以便访问综合仪表盘。
3. 作为普通用户，我想要看到论文、热门论文、偏好标签和推荐结果的展示区域，以便理解系统未来会提供什么信息。
4. 作为管理员，我想要和普通用户区分角色，以便后续扩展论文维护、爬虫管理和系统管理功能。
5. 作为开发者，我想要 Java、Python、前端分别在独立文件夹中启动，以便业务系统、算法服务和展示层解耦开发。
6. 作为开发者，我想要系统真实连接 MySQL 和 Redis，以便后续功能直接基于真实存储继续开发。

## 功能需求

- [x] 项目根目录下建立独立模块目录: `java-service/`、`python-infer/`、`frontend/`、`sql/`。
- [x] Java 服务使用正式 Spring Boot 工程结构，作为统一业务入口，提供注册、登录、当前用户信息和基础健康检查 API。
- [x] Java 后端包结构按领域拆分，并在领域内使用 `controller/service/mapper/entity/dto/vo` 分层。
- [x] 用户表访问使用 MyBatis-Plus `UserMapper` 和实体映射，不再为用户表新增 `JdbcTemplate Repository`。
- [x] 登录、当前用户和管理员接口鉴权改为 Spring Security 承载，Redis 简单 token 作为 Spring Security 认证凭证来源。
- [x] Python 服务使用 FastAPI，提供基础健康检查 API，并保留后续 embedding 推荐模型接入位置。
- [x] 前端提供注册页、登录页和登录后的综合仪表盘页面。
- [x] 综合仪表盘至少包含热门论文、用户偏好标签、推荐结果、服务状态四类展示区域；真实业务数据未完成时允许显示空状态或样例占位。
- [x] 用户注册时写入 MySQL 用户表，至少包含账号、密码摘要、角色、状态、创建时间、更新时间。
- [x] 支持普通用户和管理员两类角色，默认注册为普通用户；管理员可以通过初始化数据或后续配置创建。
- [x] 初始化 SQL 创建管理员账号 `fjy`，初始密码为 `123456`，数据库中只保存密码摘要。
- [x] 用户登录成功后返回简单 token，不使用 JWT refresh token。
- [x] token 存储在 Redis 中，有效期为 24 小时，并能被 Java 服务用于后续受保护接口的鉴权。
- [x] 密码无复杂强度要求，但必须非空，并且数据库只能保存密码摘要，不能保存明文密码。
- [x] 前端请求受保护接口时携带 token，刷新页面后仍能从本地状态恢复登录态。
- [x] Java 服务启动时使用真实 MySQL 和 Redis 配置；本机 MySQL 使用 `root/root` 初始化项目库，运行配置通过本地配置或环境变量提供，不提交真实私有配置。
- [x] Redis 使用本机目录 `D:\study software\redis`，数据目录配置到 `D:\study software\redis\data`。
- [x] 提供数据库初始化 SQL，能创建本 spec 所需的用户表和必要索引。
- [x] 三个服务的启动方式在 README 或模块文档中写清楚。

## 边界条件

- **账号为空**: 注册和登录都返回明确的参数错误。
- **密码为空**: 注册和登录都返回明确的参数错误。
- **账号重复注册**: 注册失败，提示账号已存在。
- **账号不存在**: 登录失败，提示账号或密码错误。
- **密码错误**: 登录失败，提示账号或密码错误。
- **用户被禁用**: 登录失败，提示账号不可用。
- **token 缺失**: 访问受保护接口返回未登录。
- **token 无效或过期**: 访问受保护接口返回登录已失效。
- **普通用户访问管理员接口**: 返回无权限。
- **Spring Security 配置缺失或错误**: `/api/auth/me`、`/api/admin/**` 必须由 Security 过滤链保护，不能回退成控制器里手写鉴权。
- **MySQL 连接失败**: 服务健康检查能暴露数据库不可用状态，启动或接口失败时有明确错误日志。
- **Redis 连接失败**: 登录或鉴权不可用时有明确错误日志，健康检查能暴露缓存不可用状态。
- **Python 服务未启动**: Java 和前端基础登录链路不应因此完全不可用；仪表盘的算法服务状态显示异常。
- **前端刷新页面**: 已保存 token 时尝试恢复当前用户；恢复失败则回到登录页。
- **多个浏览器会话登录**: 第一版允许同一用户存在多个有效 token。

## 不在范围内

- 不做 JWT、refresh token、单点登录或 OAuth。
- 不做短信登录、邮箱验证、找回密码、验证码。
- 不做复杂密码强度策略和登录失败锁定策略。
- 不做完整 RBAC 权限系统，只保留普通用户和管理员角色。
- 不引入 Spring Security OAuth2、Spring Authorization Server 或表单登录页面；前端仍使用 JSON API 登录。
- 不做论文 CRUD、爬虫实时更新、推荐模型推理和用户行为分析的完整实现。
- 不做复杂前端路由、状态管理和管理台，只做简洁展示页面。
- 不提交真实 MySQL/Redis 密码、个人账号或本地私有配置。

## 验收标准

- [x] `java-service/`、`python-infer/`、`frontend/`、`sql/` 目录存在且职责清晰。
- [x] MySQL 执行初始化 SQL 后能创建本 spec 所需表结构。
- [x] 初始化 SQL 执行后能创建项目数据库，并插入管理员账号 `fjy`。
- [x] Java 服务能连接真实 MySQL 和 Redis，并通过健康检查返回状态。
- [x] Python FastAPI 服务能启动，并通过健康检查返回状态。
- [x] 前端能启动并展示注册、登录和综合仪表盘页面。
- [x] 用户可以完成注册，并在 MySQL 中看到对应用户记录。
- [x] 用户可以完成登录，并获得简单 token。
- [x] token 存在 Redis 中，使用该 token 能访问当前用户信息接口。
- [x] 未携带 token 访问受保护接口会被拒绝。
- [x] 普通用户和管理员角色能被区分。
- [x] Spring Security 过滤链明确放行 `/api/auth/register`、`/api/auth/login`、`/api/health`，保护 `/api/auth/me` 和业务接口，并限制 `/api/admin/**` 只有管理员访问。
- [x] Bearer token 由 Spring Security 过滤器从 Redis 读取并转换为当前认证用户，token TTL 仍为 24 小时。
- [x] 认证失败返回 401，权限不足返回 403，错误响应格式与现有 API 错误格式保持一致。
- [x] 综合仪表盘能展示热门论文、用户偏好标签、推荐结果、服务状态四块区域。
- [x] Java、Python、前端的基础测试或健康检查验证通过。
- [x] README 或模块文档写明启动顺序、必要配置和验证方式。

## 已决策

- 管理员账号第一版通过 SQL 初始化，不提供创建管理员接口。
- 初始化管理员账号为 `fjy`，初始密码为 `123456`，角色为管理员，密码必须以摘要形式入库。
- 简单 token 有效期为 24 小时。
- 登录和受保护接口鉴权使用 Spring Security；Redis token 不改为 JWT。
- 用户表持久层使用 MyBatis-Plus Mapper，保持正式 Spring Boot 分层包结构。
- 前端正式采用 Vue/Vite + Element Plus。
- 本机 MySQL 使用 `root/root` 初始化项目数据库和表。
- Redis 目录使用 `D:\study software\redis`，数据目录使用 `D:\study software\redis\data`。

## 修订历史

- 2026-05-25: Spring Security + Redis 简单 token 登录鉴权升级完成，Java Maven 测试通过: `Tests run: 75, Failures: 0, Errors: 0, Skipped: 4`。
- 2026-05-25: 追加 Spec 001 实现约束: Java 使用正式 Spring Boot 分层结构和 MyBatis-Plus；登录鉴权改为 Spring Security + Redis 简单 token。
- 2026-05-25: Spec 001 实施完成，T8 验收通过。
- 2026-05-24: 初稿，根据架构访谈和用户确认的认证、角色、真实存储、前端仪表盘要求整理。
- 2026-05-24: Spec review 通过，确认 SQL 初始化管理员、24 小时 token、Vue/Vite + Element Plus、本机 MySQL/Redis 约定。
