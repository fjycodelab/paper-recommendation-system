# 论文推荐与科研行为分析系统

面向科研文献管理场景的后端与算法服务项目。系统围绕论文信息管理、用户收藏评分、研究方向标签、论文语义相似度和个性化推荐结果生成展开。

当前处于 MVP 起步阶段。代码统一放在本目录下，先按 Vibe Coding V3 的路径 A 推进：先项目定义、再架构访谈、再逐个 spec 实现。

## 技术栈

- Java: Spring Boot, MyBatis-Plus, Spring WebClient
- 数据存储: MySQL, Redis
- 算法服务: Python, FastAPI, PyTorch
- 前端展示: 暂定 Vue/Vite + 成熟组件库，保持简洁展示
- 协作方式: spec -> plan -> task -> test -> review -> lesson/ADR

## MVP 范围

1. 论文信息、摘要、关键词、研究方向标签的管理能力。
2. 用户收藏、评分、访问等科研行为记录能力。
3. 基于摘要、关键词、研究方向生成语义向量并计算相似论文。
4. Java 服务调用 Python 推理服务，生成个性化推荐结果。
5. 基于 Redis 缓存热门论文、用户偏好标签和推荐结果。
6. 提供简洁前端页面展示登录、论文列表、热门论文、偏好标签和推荐结果。

## 当前状态

起步配置中。先完成 `docs/project-brief.md`、`CLAUDE.md` 和 `docs/progress.md`，再进入架构访谈。
