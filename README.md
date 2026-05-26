# 论文推荐与科研行为分析系统

面向科研文献管理场景的后端与算法服务项目。系统围绕论文信息管理、用户收藏评分、研究方向标签、论文语义相似度和个性化推荐结果生成展开。

当前 Spec 001 已完成登录认证与项目基础骨架，Spec 002 已完成论文信息、两级研究方向标签、arXiv 样例导入、下载尝试和前端论文工作台。Spec 003 正在实现用户收藏评分与行为记录。代码统一放在本目录下，按 Vibe Coding V3 的路径 A 推进：先 spec，再 plan，再按任务逐个实现和验证。

## 代码仓库

- GitHub: [fjycodelab/paper-recommendation-system](https://github.com/fjycodelab/paper-recommendation-system)
- 原始占位仓库名 `fjycodelab/java` 已改为 `paper-recommendation-system`，避免仓库名过泛，方便展示项目主题。
- 协作约定: 每完成一个可验证 task，或一小段连续本地提交后，将本地提交同步推送到线上仓库。
- 默认分支: `main`。

## 技术栈

- Java: Spring Boot, Spring Security, MyBatis-Plus, Spring WebClient
- 数据存储: MySQL, Redis
- 算法服务: Python, FastAPI, PyTorch
- 前端展示: Vue/Vite + Element Plus，保持简洁展示
- 协作方式: spec -> plan -> task -> test -> review -> lesson/ADR

## 目录

- `java-service/`: Spring Boot 业务服务，负责认证、业务 API、MySQL/Redis 和调用 Python 服务。
- `python-infer/`: FastAPI 推理服务，先提供健康检查，后续接入 embedding 和相似度接口。
- `frontend/`: Vue/Vite + Element Plus 前端，先提供注册、登录和综合仪表盘。
- `sql/`: 数据库初始化、迁移或样例数据。
- `docs/`: spec、plan、进度、经验和架构决策。

## MVP 范围

1. 论文信息、摘要、关键词、研究方向标签的管理能力。
2. 用户收藏、评分、访问等科研行为记录能力。
3. 基于摘要、关键词、研究方向生成语义向量并计算相似论文。
4. Java 服务调用 Python 推理服务，生成个性化推荐结果。
5. 基于 Redis 缓存热门论文、用户偏好标签和推荐结果。
6. 提供简洁前端页面展示登录、论文列表、热门论文、偏好标签和推荐结果。

## 当前状态

Spec 001 已完成:

- MySQL 初始化 SQL 和管理员账号 `fjy/123456`。
- Java Spring Boot 认证服务、健康检查、Spring Security + Redis token 鉴权和管理员接口；用户表访问使用 MyBatis-Plus。
- Python FastAPI `/health` 骨架，固定使用 Conda 环境 `cd-agent`。
- Vue/Vite + Element Plus 登录、注册、刷新恢复和仪表盘。

Spec 002 已完成:

- `sql/002-paper-and-tags.sql` 初始化论文、两级研究方向标签、论文标签关联和下载记录表。
- Java 服务提供论文提交、分页列表、详情、组合搜索、管理员编辑、软删除、恢复、标签维护、arXiv 导入和下载尝试接口。
- 前端论文工作台支持标签筛选、搜索、详情、提交、管理员维护、回收站恢复、arXiv 导入和下载状态展示。
- 2026-05-26 本机验收导入 arXiv 样例论文 100 条，完成 1 次真实 PDF 下载成功、1 次失败下载记录、管理员软删除恢复和普通用户检索。

下一步从 `docs/progress.md` 中选择 Spec 003 或后续任务继续推进。

## 本机基础配置

### MySQL

- 默认连接: `root/root@127.0.0.1:3306`
- 项目数据库: `paper_recommendation`
- 初始化 SQL: `sql/001-auth-and-foundation.sql`

使用 MySQL Shell 执行:

```powershell
mysqlsh --sql root:root@127.0.0.1:3306 --file sql/001-auth-and-foundation.sql
```

如果 `mysqlsh` 不在 PATH，使用全路径:

```powershell
& "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe" --sql root:root@127.0.0.1:3306 --file sql/001-auth-and-foundation.sql
```

初始化后会创建管理员账号:

- 账号: `fjy`
- 初始密码: `123456`
- 角色: `ADMIN`

数据库只保存 BCrypt 密码摘要，不保存明文密码。

### Redis

- Redis 程序目录: `D:\study software\redis\Redis-8.6.3-Windows-x64-msys2-with-Service`
- Redis 数据目录: `D:\study software\redis\data`
- 默认端口: `6379`

验证:

```powershell
& "D:\study software\redis\Redis-8.6.3-Windows-x64-msys2-with-Service\redis-cli.exe" ping
```

应返回 `PONG`。

## Spec 001 启动顺序

以下命令默认从 `D:\code\codex_2\lencode\javacode` 执行。路径包含空格时必须保留引号。

### 1. 初始化 MySQL

```powershell
mysqlsh --sql root:root@127.0.0.1:3306 --file sql/001-auth-and-foundation.sql
```

如果当前 shell 找不到 `mysqlsh`:

```powershell
& "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe" --sql root:root@127.0.0.1:3306 --file sql/001-auth-and-foundation.sql
```

验证管理员账号:

```powershell
mysqlsh --sql root:root@127.0.0.1:3306 --execute "SELECT username, role, status FROM paper_recommendation.users WHERE username='fjy';"
```

### 2. 确认 Redis

```powershell
& "D:\study software\redis\Redis-8.6.3-Windows-x64-msys2-with-Service\redis-cli.exe" ping
```

应返回 `PONG`。Redis 配置和数据目录位于:

- 配置: `D:\study software\redis\redis.local.conf`
- 数据: `D:\study software\redis\data`

### 3. 启动 Python 服务

Python 服务固定使用 `cd-agent`:

```powershell
cd D:\code\codex_2\lencode\javacode\python-infer
& "D:\Anaconda\Anaconda\envs\cd-agent\python.exe" -m pip install -r requirements.txt
.\run-dev.ps1
```

验证:

```powershell
Invoke-RestMethod http://127.0.0.1:8001/health
```

### 4. 启动 Java 服务

```powershell
cd D:\code\codex_2\lencode\javacode\java-service
$env:APP_DB_USERNAME="root"
$env:APP_DB_PASSWORD="root"
$env:APP_REDIS_HOST="127.0.0.1"
$env:APP_REDIS_PORT="6379"
& "D:\study software\apache-maven-3.9.13\bin\mvn.cmd" "-Dmaven.repo.local=D:\study software\maven-repository" spring-boot:run
```

验证:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health
```

### 5. 启动前端

```powershell
cd D:\code\codex_2\lencode\javacode\frontend
npm install
npm run dev
```

访问:

```text
http://127.0.0.1:5173/login
```

使用 `fjy / 123456` 登录后应进入仪表盘，并看到 Java、MySQL、Redis、Python 推理服务状态。

如果 8080 被旧 Java 进程占用，可以临时把当前 Java 服务启动到其他端口，并指定前端代理目标:

```powershell
$env:JAVA_SERVICE_PORT="18080"
& "D:\study software\apache-maven-3.9.13\bin\mvn.cmd" "-Dmaven.repo.local=D:\study software\maven-repository" spring-boot:run

$env:VITE_API_PROXY_TARGET="http://127.0.0.1:18080"
npm run dev
```

## Spec 002 初始化和验收

以下命令默认从 `D:\code\codex_2\lencode\javacode` 执行。

### 1. 初始化论文和标签表

先执行 Spec 001，再执行 Spec 002:

```powershell
& "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe" --sql root:root@127.0.0.1:3306 --file sql/001-auth-and-foundation.sql
& "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe" --sql root:root@127.0.0.1:3306 --file sql/002-paper-and-tags.sql
```

验证基础数据:

```powershell
& "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysqlsh.exe" --sql root:root@127.0.0.1:3306 --execute "SELECT COUNT(*) AS tags FROM paper_recommendation.research_tags; SELECT COUNT(*) AS papers FROM paper_recommendation.papers;"
```

### 2. 下载目录

默认下载目录在 git 仓库之外:

```text
D:\code\codex_2\lencode\paper-downloads
```

可用环境变量覆盖:

```powershell
$env:APP_PAPER_DOWNLOAD_DIR="D:\code\codex_2\lencode\paper-downloads"
$env:APP_PAPER_DOWNLOAD_TIMEOUT_MS="8000"
```

不要把下载到的 PDF 提交进 git。仓库只记录下载状态、文件名、大小和本机路径。

### 3. 启动服务

Java 服务:

```powershell
cd D:\code\codex_2\lencode\javacode\java-service
$env:APP_DB_USERNAME="root"
$env:APP_DB_PASSWORD="root"
$env:APP_REDIS_HOST="127.0.0.1"
$env:APP_REDIS_PORT="6379"
& "D:\study software\apache-maven-3.9.13\bin\mvn.cmd" "-Dmaven.repo.local=D:\study software\maven-repository" spring-boot:run
```

前端:

```powershell
cd D:\code\codex_2\lencode\javacode\frontend
npm run dev
```

Spec 002 不依赖 Python 推理服务。若 Python 未启动，`/api/health` 可能显示 `DEGRADED`，但论文、标签、导入和下载链路仍可验收。

### 4. arXiv 导入

登录管理员并导入 100 条样例:

```powershell
$base = "http://127.0.0.1:8080"
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType "application/json" -Body '{"username":"fjy","password":"123456"}'
$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Method Post -Uri "$base/api/admin/papers/import/arxiv" -Headers $headers -ContentType "application/json" -Body '{"query":"cat:cs.AI","maxResults":100}'
Invoke-RestMethod -Method Get -Uri "$base/api/papers?page=1&pageSize=10"
```

导入请求字段是 `query` 和 `maxResults`，最大一次 100 条；重复导入会按 `source + sourcePaperId` 跳过已有论文。

### 5. 下载、软删除和恢复验收

论文详情页可以触发下载尝试；API 形式如下:

```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/papers/{paperId}/download-attempt" -Headers $headers
Invoke-RestMethod -Method Delete -Uri "$base/api/admin/papers/{paperId}" -Headers $headers
Invoke-RestMethod -Method Get -Uri "$base/api/admin/papers/deleted?page=1&pageSize=20" -Headers $headers
Invoke-RestMethod -Method Post -Uri "$base/api/admin/papers/{paperId}/restore" -Headers $headers
```

2026-05-26 T10 本机验收结果:

- arXiv 导入: requested 100, imported 100, skipped 0, failed 0。
- 下载成功: `SUCCESS`，文件 `paper-101-1779785086503-dummy.pdf`，13264 bytes，保存到 `D:\code\codex_2\lencode\paper-downloads`。
- 下载失败: 非法下载链接返回 `FAILED`，记录失败原因。
- 管理员软删除后回收站可见，恢复后状态回到 `ACTIVE`。
- 普通用户可查看论文列表和详情。

## 通用验收命令

Java 测试:

```powershell
cd D:\code\codex_2\lencode\javacode\java-service
$env:APP_DB_USERNAME="root"
$env:APP_DB_PASSWORD="root"
$env:APP_REDIS_HOST="127.0.0.1"
$env:APP_REDIS_PORT="6379"
& "D:\study software\apache-maven-3.9.13\bin\mvn.cmd" "-Dmaven.repo.local=D:\study software\maven-repository" test
```

前端构建:

```powershell
cd D:\code\codex_2\lencode\javacode\frontend
npm run build
```
