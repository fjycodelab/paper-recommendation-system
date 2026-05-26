# Java Service

Spring Boot 业务服务目录。

## 职责

- 提供注册、登录、当前用户和健康检查 API。
- 连接 MySQL 作为事实数据源。
- 使用 MyBatis-Plus Mapper 访问业务表。
- 登录鉴权按 Spec 001 约束接入 Spring Security，Redis token 作为 Bearer 凭证来源。
- 使用 Redis 保存 24 小时简单 token。
- 后续通过 WebClient 调用 `python-infer`。

## 当前状态

T8 已完成 Java 认证链路和端到端启动文档。当前服务包含 `/api/health`、注册、登录、`/api/auth/me` 和 `/api/admin/ping`。

2026-05-25 追加 Spec 001 约束已实现: 登录、当前用户和管理员权限改为 Spring Security 承载；用户表持久层使用 MyBatis-Plus `UserMapper`。

## 本机运行

不要把本地数据库密码写入提交文件。运行时用环境变量传入:

```powershell
$env:APP_DB_USERNAME="root"
$env:APP_DB_PASSWORD="root"
$env:APP_REDIS_HOST="127.0.0.1"
$env:APP_REDIS_PORT="6379"
& "D:\study software\apache-maven-3.9.13\bin\mvn.cmd" "-Dmaven.repo.local=D:\study software\maven-repository" test
& "D:\study software\apache-maven-3.9.13\bin\mvn.cmd" "-Dmaven.repo.local=D:\study software\maven-repository" spring-boot:run
```

健康检查:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health
```

注册普通用户:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8080/api/auth/register `
  -ContentType "application/json" `
  -Body '{"username":"demo","password":"123456"}'
```

使用初始化管理员登录并读取当前用户:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri http://127.0.0.1:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"fjy","password":"123456"}'

$token = $login.token
Invoke-RestMethod `
  -Uri http://127.0.0.1:8080/api/auth/me `
  -Headers @{ Authorization = "Bearer $token" }
```

管理员接口验证:

```powershell
Invoke-RestMethod `
  -Uri http://127.0.0.1:8080/api/admin/ping `
  -Headers @{ Authorization = "Bearer $token" }
```
