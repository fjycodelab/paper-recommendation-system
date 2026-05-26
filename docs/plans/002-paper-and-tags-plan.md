# Plan 002: 论文信息与研究方向标签管理 - 技术方案

> 创建日期: 2026-05-25
> 状态: 已完成
> 对应 spec: [docs/specs/002-paper-and-tags.md](../specs/002-paper-and-tags.md)

## 概述

本 plan 在现有登录认证骨架上新增论文、两级研究方向标签、论文检索、软删除恢复、下载尝试和 arXiv 样例数据导入能力。Java 服务继续作为统一 API 入口，前端只调用 Java；Python 推理服务不参与本 spec。

实现策略是先落数据库结构和后端核心 API，再接前端页面，最后补 arXiv 导入与下载验收。下载功能遵守 arXiv API 使用约束: 元数据可导入，PDF 只做本机个人/研究用途下载验证，不把 PDF 提交进 git，也不在本系统中对外二次分发。

## 涉及的既有代码评估

### 当前状态

- `java-service/src/main/java/com/lencode/paper/auth/`: 认证、当前用户和管理员权限已有基础能力，后续论文接口应复用 `AuthService` 校验 token 和管理员角色。
- `java-service/src/main/java/com/lencode/paper/auth/ApiExceptionHandler.java`: 已统一处理 400/401/403/409，论文模块可复用现有异常类型，必要时只补 404。
- `java-service/src/main/java/com/lencode/paper/health/HealthService.java`: 已使用 `RestTemplate`，arXiv 导入和下载尝试可复用 Spring Boot 自带 HTTP 客户端能力，不新增依赖。
- `java-service/src/main/resources/application.yml`: 已有可提交的环境变量配置风格，需要新增下载目录、下载超时、arXiv 查询配置。
- `frontend/src/App.vue`: 当前是单文件登录/仪表盘实现，Spec 002 会明显变大，实施时应先拆出 API 和页面组件，避免继续堆在一个文件里。
- `frontend/src/api.js`: 已封装 token 和请求错误处理，可扩展论文、标签、下载和导入 API。
- `sql/001-auth-and-foundation.sql`: 已创建用户表和管理员种子；Spec 002 应新增独立 SQL 文件，不改写 001 的历史语义。

### 重构建议

- [x] **改动前重构**: 前端实现论文页面前，先把 `App.vue` 中的认证/仪表盘逻辑拆成小组件或至少拆出清晰状态区域，避免单文件继续膨胀。
- [x] **改动中重构**: 后端保持每个领域一个包，例如 `paper`、`tag`、`download`、`importer`，不要把论文逻辑塞进 `auth` 包。
- [x] **改动后重构**: Spec 002 完成后，把 arXiv 导入和下载策略沉淀到 `docs/lessons/` 或 ADR。

### 不重构的部分及理由

- `AuthService`: 只复用 `currentUser` 和 `requireAdmin`，不改登录认证行为，避免回归 Spec 001。
- `HealthService`: 本 spec 不改变服务健康检查，下载和导入失败不应影响基础健康状态。
- `python-infer/`: 本 spec 不涉及 embedding 或相似度，不动 Python 推理服务。

## 涉及的文件

### 新增

- `sql/002-paper-and-tags.sql` - 论文、标签、关联表、下载记录和样例标签初始化 SQL。
- `java-service/src/main/java/com/lencode/paper/common/NotFoundException.java` - 统一 404 异常。
- `java-service/src/main/java/com/lencode/paper/paper/**` - 论文模型、仓库、服务、控制器、DTO。
- `java-service/src/main/java/com/lencode/paper/tag/**` - 两级标签模型、仓库、服务、控制器、DTO。
- `java-service/src/main/java/com/lencode/paper/download/**` - 论文下载尝试、文件元数据记录和状态 DTO。
- `java-service/src/main/java/com/lencode/paper/importer/**` - arXiv 元数据导入服务、Atom XML 解析和导入结果 DTO。
- `java-service/src/test/java/com/lencode/paper/paper/**` - 论文提交、检索、软删除、恢复测试。
- `java-service/src/test/java/com/lencode/paper/tag/**` - 标签层级和状态测试。
- `java-service/src/test/java/com/lencode/paper/download/**` - 下载成功、超时、非 PDF、失败记录测试。
- `java-service/src/test/java/com/lencode/paper/importer/**` - arXiv Atom 解析和去重导入测试。
- `frontend/src/components/**` - 论文列表、详情、表单、标签筛选、管理操作组件。

### 修改

- `docs/specs/002-paper-and-tags.md` - 标记 spec review 通过。
- `docs/progress.md` - 记录 Plan 002 状态和任务拆解。
- `java-service/src/main/resources/application.yml` - 新增下载目录、下载超时、arXiv API 配置。
- `java-service/src/main/java/com/lencode/paper/auth/ApiExceptionHandler.java` - 补充 404 处理。
- `frontend/src/api.js` - 新增论文、标签、下载、导入 API 调用。
- `frontend/src/App.vue` - 接入论文工作台或路由式页面切换。
- `frontend/src/styles.css` - 增加论文列表、详情、管理表单的布局样式。
- `README.md` - 补充 SQL 002、样例数据导入、下载目录和验收命令。
- `.gitignore` - 排除本机下载目录、临时导入日志和测试下载文件。

## 数据模型

### `research_tags`

```sql
CREATE TABLE research_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  level TINYINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_research_tags_parent_name (parent_id, name),
  KEY idx_research_tags_parent (parent_id),
  KEY idx_research_tags_status (status),
  CONSTRAINT fk_research_tags_parent FOREIGN KEY (parent_id) REFERENCES research_tags(id)
);
```

约束: `level=1` 的标签 `parent_id` 为空；`level=2` 的标签必须有一级父标签。论文只能绑定二级标签。

### `papers`

```sql
CREATE TABLE papers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(512) NULL,
  authors TEXT NULL,
  abstract_text TEXT NULL,
  publish_year INT NULL,
  source VARCHAR(128) NULL,
  source_paper_id VARCHAR(128) NULL,
  doi VARCHAR(255) NULL,
  source_url VARCHAR(1024) NULL,
  download_url VARCHAR(1024) NULL,
  keywords VARCHAR(1000) NULL,
  citation_count INT NULL,
  published_at DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  submitted_by BIGINT NULL,
  deleted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_papers_doi (doi),
  UNIQUE KEY uk_papers_source_id (source, source_paper_id),
  KEY idx_papers_year (publish_year),
  KEY idx_papers_source (source),
  KEY idx_papers_status (status),
  KEY idx_papers_submitted_by (submitted_by),
  CONSTRAINT fk_papers_submitted_by FOREIGN KEY (submitted_by) REFERENCES users(id)
);
```

说明: MySQL 唯一索引允许多个 `NULL`，因此 DOI 为空不影响大量样例数据；arXiv 导入通过 `source + source_paper_id` 去重。

### `paper_tags`

```sql
CREATE TABLE paper_tags (
  paper_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (paper_id, tag_id),
  KEY idx_paper_tags_tag (tag_id),
  CONSTRAINT fk_paper_tags_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_paper_tags_tag FOREIGN KEY (tag_id) REFERENCES research_tags(id)
);
```

### `paper_download_attempts`

```sql
CREATE TABLE paper_download_attempts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  requested_by BIGINT NULL,
  download_url VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NULL,
  file_size BIGINT NULL,
  local_file_path VARCHAR(1024) NULL,
  failure_reason VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_download_attempts_paper (paper_id),
  KEY idx_download_attempts_status (status),
  CONSTRAINT fk_download_attempts_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_download_attempts_user FOREIGN KEY (requested_by) REFERENCES users(id)
);
```

下载状态建议: `SUCCESS`、`FAILED`、`TIMEOUT`、`NON_PDF`、`NO_URL`。

## 接口设计

### 标签接口

```text
GET /api/tags
Response: 200 [
  { id, name, level: 1, children: [{ id, name, level: 2 }] }
]

POST /api/admin/tags
Header: Authorization: Bearer {token}
Body: { parentId?: number, name: string, sortOrder?: number }
Response: 200 { id, parentId, name, level, status }

PATCH /api/admin/tags/{id}/status
Header: Authorization: Bearer {token}
Body: { status: "ACTIVE" | "DISABLED" }
Response: 200 { id, status }
```

### 论文接口

```text
GET /api/papers?page=1&pageSize=10&title=&author=&year=&source=&tagId=&abstractKeyword=
Response: 200 { items: [...], total, page, pageSize }

POST /api/papers
Header: Authorization: Bearer {token}
Body: {
  title?, authors?, abstractText?, publishYear?, source?, doi?,
  sourceUrl?, downloadUrl?, keywords?, citationCount?, publishedAt?, tagIds?
}
Response: 200 { id, title, status, tags }

GET /api/papers/{id}
Response: 200 { id, title, authors, abstractText, ..., tags, latestDownloadAttempt? }

PUT /api/admin/papers/{id}
Header: Authorization: Bearer {token}
Body: same as create
Response: 200 { id, ... }

DELETE /api/admin/papers/{id}
Header: Authorization: Bearer {token}
Response: 200 { id, status: "DELETED" }

POST /api/admin/papers/{id}/restore
Header: Authorization: Bearer {token}
Response: 200 { id, status: "ACTIVE" }
```

### 下载和样例导入接口

```text
POST /api/papers/{id}/download-attempt
Header: Authorization: Bearer {token}
Response: 200 {
  paperId, status, fileName?, fileSize?, localFilePath?, externalUrl?, failureReason?
}

POST /api/admin/papers/import/arxiv
Header: Authorization: Bearer {token}
Body: { query?: string, maxResults?: number }
Response: 200 { requested, imported, skipped, failed, message }
```

arXiv 导入默认查询建议: `cat:cs.IR OR cat:cs.AI OR cat:cs.LG OR cat:cs.CL`，`maxResults=100`。需要遵守官方 API 限制: legacy API 不超过每 3 秒 1 次请求，单连接访问。

## 配置设计

```yaml
app:
  paper:
    download-dir: ${APP_PAPER_DOWNLOAD_DIR:D:/code/codex_2/lencode/paper-downloads}
    download-timeout-ms: ${APP_PAPER_DOWNLOAD_TIMEOUT_MS:8000}
    arxiv:
      api-url: ${APP_ARXIV_API_URL:https://export.arxiv.org/api/query}
      default-query: ${APP_ARXIV_DEFAULT_QUERY:cat:cs.IR OR cat:cs.AI OR cat:cs.LG OR cat:cs.CL}
      max-results: ${APP_ARXIV_MAX_RESULTS:100}
      timeout-ms: ${APP_ARXIV_TIMEOUT_MS:8000}
```

下载目录在 `javacode` git 仓库之外，避免 PDF、临时文件或个人下载内容进入版本控制。

## 任务拆解

> 每个任务只做一个可验证切片。实施时一次只做一个 T，不跨任务偷跑。

- [x] **T1**: 数据库结构与固定两级标签种子
  - 文件: `sql/002-paper-and-tags.sql`, `.gitignore`
  - 内容: 新建 `research_tags`、`papers`、`paper_tags`、`paper_download_attempts`；初始化 5-8 个一级标签和 15-25 个二级标签；排除下载目录。
  - 验证: MySQL 执行 SQL 成功；能查到两级标签；重复执行不破坏已有数据。

- [x] **T2**: 后端标签查询与管理员标签状态维护
  - 文件: `java-service/src/main/java/com/lencode/paper/tag/**`, `ApiExceptionHandler.java`, 对应测试
  - 内容: `GET /api/tags` 返回启用两级标签树；管理员可新增标签和禁用/启用标签。
  - 验证: 标签树测试通过；普通用户不能调用管理员标签接口；二级标签必须有父标签。

- [x] **T3**: 后端论文提交、详情和分页列表
  - 文件: `java-service/src/main/java/com/lencode/paper/paper/**`, 对应测试
  - 内容: 登录用户提交论文；普通用户查询正常论文列表和详情；支持字段为空和基础分页。
  - 验证: 空字段提交成功；未登录提交返回 401；分页返回 total/page/pageSize；软删除论文默认不可见。

- [x] **T4**: 后端论文标签绑定和组合搜索
  - 文件: `paper/**`, `tag/**`, 对应测试
  - 内容: 论文创建/更新时绑定多个二级标签；按标题、作者、年份、来源、标签、摘要关键词组合搜索。
  - 验证: 一级标签绑定被拒绝；不存在/禁用标签被拒绝；多条件搜索取交集；空搜索返回分页列表。

- [x] **T4.5**: Spring Boot 包结构规范化和 MyBatis-Plus Mapper 改造
  - 文件: `java-service/src/main/java/com/lencode/paper/**`, 对应测试
  - 内容: 将 `auth`、`tag`、`paper` 细分为 `controller/service/mapper/entity/dto/vo`；用 MyBatis-Plus `BaseMapper` 和 Mapper 接口替换旧 `JdbcTemplate Repository`。
  - 验证: `mvn test` 通过；MVC 切片测试不加载 MyBatis Mapper；T2-T4 行为保持不变。

- [x] **T5**: 管理论文编辑、软删除和恢复
  - 文件: `paper/**`, 对应测试
  - 内容: 管理员编辑论文、软删除、查看软删除、恢复；普通用户不能编辑他人论文。
  - 验证: ADMIN 可编辑/删除/恢复；USER 访问管理接口返回 403；软删除后普通详情不可见，恢复后可见。

- [x] **T6**: 论文下载尝试与下载记录
  - 文件: `download/**`, `application.yml`, 对应测试
  - 内容: 按论文 `downloadUrl` 尝试下载 PDF 到仓库外目录；记录成功、超时、非 PDF、失败原因；接口返回外部链接和下载状态。
  - 验证: 使用本地 mock HTTP 或可控测试资源覆盖成功、失败、非 PDF；下载文件不进入 git；无下载链接返回 `NO_URL`。

- [x] **T7**: arXiv 样例论文导入
  - 文件: `importer/**`, `paper/**`, 对应测试
  - 内容: 管理员调用 arXiv 导入 100 条左右元数据；解析 Atom 标题、作者、摘要、发布时间、分类、abs URL、pdf URL、arXiv id；映射到固定标签并去重。
  - 验证: Atom 解析单元测试通过；重复导入跳过已有 `source + source_paper_id`；网络失败返回明确错误，不影响已有论文数据。

- [x] **T8**: 前端论文列表、详情、提交和标签筛选
  - 文件: `frontend/src/App.vue`, `frontend/src/api.js`, `frontend/src/components/**`, `frontend/src/styles.css`
  - 内容: 登录后进入论文工作台；展示标签筛选、搜索表单、分页列表、详情弹窗/页面、普通用户提交论文。
  - 验证: `npm run build` 通过；用户可提交论文并从列表进入详情；空字段展示不崩溃。

- [x] **T9**: 前端管理员维护、导入和下载入口
  - 文件: `frontend/src/**`
  - 内容: 管理员可编辑、软删除、恢复论文；触发 arXiv 导入；论文详情可尝试下载并展示下载状态，始终保留外部链接。
  - 验证: `fjy/123456` 可执行管理操作；普通用户看不到管理操作；下载成功/失败都有提示。

- [x] **T10**: 文档、端到端验收和进度收尾
  - 文件: `README.md`, `docs/progress.md`, `docs/lessons/003-paper-and-tags-e2e.md`, `docs/decisions/002-paper-source-and-download-policy.md`
  - 内容: 写明 SQL 002 初始化、下载目录、arXiv 导入、服务启动和验收命令；记录 Windows/网络/下载踩坑。
  - 验证: Java 测试通过；前端构建通过；本机至少完成 100 条样例导入、一次成功下载、一次失败下载、管理员软删除恢复和普通用户检索。

## 关键技术决策

### 决策 1: 样例论文使用 arXiv API 元数据

**理由**: arXiv 提供公开 API 和 Atom 元数据，适合快速拿到真实论文标题、作者、摘要、发布时间、分类和 PDF 链接。  
**代价**: 没有引用量，且需要遵守 API 速率限制和版权/再分发约束。  
**应对**: `citation_count` 可为空；导入只请求一次 100 条左右；PDF 只做本机下载验证，不在系统里对外分发。

### 决策 2: PDF 下载目录放在 git 仓库之外

**理由**: PDF 是大文件且可能有许可限制，不应进入仓库。  
**代价**: 不同机器需要配置 `APP_PAPER_DOWNLOAD_DIR`。  
**应对**: README 写清楚默认本机目录；`.gitignore` 排除备用本地目录；数据库只记录本机路径和状态。

### 决策 3: 普通用户提交论文后立即公开

**理由**: 第一版先建立数据闭环和推荐基础，不引入审核流。  
**代价**: 数据质量依赖用户输入。  
**应对**: 管理员可编辑、软删除和恢复；审核流未来单独开 spec。

### 决策 4: DOI 非空唯一，arXiv 用 source id 去重

**理由**: DOI 和来源 id 是最稳定的重复判定依据。  
**代价**: 少数异常 DOI 数据可能阻止入库。  
**应对**: DOI 为空允许保存；arXiv 导入优先使用 `source='arXiv' + source_paper_id` 去重。

## 潜在风险

1. **arXiv API 或网络不稳定导致 100 条样例导入失败** - 应对: 导入接口返回明确失败原因；保留本地最小种子 SQL；验收记录说明实际导入数量和失败原因。
2. **真实 PDF 下载涉及许可和大文件问题** - 应对: 只做本机个人/研究用途下载验证；不提交、不对外分发 PDF；页面保留外部链接优先跳转。
3. **前端单文件继续膨胀导致后续难维护** - 应对: T8 开始前拆组件和 API 调用；页面保持工作台式简洁布局，不做复杂状态管理。

## 不在本 plan 范围内

- 用户收藏、评分、浏览、推荐点击和下载点击行为统计。
- embedding 生成、论文向量表和相似度服务。
- 论文全文解析、PDF 文本抽取和全文检索。
- 复杂爬虫平台、反爬、登录态抓取、多站点适配器。
- 审核流、版本历史、多人协作编辑。
- 对外提供 PDF 文件下载服务或 PDF 再分发能力。

## Plan Review 关注点

- T1-T10 是否太多；如果想更快看到页面，可以把 T7 arXiv 导入和 T6 下载放到列表/详情之后做。
- 下载接口是否只记录本机下载状态，不提供系统内 PDF 再分发。
- 普通用户提交后立即公开是否确认；如果要审核流，需要拆出新 spec。
- 前端是否接受先做工作台式页面切换，不引入 Vue Router。

## 修订历史

- 2026-05-26: 按路径 A 收尾补齐 Spec 002 ADR，确认 AGENTS 当前阶段、Plan 重构建议、progress 和叙事证据链均已同步；复核 Java 测试 95 个全通过，前端构建通过。
- 2026-05-26: T10 完成，补充 README 初始化和验收命令、T10 lessons；Java 测试 95 个全通过，前端构建通过；本机完成 arXiv 导入 100 条、真实 PDF 下载成功 1 次、失败下载记录 1 次、管理员软删除恢复和普通用户检索。
- 2026-05-25: T9 完成，前端新增管理员论文编辑、软删除、回收站恢复、arXiv 导入和详情页下载尝试入口；`npm run build` 通过，浏览器冒烟覆盖管理员入口和下载状态展示。
- 2026-05-25: T8 完成，前端论文工作台接入列表、搜索、标签筛选、分页、详情和普通用户提交；`npm run build` 通过。
- 2026-05-25: T4.5 完成，规范化 Spring Boot 包结构，新增 MyBatis-Plus Mapper 配置，并将旧 JdbcTemplate Repository 改为 Mapper 接口。
- 2026-05-25: T7 完成，新增 arXiv Atom 元数据导入、分类映射二级标签、`source + source_paper_id` 去重和管理员导入接口；Maven 测试通过，`Tests run: 95, Failures: 0, Errors: 0, Skipped: 4`。
- 2026-05-25: T6 完成，新增论文下载尝试接口、下载状态记录、PDF 保存到仓库外目录、超时/非 PDF/无链接/失败降级；Maven 测试通过，`Tests run: 86, Failures: 0, Errors: 0, Skipped: 4`。
- 2026-05-25: T4 完成，新增论文标签绑定、返回标签和标题/作者/年份/来源/标签/摘要关键词组合搜索。
- 2026-05-25: T5 完成，新增管理员论文编辑、软删除、软删除列表和恢复接口；Maven 测试通过，`Tests run: 75, Failures: 0, Errors: 0, Skipped: 4`。
- 2026-05-25: T3 完成，新增论文提交、正常论文分页列表、详情接口和对应测试。
- 2026-05-25: T2 完成，新增标签树查询、管理员标签新增/启停接口和对应测试。
- 2026-05-25: T1 完成，新增 SQL 002 和下载目录忽略规则，并通过 MySQL 幂等执行验证。
- 2026-05-25: 初稿，根据 Spec 002 已通过约束、现有代码结构和 arXiv 官方 API/使用条款整理。
