# ADR 003: GitHub 仓库命名与同步策略

> 日期: 2026-05-26
> 状态: 已接受

## 背景

项目最初需要提交到用户提供的 GitHub 仓库 `fjycodelab/java`。该名称过于泛化，不能表达论文推荐、科研行为分析、Java/Python 分层服务和 Kafka/Redis 行为链路这些项目主题。

项目采用 Vibe Coding V3 路径 A，已经形成 spec、plan、task、test、progress、lesson/ADR 的节奏。线上仓库也需要跟随这个节奏同步，避免本地提交长期滞后于 GitHub。

## 决策

1. GitHub 仓库名改为 `paper-recommendation-system`。
2. 线上仓库地址使用 `https://github.com/fjycodelab/paper-recommendation-system`。
3. 本地默认分支使用 `main`，并跟踪 `origin/main`。
4. 每完成一个可验证 task，或完成一小段连续本地提交后，同步推送到线上仓库。
5. 远程同步只推送代码、SQL、文档和可复现配置，不推送 `.env`、密钥、真实 token、下载 PDF 和本机私有数据。

## 理由

- `paper-recommendation-system` 能直接表达项目业务主题，比 `java` 更适合展示和检索。
- 本项目有明确的阶段性 task 边界，按 task 同步可以让 GitHub 历史和 `docs/progress.md` 对齐。
- 保留本地先验证再推送的节奏，可以减少线上仓库出现半成品或无法解释的提交。

## 影响

- README 需要展示新的仓库地址和同步约定。
- 后续每次阶段性提交后，默认执行 `git push` 同步到 GitHub。
- 如后续新增 release、部署或简历链接，也以新仓库名为准。
