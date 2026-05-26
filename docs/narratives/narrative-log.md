# 项目叙事台账

> 最后更新: 2026-05-26
> 原则: 只记录真实实现或明确计划；涉及提升数字时必须有测试或日志证据。

## 叙事总线

本项目可以讲成一条完整主线:

从科研文献管理场景出发，先建立登录认证、真实 MySQL/Redis、多服务启动和前端仪表盘，再沉淀论文元数据、研究方向标签和用户行为，最后通过 Python FastAPI + PyTorch embedding 服务实现论文相似度和个性化推荐。Java 服务负责业务聚合和缓存，Python 服务负责模型推理，前端负责简洁展示。

## Narrative 001: 本机三服务骨架与真实存储联通

> 对应: Spec 001 登录认证与项目基础骨架
> 状态: 已验证

### 场景

项目需要同时包含 Java 后端、Python 算法服务和前端展示，并且不是纯 mock，需要真实连接 MySQL 和 Redis，为后续论文、行为和推荐模块提供可继续扩展的基础。

### 问题

Windows 本机环境里路径包含空格，Maven、Python、Node、Redis 分散在不同安装目录；如果启动方式不固定，后续每次联调都会被环境问题拖慢。前端还需要确认 Java、MySQL、Redis、Python 是否都处于可用状态。

### 方案

将项目拆成 `java-service/`、`python-infer/`、`frontend/`、`sql/` 四个目录。Java 服务通过 Spring Boot 连接 MySQL/Redis，并提供健康检查；用户表持久层使用 MyBatis-Plus Mapper；登录鉴权由 Spring Security 承载，Redis 简单 token 作为 Bearer 凭证来源。Python 服务用 FastAPI 暴露 `/health`；前端用 Vue/Vite + Element Plus 展示综合仪表盘和服务状态。

### 落地

- `java-service/`: 登录认证、Redis token、管理员接口、健康检查；正式 Spring Boot 分层结构和 MyBatis-Plus 用户表访问。
- `python-infer/`: FastAPI 健康检查骨架。
- `frontend/`: 注册、登录、刷新恢复、综合仪表盘。
- `sql/001-auth-and-foundation.sql`: 用户表和管理员账号初始化。
- `docs/lessons/001-windows-local-stack.md`: Windows 多服务启动经验。

### 验证

- Java Maven 测试通过: `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- Spring Security 升级后 Java Maven 测试通过: `Tests run: 75, Failures: 0, Errors: 0, Skipped: 4`。
- 前端 `npm run build` 通过。
- Redis `ping` 返回 `PONG`。
- Python `/health` 返回 `UP`。
- 浏览器 E2E 使用 `fjy/123456` 登录进入仪表盘，显示 Java/Python 服务状态。

### 简历表达

- 搭建 Spring Boot + FastAPI + Vue 多服务项目骨架，接入真实 MySQL/Redis，并通过统一健康检查和前端仪表盘展示服务可用性。
- 将登录鉴权迁移到 Spring Security + Redis 简单 token，统一注册、登录、管理员接口和刷新恢复登录态的鉴权基础。

### 面试追问

- 问: 为什么不用 JWT refresh token?
  答: 第一版目标是快速打通真实存储和前后端链路，Redis token 更容易失效、调试和撤销；Spring Security 负责统一认证上下文和 401/403，后续需要跨服务或无状态扩展时再升级 JWT/refresh。
- 问: 为什么要单独做 Python 服务?
  答: 推荐模型和 embedding 推理更适合 Python/PyTorch，Java 保持业务聚合和数据访问职责，通过 HTTP 接口解耦。

## Narrative 002: 论文元数据与两级研究方向标签

> 对应: Spec 002 T1-T4
> 状态: 已验证

### 场景

推荐系统需要真实论文数据作为输入。仅有标题列表不足以支撑后续 embedding 和行为分析，因此需要沉淀论文标题、作者、摘要、年份、来源、关键词、下载入口以及研究方向标签。

### 问题

论文元数据字段天然不完整，很多公开来源可能缺少 DOI、引用量或下载链接；研究方向如果只用自由文本，后续统计用户偏好和推荐解释会很难稳定。

### 方案

设计固定两级研究方向标签库，论文只绑定二级标签；论文业务字段允许为空，但系统字段和状态完整。通过 MyBatis-Plus 管理论文、标签和关联关系，后端支持按标题、作者、年份、来源、标签、摘要关键词组合检索。

### 落地

- `sql/002-paper-and-tags.sql`: 论文、标签、论文标签关联和下载记录表。
- `java-service/src/main/java/com/lencode/paper/tag/**`: 标签查询和管理员维护。
- `java-service/src/main/java/com/lencode/paper/paper/**`: 论文提交、详情、分页列表和组合搜索。
- `docs/plans/002-paper-and-tags-plan.md`: T1-T10 已完成记录。

### 验证

- 已验证: Java Maven 测试通过，`Tests run: 95, Failures: 0, Errors: 0, Skipped: 0`。
- 已验证: 前端 `npm run build` 通过。
- 已验证: MySQL 中 `research_tags=28`，T10 真实导入后 `papers=102`。
- 已验证: 普通用户可提交、检索和查看论文；管理员可编辑、软删除、恢复论文。

### 简历表达

- 设计论文元数据与两级研究方向标签模型，支持论文多标签绑定和标题、作者、年份、来源、标签、摘要关键词组合检索，为后续语义推荐和用户偏好分析提供结构化数据基础。
- 针对论文公开数据字段不完整的问题，采用业务字段可空、系统字段强约束的建模方式，保证数据可入库、可检索、可逐步补全。

### 面试追问

- 问: 为什么固定两级标签，而不是无限层级?
  答: MVP 阶段目标是稳定支撑检索、偏好统计和推荐解释；两级标签足够表达主方向和子方向，也更容易做前端筛选和统计聚合。
- 问: 组合搜索怎么避免结果不准确?
  答: 多条件取交集，标签通过关联表过滤，文本字段使用明确字段匹配；后续可再升级全文索引或向量召回。

## Narrative 003: 下载与 arXiv 样例导入

> 对应: Spec 002 T6-T9
> 状态: 已验证

### 场景

系统需要约 100 条真实样例论文用于展示和后续推荐验证，同时需要保留论文下载入口，方便用户从详情页跳转或尝试下载。

### 问题

PDF 文件体积大且可能涉及许可约束，不适合提交到 git，也不应在系统内对外二次分发。公开 API 也可能存在网络失败、限流和元数据不完整问题。

### 方案

优先使用 arXiv 公开 API 导入元数据，PDF 只做本机下载尝试和状态记录。下载文件保存到 git 仓库之外，数据库只记录下载状态、本机路径、文件大小和失败原因。

### 落地

- 已实现: `paper_download_attempts` 下载状态记录，覆盖 `SUCCESS`、`FAILED`、`TIMEOUT`、`NON_PDF`、`NO_URL`。
- 已实现: `POST /api/papers/{id}/download-attempt`，登录用户可按论文 `downloadUrl` 发起下载尝试，PDF 保存到仓库外下载目录。
- 已实现: `POST /api/admin/papers/import/arxiv`，管理员可导入 arXiv Atom 元数据，解析标题、作者、摘要、发布时间、分类、abs URL、PDF URL 和 arXiv id。
- 已实现: arXiv 导入通过 `source='arXiv' + source_paper_id` 去重，并将 arXiv 分类映射到固定二级标签。
- 已实现: 前端管理员可触发 arXiv 导入、编辑论文、软删除论文并从回收站恢复。
- 已实现: 前端详情页保留外部链接和下载链接，并展示下载尝试状态、文件名、大小、本机路径和失败原因。
- 已沉淀: `docs/lessons/003-paper-and-tags-e2e.md` 记录 T10 端到端验收和 Windows/下载链路坑；`docs/decisions/002-paper-source-and-download-policy.md` 固化 arXiv 元数据和本机下载策略。

### 验证

- 已验证: Java Maven 测试通过，`Tests run: 95, Failures: 0, Errors: 0, Skipped: 0`。
- 已验证: 下载服务覆盖成功保存、无下载链接、非 PDF、超时、非法链接和论文不存在场景；HTTP 客户端使用 mock HTTP 响应验证。
- 已验证: arXiv Atom 解析、重复导入跳过、网络失败返回明确结果、管理员接口鉴权测试通过。
- 已验证: 前端 `npm run build` 通过。
- 已验证: Browser 冒烟通过 mock API 确认管理员入口可见，详情页下载尝试展示 `SUCCESS` 和本机路径。
- 已验证: T10 真实 arXiv 导入 `requested=100, imported=100, skipped=0, failed=0`。
- 已验证: T10 真实 PDF 下载返回 `SUCCESS`，保存文件 `paper-101-1779785086503-dummy.pdf`，大小 13264 bytes。
- 已验证: T10 非法链接下载返回 `FAILED` 并记录失败原因。
- 已验证: T10 管理员软删除、回收站查询、恢复，以及普通用户列表/详情读取均通过 API 级端到端验收。

### 简历表达

- 实现论文 PDF 本机下载尝试与状态记录机制，将成功、超时、非 PDF、无链接和失败原因写入 MySQL，并把文件保存到仓库外目录，避免大文件和版权风险进入代码仓库。
- 实现基于 arXiv Atom API 的样例论文导入，按 arXiv id 去重并映射固定研究方向标签，为后续推荐链路提供可复现样例数据。

### 面试追问

- 问: 为什么不把 PDF 直接存进项目?
  答: PDF 是大文件且涉及许可和分发风险，MVP 更需要元数据、摘要和下载状态；文件放项目外，仓库只保存可复现的代码和状态记录。
