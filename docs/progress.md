# 项目进度

> 最后更新: 2026-05-26
> 当前阶段: Spec 003 实施中

## 进行中

### Spec 003: 用户收藏评分与行为记录
对应 spec: [docs/specs/003-user-behavior.md](./specs/003-user-behavior.md)

- [x] 2a: 完成用户行为记录需求访谈
- [x] 2b: 写出 spec 草稿
- [x] 2c: spec review 通过并写入已决策约束
- [x] 3a: 编写技术方案 plan 草稿
- [x] 3b: plan review 通过，开始按任务实施

启动于: 2026-05-26。当前步骤: T2 收藏和评分后端强状态 API。

Plan 任务拆解:

- [x] T1: Kafka 依赖、配置与数据库结构
- [ ] T2: 收藏和评分后端强状态 API
- [ ] T3: 用户感知论文列表、详情、我的收藏
- [ ] T4: Kafka 行为生产者、消费者和幂等落库
- [ ] T5: 隐式行为接入业务入口和最近浏览 Redis
- [ ] T6: 管理员全局行为统计、Redis 缓存和缓存重建锁
- [ ] T7: 前端收藏、评分、我的收藏和最近浏览
- [ ] T8: 前端行为上报和管理员统计面板
- [ ] T9: 文档、叙事和端到端验收

Spec 003 T1 验收记录:

- 新增 `spring-kafka` 依赖，并在 `application.yml` 中配置 Kafka bootstrap、producer、consumer group 和序列化配置。
- 新增 behavior 配置项: 行为 topic、统计缓存 key/TTL、统计缓存重建锁 key/TTL、最近浏览上限。
- 新增 `sql/003-user-behavior.sql`，包含 `paper_favorites`、`paper_ratings`、`paper_behavior_events` 三张表。
- 新增 `behavior/entity` 和 `behavior/mapper` 基础结构，并把 `com.lencode.paper.behavior.mapper` 加入 MyBatis-Plus 扫描。
- 验证: Java `mvn test` 通过，`Tests run: 95, Failures: 0, Errors: 0, Skipped: 4`。
- 旧 Repository 名称检查通过: 未发现 `ResearchTagRepository`、`PaperRepository`、`PaperTagRepository`、`UserRepository` 残留。

---

## 最近完成记录

### Spec 002: 论文信息与研究方向标签管理
对应 spec: [docs/specs/002-paper-and-tags.md](./specs/002-paper-and-tags.md)

- [x] 2a: 完成论文信息与研究方向标签管理需求访谈
- [x] 2b: 写出 spec 草稿
- [x] 2c: spec review 通过并写入已决策约束
- [x] 3a: 编写技术方案 plan 草稿
- [x] 3b: plan review 通过，开始按任务实施

暂停于: T10 已完成。下一步建议开始 Spec 003: 用户收藏评分与行为记录。

Plan 任务拆解:

- [x] T1: 数据库结构与固定两级标签种子
- [x] T2: 后端标签查询与管理员标签状态维护
- [x] T3: 后端论文提交、详情和分页列表
- [x] T4: 后端论文标签绑定和组合搜索
- [x] T4.5: Spring Boot 包结构规范化和 MyBatis-Plus Mapper 改造
- [x] T5: 管理论文编辑、软删除和恢复
- [x] T6: 论文下载尝试与下载记录
- [x] T7: arXiv 样例论文导入
- [x] T8: 前端论文列表、详情、提交和标签筛选
- [x] T9: 前端管理员维护、导入和下载入口
- [x] T10: 文档、端到端验收和进度收尾

Spec 002 T8 验收记录:

- 前端接入 `/api/tags`、`/api/papers`、`/api/papers/{id}` 和 `POST /api/papers`。
- 登录后 dashboard 进入论文工作台，支持标签筛选、标题/作者/年份/来源/摘要关键词搜索、分页、详情抽屉和普通用户提交论文。
- 详情页覆盖标题、作者、摘要、年份、来源、DOI、URL、下载链接、关键词、引用量、发布时间和标签，空字段显示为 `未填写`。
- 验证: `frontend` 目录下 `npm run build` 通过。
- Browser 冒烟: 通过 Vite + mock API 登录进入 dashboard，检查论文列表、详情抽屉、提交弹窗，并实际点击提交走过 `POST /api/papers`；824px 宽度下筛选区和表格不再撑破主界面。

Spec 002 T9 验收记录:

- 前端接入管理员论文编辑、软删除、回收站列表、恢复、arXiv 导入和论文下载尝试 API。
- 管理员入口通过 `role === 'ADMIN'` 控制显示；普通用户工作台不渲染导入、回收站、编辑和删除操作。
- 论文详情保留外部 URL 和下载链接，并支持登录用户触发下载尝试后展示状态、文件名、大小、本机路径和失败原因。
- 验证: `frontend` 目录下 `npm run build` 通过。
- Browser 冒烟: 通过 Vite + mock API 登录管理员账号，确认管理员入口可见，打开详情并触发下载尝试后展示 `SUCCESS` 和本机路径；浏览器工具随后触发用量限制，编辑/软删除/恢复/导入和普通用户隐藏管理按钮已通过构建和代码级条件渲染检查，完整真实端到端留到 T10 联调。

Spec 002 T10 验收记录:

- Java 测试通过: `Tests run: 95, Failures: 0, Errors: 0, Skipped: 0`。
- 前端构建通过: `npm run build` 成功，仅有 Rollup 注释位置和 chunk size 提示。
- 本机 Java `/api/health` 返回 MySQL `UP`、Redis `UP`、Python `DOWN`，整体 `DEGRADED`；Spec 002 不依赖 Python 推理服务。
- arXiv 真实导入通过: `requested=100, imported=100, skipped=0, failed=0`，论文总数达到 100。
- 真实 PDF 下载尝试通过: `SUCCESS`，文件保存到 `D:\code\codex_2\lencode\paper-downloads\paper-101-1779785086503-dummy.pdf`，大小 13264 bytes。
- 失败下载记录通过: 非法链接返回 `FAILED` 并记录失败原因。
- 管理员软删除、回收站查询、恢复通过；普通用户注册登录后可查看论文列表和详情。
- MySQL 复核: `papers=102`、`ACTIVE=102`、`DELETED=0`、下载记录 `SUCCESS=1`、`FAILED=1`、标签 `28`。
- 已补充 README 的 SQL 002 初始化、下载目录、arXiv 导入、下载/软删恢复验收命令，并新增 `docs/lessons/003-paper-and-tags-e2e.md` 和 `docs/decisions/002-paper-source-and-download-policy.md`。
- 路径 A 收尾复核: 2026-05-26 再次运行 Java 测试通过，`Tests run: 95, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run build` 通过，仅有既有 Rollup 注释和 chunk size warning。

---

## 已完成

- 阶段一: 项目起步和架构概述完成。
- Spec 001: 登录认证与项目基础骨架完成。
- Spec 002: 论文信息与研究方向标签管理完成。

Spec 001 T8 验收记录:

- MySQL: `fjy` 管理员账号存在，角色 `ADMIN`，状态 `ACTIVE`，密码不是明文。
- Redis: `redis-cli ping` 返回 `PONG`。
- Python: `http://127.0.0.1:8001/health` 返回 `{"status":"UP","service":"python-infer","modelReady":false}`，进程来自 `D:\Anaconda\Anaconda\envs\cd-agent\python.exe`。
- Java: Maven 测试通过，`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- Frontend: `npm run build` 通过。
- Browser E2E: `fjy/123456` 登录进入 `/dashboard`，显示 `fjy`、`ADMIN`、Java/Python 服务 `UP`、管理员接口可访问，浏览器错误日志为空。

Spec 001 追加约束记录:

- Java 后端使用正式 Spring Boot 分层包结构。
- 用户表访问使用 MyBatis-Plus `UserMapper`，不再新增 `JdbcTemplate Repository`。
- 登录、当前用户和管理员接口鉴权已改为 Spring Security 承载；Redis 简单 token 和 24 小时有效期保持不变。
- 验证: `mvn test` 通过，结果为 `Tests run: 75, Failures: 0, Errors: 0, Skipped: 4`。

---

## 待办(优先级排序)

### 高优先级

- [ ] Spec 004: 论文语义向量与相似度服务 - Python 服务提供真实 embedding 和相似度计算接口
- [ ] Spec 005: 推荐聚合、热门论文与偏好标签 - Java 服务聚合推荐分数、热门论文和用户偏好标签

### 中优先级

- [ ] 定时任务同步临时行为数据到 MySQL
- [ ] 科研行为分析指标接口
- [ ] 推荐结果解释字段
- [ ] 爬虫实时更新与来源适配器
- [ ] 持续维护项目叙事台账，补充每个 spec 的问题、方案、验证和简历表达

### 低优先级 / 想法池

- [ ] 批量论文导入
- [ ] 模型效果评估报表
- [ ] 前端管理台
- [ ] 论文全文解析

---

## 阻塞 / 待决策

- 待决策: embedding 模型具体选择和本地缓存方式。
- 已决策: Spec 002 完成后进入 Spec 003 行为记录，不在 Spec 002 里继续追加推荐、embedding 或行为统计。
- 已决策: Spec 002 样例论文优先使用 arXiv 公开 API 元数据。
- 已决策: Spec 002 真实下载文件保存到 git 仓库之外的本机目录，仓库只记录状态和路径。
- 已决策: Spec 002 PDF 只做本机下载尝试和状态记录，不在系统内对外二次分发；见 `docs/decisions/002-paper-source-and-download-policy.md`。
- 已决策: Spec 002 普通用户提交论文后第一版立即公开。
- 已决策: Spec 002 DOI 非空时按全局唯一处理。
- 已决策: Spec 001 前端采用 Vue/Vite + Element Plus。
- 已决策: Spec 001 登录认证使用 Spring Security + Redis 简单 token，token 有效期 24 小时，不引入 JWT refresh token。
- 已决策: Spec 001 用户表持久层使用 MyBatis-Plus Mapper，并保持正式 Spring Boot 分层包结构。
- 已决策: Spec 001 管理员账号通过 SQL 初始化，账号 `fjy`，初始密码 `123456`。
- 已决策: Spec 003 需要收藏、取消收藏和 1-5 星评分。
- 已决策: Spec 003 搜索行为只保留关键词、作者、年份。
- 已决策: Spec 003 重复浏览、搜索和点击按多次行为记录。
- 已决策: Spec 003 管理端第一版只做全局统计，不展示用户级明细。
- 已决策: Spec 003 使用 Kafka 承载隐式行为消息；Redis 用于最近浏览缓存、管理员统计缓存和缓存重建锁。
- 已决策: Spec 003 不做推荐、embedding 和热门论文排序。

---

## 已知问题 / 技术债

- Windows `Start-Process` 启动 Maven 时，`-Dmaven.repo.local=D:\study software\maven-repository` 必须整体加引号，否则会被拆成 `software\maven-repository` lifecycle phase。
- MySQL Shell 在沙箱用户下可能无法写 `mysqlsh.log`，需要在正常用户环境执行本地 DB 验收命令。

---

## 本周目标

- 启动 Spec 003: 用户收藏评分与行为记录。
- 不改动 Spec 002 主链路，除非修复验收发现的明确 bug。
