# CLAUDE.md

## 项目说明

本项目是论文推荐与科研行为分析系统，服务于科研文献管理场景。
核心目标是沉淀论文元数据、用户收藏评分行为、研究方向标签，并通过 Java 业务服务调用 Python 推理服务生成推荐结果。

## 技术栈

- Java: Spring Boot, MyBatis-Plus, Spring WebClient
- 数据库: MySQL
- 缓存: Redis
- 算法服务: Python, FastAPI, PyTorch
- 测试: Java 单元测试优先，Python 推理服务保留可替换测试入口

## 目录约定

- `docs/`: 需求、方案、经验和架构决策
- `docs/specs/`: 单个功能的 spec
- `docs/plans/`: spec 对应的技术方案和任务拆解
- `docs/lessons/`: 踩坑记录和复盘
- `docs/decisions/`: ADR 架构决策记录
- `java-service/`: Spring Boot 业务服务，后续创建
- `python-infer/`: FastAPI/PyTorch 推理服务，后续创建
- `sql/`: 数据库初始化、迁移或样例数据，后续创建

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

## 当前阶段

MVP 起步。先完成项目定义、架构访谈和第一个 spec，再开始写业务代码。
