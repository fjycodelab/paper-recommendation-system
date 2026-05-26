# ADR 001: Spec 001 登录鉴权使用 Spring Security

> 日期: 2026-05-25
> 状态: 已接受，已实现
> 对应: Spec 001 登录认证与项目基础骨架

## 背景

Spec 001 已打通注册、登录、Redis 简单 token、当前用户和管理员接口。项目后端结构已规范为正式 Spring Boot 分层目录，用户表持久层使用 MyBatis-Plus Mapper。

后续论文、标签、行为和推荐接口都会依赖登录用户和角色判断。如果继续在 Controller 或业务方法里手写 token 解析，权限逻辑会分散且难维护。

## 决策

- 登录、当前用户和管理员权限统一接入 Spring Security。
- 前端仍通过 JSON API 登录，不使用 Spring Security 默认表单登录。
- 登录成功后继续签发 Redis 简单 token，不改为 JWT。
- token 有效期保持 24 小时。
- Bearer token 过滤器从 Redis 读取 token payload，并建立 Spring Security 当前用户上下文。
- `/api/auth/register`、`/api/auth/login`、`/api/health` 放行。
- `/api/auth/me` 和后续业务接口需要认证。
- `/api/admin/**` 需要管理员角色。

## 影响

- Java 服务已新增 Spring Security 依赖和 `SecurityConfig` 配置。
- 已自定义 401 和 403 响应，使错误格式与现有 API 保持一致。
- `AuthService` 继续负责注册、密码校验、token 签发和 Redis token 读取，受保护接口不再手写鉴权。
- 测试已覆盖匿名访问、错误 token、普通用户访问管理员接口、管理员访问成功和 token TTL。

## 不做

- 不引入 JWT refresh token。
- 不引入 OAuth2、单点登录或 Spring Authorization Server。
- 不使用 Cookie session 或服务端页面登录。
- 不扩展完整 RBAC；Spec 001 仍只区分 `USER` 和 `ADMIN`。
