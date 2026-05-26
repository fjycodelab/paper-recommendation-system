# AGENTS.md

## 项目说明

本项目是论文推荐与科研行为分析系统，服务于科研文献管理场景。
核心目标是沉淀论文元数据、用户收藏评分行为、研究方向标签，并通过 Java 业务服务调用 Python 推理服务生成推荐结果。

Codex 会自动读取本文件作为本目录的项目规则。原 `CLAUDE.md` 已迁移为 `AGENTS.md`。

## 技术栈

- Java: Spring Boot, Spring Security, MyBatis-Plus, Spring WebClient
- 数据库: MySQL
- 缓存: Redis
- 算法服务: Python, FastAPI, PyTorch
- 测试: Java 单元测试优先，Python 推理服务保留可替换测试入口

## Java 后端约定

- 领域包按 `controller/service/mapper/entity/dto/vo` 分层，不把控制器、服务、实体和返回对象混放在同一层目录。
- 登录、当前用户和管理员权限属于 Spec 001 范围，统一使用 Spring Security；不要新增 Controller 级手写 token 鉴权。
- 持久层优先使用 MyBatis-Plus Mapper；业务表不要新增 `JdbcTemplate Repository`。
- MyBatis-Plus 实体需要无参构造、setter、`@TableName`、`@TableId` 和必要的 `@TableField`。
- `@MapperScan` 放在独立配置类中，不直接放启动类，避免 `@WebMvcTest` 切片测试误加载 Mapper。
- 包结构或持久层改造后，必须运行 `mvn test`，并检查是否残留旧 Repository 名称。

## 目录约定

- `docs/`: 需求、方案、经验和架构决策
- `docs/specs/`: 单个功能的 spec
- `docs/plans/`: spec 对应的技术方案和任务拆解
- `docs/lessons/`: 踩坑记录和复盘
- `docs/decisions/`: ADR 架构决策记录
- `docs/narratives/`: 项目叙事点，记录可用于简历/面试的背景、问题、方案、技术和结果
- `java-service/`: Spring Boot 业务服务，用 IDEA 打开和运行。
- `python-infer/`: FastAPI/PyTorch 推理服务，用 PyCharm 打开，直接运行 `app/main.py`。
- `frontend/`: Vue/Vite + Element Plus 前端，用 VSCode 打开和运行。
- `sql/`: 数据库初始化、迁移或样例数据。

## 本机开发工具约定

- Java: 使用 IntelliJ IDEA 打开 `java-service/`，运行 `PaperJavaServiceApplication`。
- Java SDK: 使用 Java 8。
- Maven 本地仓库: `D:\study software\maven-repository`。
- Python: 使用 PyCharm 打开 `python-infer/`，解释器固定为 `D:\Anaconda\Anaconda\envs\cd-agent\python.exe`。
- Python 启动: PyCharm 直接运行 `python-infer/app/main.py`，工作目录设为 `python-infer/`。
- Frontend: 使用 VSCode 打开 `frontend/`。
- Node 环境变量: 用户级 `Path` 包含 `C:\Program Files\nodejs` 和 `C:\Users\fjy\AppData\Roaming\npm`，用户级 `NODE_HOME=C:\Program Files\nodejs`。
- Frontend 启动: 重开 VSCode 或新终端后，在 `frontend/` 直接使用 `npm install`、`npm run dev`、`npm run build`。
- Redis: 本机目录 `D:\study software\redis`，数据目录 `D:\study software\redis\data`。

## 强制规则

- IMPORTANT: 修改既有代码前，先用 `rg` 搜索它被谁引用，并报告影响范围。
- 提交或收尾前必须运行相关测试；无法运行时要说明原因。
- 禁止 `try except pass` 或吞异常；异常必须有明确处理或向上抛出。
- 新增依赖必须先说明理由并征求确认。
- 优先组合，不优先继承；除非框架约定要求继承。
- 函数超过 50 行要警觉，超过 100 行必须拆分或说明理由。
- 禁止提交 `.env`、密钥、真实账号、真实 token 和本地私有配置。
- 禁止批量删除文件或目录；需要删除时一次只删一个明确路径的文件。
- 不使用 `Remove-Item -Recurse`、`rm -rf`、`rd /s`、`rmdir /s`、`del /s`。
- 注释只解释“为什么”，不要复述代码“做了什么”。

## 工作流程

1. 新功能先写 `docs/specs/00X-xxx.md`，明确目标、边界和验收标准。
2. spec 通过后写 `docs/plans/00X-xxx-plan.md`，任务必须是可测试的垂直切片。
3. 一次只实现一个任务，不跨 spec 偷跑。
4. 遇到 bug 先写复现测试，确认失败后再修。
5. 每完成一个任务，更新 `docs/progress.md`。
6. 一个 spec 完成后，沉淀 lessons 或 ADR，再移动到已完成。
7. 每完成一个能讲清楚价值的 spec/task，更新 `docs/narratives/narrative-log.md`。

## 项目叙事规则

- 叙事必须遵循“业务场景/痛点 -> 遇到的问题 -> 技术方案 -> 落地功能 -> 验证结果/提升 -> 简历表达”的结构。
- 只写真实发生或已经实现的内容；计划中的能力必须标记为“计划”或“待验证”。
- 不允许编造性能提升数字。所有“提升 xx%”“减少 xx ms”“通过 xx 测试”必须能追溯到测试、日志、README、progress 或代码。
- 技术点要绑定具体功能，例如 Redis 绑定 token 鉴权/推荐缓存，MyBatis-Plus 绑定论文检索，FastAPI/PyTorch 绑定 embedding 相似度服务。
- 每条叙事都要写“面试追问点”，提前记录为什么这么设计、有什么取舍、哪里还能优化。
- 简历表达要结果导向，避免只堆技术名词。
- 当某个问题形成可复用经验时，同时写入 `docs/lessons/`；当某个方案是长期架构选择时，同时写入 `docs/decisions/`。

## 当前阶段

Spec 001 登录认证与项目基础骨架已完成。Spec 002 论文信息与研究方向标签管理已完成并完成 T10 收尾验收。下一步优先推进 Spec 003: 用户收藏评分与行为记录。
