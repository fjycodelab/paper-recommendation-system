# Plan 003: 用户收藏评分与行为记录 - 技术方案

> 创建日期: 2026-05-26
> 状态: 已通过
> 对应 spec: [docs/specs/003-user-behavior.md](../specs/003-user-behavior.md)

## 概述

本 plan 在 Spec 002 的论文工作台之上新增用户收藏、评分、最近浏览、隐式行为记录和管理员全局行为统计。Java 服务继续作为唯一 API 入口；MySQL 保存用户可见的当前状态和最终行为明细；Kafka 承载高频隐式行为消息，完成行为上报与 MySQL 持久化的异步解耦；Redis 用于最近浏览缓存、管理员统计缓存和缓存重建锁。

实现策略是先落 MySQL 表和后端收藏/评分强状态，再接 Kafka 生产者/消费者和 `event_id` 幂等落库，随后用 Redis 支撑最近浏览与统计缓存，最后扩展前端入口和项目叙事文档。用户已明确要求消息队列使用 Kafka，因此本 spec 会新增 `spring-kafka` 依赖；不引入 RabbitMQ/RocketMQ。

## 涉及的既有代码评估

### 当前状态

- `java-service/src/main/java/com/lencode/paper/auth/`: 已通过 Spring Security + Redis token 建立当前用户上下文，业务接口可复用 `AuthService.currentUser()`。
- `java-service/src/main/java/com/lencode/paper/paper/`: 已提供论文列表、详情、提交、编辑、软删除和恢复；`PaperResponse` 暂无当前用户收藏/评分字段。
- `java-service/src/main/java/com/lencode/paper/download/`: 已有 `POST /api/papers/{id}/download-attempt` 和 `PaperDownloadService`，可在下载尝试前补 `DOWNLOAD_CLICK` 行为消息。
- `java-service/src/main/java/com/lencode/paper/health/` 与 `auth/AuthTokenService`: 已使用 `StringRedisTemplate`，Redis 连接配置可复用到最近浏览和统计缓存。
- `java-service/src/main/resources/application.yml`: 已有 Redis 和 app 配置风格，需要新增 Kafka bootstrap、topic、groupId、统计缓存 TTL 和锁 TTL 配置。
- `frontend/src/components/PaperWorkbench.vue`: 当前单组件承载论文列表、详情、下载、导入和回收站；Spec 003 会继续变大，实施时需要先做局部拆分或至少分区整理。
- `frontend/src/api.js`: 已封装 token、请求错误和论文 API，可扩展收藏、评分、行为上报、我的收藏、最近浏览和统计 API。
- `sql/002-paper-and-tags.sql`: 已创建论文、标签和下载尝试表；Spec 003 应新增 `sql/003-user-behavior.sql`，不改写 002 的历史语义。

### 影响范围

基于 `rg` 搜索，当前会受影响的主要入口:

- `PaperController.list/get`: 需要注入当前用户信息，用于返回收藏/评分状态，并在搜索、筛选、打开详情时发送行为消息。
- `PaperService.list/get`: 需要从无用户上下文改为用户感知响应；相关 controller 和 service 测试要同步更新。
- `DownloadController` 或 `PaperDownloadService.attempt`: 需要发送 `DOWNLOAD_CLICK` 行为消息，同时保留 Spec 002 下载结果记录。
- `PaperWorkbench.vue`: 需要增加收藏按钮、评分、我的收藏、最近浏览、外部 URL 点击记录和管理员统计面板。
- `SecurityConfig`: 当前 `GET /api/papers`、`GET /api/papers/*` 允许匿名访问；Spec 003 不记录匿名行为，计划改为登录后访问论文工作台，测试同步调整。

### 重构建议

- [ ] **改动前重构**: 前端先把 `PaperWorkbench.vue` 的 API 调用和视图状态按“列表/详情/管理弹窗/用户行为”分区整理；如拆文件成本过高，至少保持新增行为视图独立函数组和样式块。
- [ ] **改动中重构**: 新建 `behavior` 领域包，按 `controller/service/mapper/entity/dto/vo` 分层，不把收藏评分逻辑塞进 `paper` 或 `download` 包。
- [ ] **改动后重构**: Spec 003 完成后沉淀 `docs/lessons/004-user-behavior-kafka-redis.md` 和叙事台账，说明 Kafka、Redis、锁、幂等 event_id 和 MySQL 约束分别解决的问题。

### 不重构的部分及理由

- `AuthService` 和 token 结构: 只复用当前用户能力，不改认证策略，避免回归 Spec 001。
- `PaperDownloadClient`: 下载 HTTP 行为保持不变，Spec 003 只在业务层记录下载点击事件。
- `python-infer/`: 本 spec 不涉及 embedding 或推荐，不动 Python 服务。
- 不引入 RabbitMQ/RocketMQ: 用户已指定 Kafka，避免同时维护多套 MQ 概念。

## 涉及的文件

### 新增

- `sql/003-user-behavior.sql` - 收藏、评分、行为事件表和索引。
- `java-service/src/main/java/com/lencode/paper/behavior/entity/PaperFavorite.java`
- `java-service/src/main/java/com/lencode/paper/behavior/entity/PaperRating.java`
- `java-service/src/main/java/com/lencode/paper/behavior/entity/PaperBehaviorEvent.java`
- `java-service/src/main/java/com/lencode/paper/behavior/dto/RatePaperRequest.java`
- `java-service/src/main/java/com/lencode/paper/behavior/dto/BehaviorEventRequest.java`
- `java-service/src/main/java/com/lencode/paper/behavior/dto/BehaviorEventMessage.java`
- `java-service/src/main/java/com/lencode/paper/behavior/vo/BehaviorAcceptedResponse.java`
- `java-service/src/main/java/com/lencode/paper/behavior/vo/BehaviorStatsResponse.java`
- `java-service/src/main/java/com/lencode/paper/behavior/vo/UserPaperStateResponse.java`
- `java-service/src/main/java/com/lencode/paper/behavior/mapper/PaperFavoriteMapper.java`
- `java-service/src/main/java/com/lencode/paper/behavior/mapper/PaperRatingMapper.java`
- `java-service/src/main/java/com/lencode/paper/behavior/mapper/PaperBehaviorEventMapper.java`
- `java-service/src/main/java/com/lencode/paper/behavior/service/PaperPreferenceService.java` - 收藏/评分强状态。
- `java-service/src/main/java/com/lencode/paper/behavior/service/BehaviorEventProducer.java` - Kafka 行为消息发送。
- `java-service/src/main/java/com/lencode/paper/behavior/service/BehaviorEventConsumer.java` - Kafka 行为消息消费和幂等落库。
- `java-service/src/main/java/com/lencode/paper/behavior/service/RecentViewService.java` - Redis 最近浏览 ZSet。
- `java-service/src/main/java/com/lencode/paper/behavior/service/BehaviorStatsService.java` - 管理员统计和 Redis 缓存。
- `java-service/src/main/java/com/lencode/paper/behavior/service/RedisLockService.java` - 统计缓存重建锁。
- `java-service/src/main/java/com/lencode/paper/behavior/controller/PaperPreferenceController.java`
- `java-service/src/main/java/com/lencode/paper/behavior/controller/BehaviorEventController.java`
- `java-service/src/main/java/com/lencode/paper/behavior/controller/BehaviorStatsController.java`
- `java-service/src/test/java/com/lencode/paper/behavior/**` - 收藏、评分、Kafka 生产消费、幂等、Redis 缓存、锁和统计测试。

### 修改

- `java-service/pom.xml` - 新增 `org.springframework.kafka:spring-kafka`。
- `docs/specs/003-user-behavior.md` - 补充对应 plan 链接，必要时同步已决策内容。
- `docs/progress.md` - 记录 Plan 003 状态和任务拆解。
- `java-service/src/main/resources/application.yml` - 新增 Kafka、行为 topic、Redis 缓存和锁 TTL 配置。
- `java-service/src/main/java/com/lencode/paper/config/SecurityConfig.java` - 论文列表/详情改为登录用户访问，避免匿名行为。
- `java-service/src/main/java/com/lencode/paper/paper/controller/PaperController.java` - 列表/详情接入当前用户和隐式行为消息。
- `java-service/src/main/java/com/lencode/paper/paper/service/PaperService.java` - 返回当前用户收藏/评分状态；必要时新增用户感知方法。
- `java-service/src/main/java/com/lencode/paper/paper/vo/PaperResponse.java` - 增加 `favorited`、`rating` 字段。
- `java-service/src/main/java/com/lencode/paper/download/service/PaperDownloadService.java` - 下载入口点击发送 `DOWNLOAD_CLICK`。
- `frontend/src/api.js` - 新增收藏、评分、行为上报、我的收藏、最近浏览和统计 API。
- `frontend/src/components/PaperWorkbench.vue` - 新增用户行为入口和管理员统计面板。
- `frontend/src/styles.css` - 新增收藏、评分、最近浏览和统计样式。
- `README.md` - 补充 SQL 003、Kafka、Redis 缓存和验收命令。
- `docs/narratives/narrative-log.md` - 新增 Spec 003 项目叙事。

## 数据模型

### `paper_favorites`

保存用户对论文的当前收藏状态。取消收藏不删除行，更新状态为 `CANCELLED`，便于后续追踪更新时间。

```sql
CREATE TABLE paper_favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_paper_favorites_user_paper (user_id, paper_id),
  KEY idx_paper_favorites_user_status (user_id, status),
  KEY idx_paper_favorites_paper_status (paper_id, status),
  CONSTRAINT fk_paper_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_paper_favorites_paper FOREIGN KEY (paper_id) REFERENCES papers(id)
);
```

### `paper_ratings`

保存用户对论文的当前评分。第一版不做删除评分，重复评分更新当前值。

```sql
CREATE TABLE paper_ratings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_paper_ratings_user_paper (user_id, paper_id),
  KEY idx_paper_ratings_paper (paper_id),
  CONSTRAINT fk_paper_ratings_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_paper_ratings_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT chk_paper_ratings_rating CHECK (rating BETWEEN 1 AND 5)
);
```

### `paper_behavior_events`

保存最终落库的行为明细。`event_id` 由 Java 生成，Kafka 重试或重复投递时通过唯一键避免重复落库。

```sql
CREATE TABLE paper_behavior_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  paper_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  keyword VARCHAR(255) NULL,
  author VARCHAR(255) NULL,
  publish_year INT NULL,
  tag_id BIGINT NULL,
  metadata VARCHAR(1000) NULL,
  occurred_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_behavior_events_event_id (event_id),
  KEY idx_behavior_events_user_time (user_id, occurred_at),
  KEY idx_behavior_events_paper_type (paper_id, event_type),
  KEY idx_behavior_events_type_time (event_type, occurred_at),
  CONSTRAINT fk_behavior_events_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_behavior_events_paper FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_behavior_events_tag FOREIGN KEY (tag_id) REFERENCES research_tags(id)
);
```

## Kafka 与 Redis 设计

### Kafka

- Topic: `paper.behavior.events`
- Consumer group: `paper-behavior-writer`
- Message key: `userId` 或 `paperId` 字符串，第一版优先 `userId`，让同一用户行为尽量有序。
- Message value: JSON 序列化的 `BehaviorEventMessage`。
- Producer: `BehaviorEventProducer` 使用 `KafkaTemplate<String, String>`。
- Consumer: `BehaviorEventConsumer` 使用 `@KafkaListener`，消费后写入 `paper_behavior_events`。
- 幂等: 消息内含 `eventId`，MySQL `uk_behavior_events_event_id` 避免重复投递导致重复落库。

### Redis

- `behavior:recent:{userId}`: 最近浏览 ZSet，score 为时间戳，member 为 `paperId`。
- `behavior:stats:global`: 管理员全局统计缓存，JSON 字符串，TTL 可配置。
- `behavior:stats:rebuild:lock`: 统计缓存重建锁，使用 `SET NX PX` 和 Lua compare-and-delete。

### 行为流转

1. 用户打开详情、搜索、筛选、点击外部 URL 或点击下载入口。
2. Java 服务生成 `eventId` 和 `BehaviorEventMessage`。
3. 对详情浏览，先写 Redis 最近浏览 ZSet，保证“最近浏览”快速可见。
4. Java 服务发送 Kafka 消息；发送失败只影响行为记录，不让论文主流程崩溃，但必须记录可观测错误。
5. Kafka consumer 消费消息并写 MySQL。
6. 如果 Kafka 重试或重复投递，MySQL 唯一键让重复 `eventId` 视为已处理。
7. 管理员统计优先读 Redis 缓存；缓存未命中时用 Redis 锁保护重聚合，避免多个请求同时压 MySQL。

## 配置

```yaml
spring:
  kafka:
    bootstrap-servers: ${APP_KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}
    consumer:
      group-id: ${APP_KAFKA_BEHAVIOR_GROUP_ID:paper-behavior-writer}
      auto-offset-reset: earliest
    producer:
      acks: all

app:
  behavior:
    topic: ${APP_BEHAVIOR_TOPIC:paper.behavior.events}
    stats-cache-key: ${APP_BEHAVIOR_STATS_CACHE_KEY:behavior:stats:global}
    stats-cache-ttl-seconds: ${APP_BEHAVIOR_STATS_CACHE_TTL_SECONDS:60}
    stats-lock-key: ${APP_BEHAVIOR_STATS_LOCK_KEY:behavior:stats:rebuild:lock}
    stats-lock-ttl-ms: ${APP_BEHAVIOR_STATS_LOCK_TTL_MS:10000}
    recent-view-limit: ${APP_BEHAVIOR_RECENT_VIEW_LIMIT:50}
```

## API 设计

### 收藏和评分

- `POST /api/papers/{id}/favorite`
  - 登录用户收藏论文。
  - 返回当前用户对该论文的状态。
- `DELETE /api/papers/{id}/favorite`
  - 登录用户取消收藏论文。
  - 返回当前状态。
- `PUT /api/papers/{id}/rating`
  - 请求体: `{ "rating": 1 }` 到 `{ "rating": 5 }`。
  - 登录用户新增或更新评分。
- `GET /api/me/favorites?page=1&pageSize=10`
  - 返回当前用户收藏的正常状态论文。
- `GET /api/me/recent-views?page=1&pageSize=10`
  - 优先基于 Redis 最近浏览 ZSet 查询，再按论文 id 批量加载正常状态论文。

### 行为上报

- `POST /api/behavior-events`
  - 给前端上报无法完全由后端自动捕捉的事件，例如 `EXTERNAL_URL_CLICK`。
  - 请求体字段: `eventType`、`paperId`、`tagId`、`keyword`、`author`、`publishYear`。
  - 后端只允许 spec 内列出的事件类型。

后端自动记录:

- `GET /api/papers` 有 `title/author/year` 时发送 `PAPER_SEARCH`。
- `GET /api/papers` 有 `tagId` 时发送 `TAG_FILTER`。
- `GET /api/papers/{id}` 发送 `PAPER_DETAIL_VIEW`，并写 Redis 最近浏览。
- `POST /api/papers/{id}/download-attempt` 发送 `DOWNLOAD_CLICK`。

### 管理员统计

- `GET /api/admin/behavior/stats`
  - 返回全局事件总数、各事件类型数量、收藏总数、评分人数、平均评分、详情浏览较多论文、下载入口点击较多论文。
  - 不返回用户级明细。
  - 优先读 Redis 缓存；缓存未命中时加锁重建。

## 关键决策

1. **Kafka 作为行为消息队列**  
   用户已指定 Kafka。Kafka 更适合讲清楚高频行为削峰、异步解耦、可重试消费和后续多消费者扩展。

2. **Redis 不做 MQ，改做最近浏览和统计缓存**  
   Redis 的价值放在低延迟读和缓存上：最近浏览打开后立即可见，管理员统计避免每次实时聚合。

3. **收藏/评分直接写 MySQL，隐式行为走 Kafka**  
   收藏和评分是用户立即可见的强状态；浏览、搜索、筛选和点击属于行为明细，可接受短暂延迟落库。

4. **Kafka 至少一次投递 + `event_id` 幂等落库**  
   第一版不追求精确一次；用 `event_id` 唯一键处理重复投递和重试。

5. **Redis 锁保护统计缓存重建**  
   锁不用于 Kafka 消费，因为 consumer group 已负责消费分配；锁用于缓存未命中时避免多个管理员请求同时重聚合。

6. **论文列表/详情改为登录后访问**  
   Spec 003 明确不记录匿名行为，且前端入口本来位于登录后 dashboard。为返回用户收藏/评分状态和行为记录，论文列表/详情接口将依赖当前用户。

## 任务拆解

### T1: Kafka 依赖、配置与数据库结构

- 新增 `spring-kafka` 依赖。
- 新增 Kafka topic/group 相关配置。
- 新增 `sql/003-user-behavior.sql`。
- 新增收藏、评分、行为事件实体和 Mapper。
- 测试重点: SQL review，后续服务测试验证 Mapper 调用。

### T2: 收藏和评分后端强状态 API

- 新增 `PaperPreferenceService` 和 `PaperPreferenceController`。
- 实现收藏、取消收藏、评分新增/更新。
- 拒绝不存在或软删除论文。
- 通过唯一键或 Mapper upsert 保证同一用户同一论文只有一条当前状态。
- 测试: service 覆盖重复收藏、重复取消、非法评分、论文不存在；controller 覆盖鉴权和请求响应。

### T3: 用户感知论文列表、详情、我的收藏

- `PaperResponse` 增加 `favorited`、`rating`。
- `PaperController.list/get` 接入当前用户。
- `PaperService` 或行为服务为列表/详情补当前用户收藏和评分。
- 新增 `GET /api/me/favorites`。
- 测试: 列表/详情返回用户状态；未登录访问论文列表/详情返回 401；我的收藏只返回正常论文。

### T4: Kafka 行为生产者、消费者和幂等落库

- 新增 `BehaviorEventProducer`，发送 `BehaviorEventMessage` 到 Kafka。
- 新增 `BehaviorEventConsumer`，用 `@KafkaListener` 消费并写 `paper_behavior_events`。
- `paper_behavior_events.event_id` 唯一，重复消息视为已处理。
- 行为发送失败记录可观测错误，不让论文主流程崩溃。
- 测试: producer 参数、consumer 成功落库、重复 eventId 幂等、非法消息不吞异常。

### T5: 隐式行为接入业务入口和最近浏览 Redis

- `GET /api/papers` 在存在标题关键词、作者或年份时发送 `PAPER_SEARCH`。
- `GET /api/papers` 在存在 `tagId` 时发送 `TAG_FILTER`。
- `GET /api/papers/{id}` 成功返回后发送 `PAPER_DETAIL_VIEW`，并写 Redis 最近浏览 ZSet。
- `POST /api/papers/{id}/download-attempt` 成功校验论文后发送 `DOWNLOAD_CLICK`，下载失败不影响点击行为。
- `POST /api/behavior-events` 支持前端记录 `EXTERNAL_URL_CLICK`。
- `GET /api/me/recent-views` 读取 Redis 最近浏览。
- 测试: 各入口触发正确消息；论文不存在时不记录有效事件；Redis 不可用时降级路径明确。

### T6: 管理员全局行为统计、Redis 缓存和缓存重建锁

- 新增 `BehaviorStatsService` 和 `BehaviorStatsController`。
- 统计事件总数、事件类型分布、收藏总数、评分人数、平均评分、详情浏览 Top 论文、下载点击 Top 论文。
- 统计结果写 Redis 短 TTL 缓存。
- 缓存未命中时通过 `RedisLockService` 获取重建锁，避免缓存击穿。
- 测试: controller 管理员鉴权；service 覆盖缓存命中、缓存未命中、锁占用、空数据返回。

### T7: 前端收藏、评分、我的收藏和最近浏览

- `api.js` 新增收藏、取消收藏、评分、我的收藏、最近浏览 API。
- 论文列表增加收藏按钮，避免点击收藏触发行点击详情。
- 详情抽屉增加 1-5 星评分。
- 新增“我的收藏”和“最近浏览”入口，可放在工作台 tab 或顶部按钮。
- 前端处理 401、非法评分和收藏状态刷新。
- 验证: `npm run build`。

### T8: 前端行为上报和管理员统计面板

- 外部 URL 和下载链接改为先记录点击事件，再打开链接或调用下载尝试。
- 搜索、筛选由后端自动记录，前端不重复上报。
- 管理员增加行为统计面板，展示全局指标卡和 Top 列表。
- 验证: 管理员账号能看到统计；普通用户不渲染统计入口；`npm run build`。

### T9: 文档、叙事和端到端验收

- README 补充 SQL 003、Kafka、Redis 最近浏览/统计缓存和验收命令。
- `docs/narratives/narrative-log.md` 新增 Spec 003 叙事，明确 Kafka、Redis、锁、MySQL 约束分别解决的问题。
- 如遇到 Kafka、Redis 缓存、锁或 Windows 启动坑，新增 `docs/lessons/004-user-behavior-kafka-redis.md`。
- 运行 Java 测试和前端构建。
- 手动或 API 级验收收藏、评分、详情浏览、搜索、筛选、外部 URL、下载点击、Kafka 消费落库、最近浏览和管理员统计。

## 测试策略

- Java service 单元测试优先，覆盖收藏/评分、Kafka 生产消费、幂等落库、Redis 缓存、锁和统计聚合。
- Controller 切片测试继续使用 `@WebMvcTest` + `@MockBean`，避免加载真实 MySQL/Redis/Kafka。
- Kafka 相关服务通过 mock `KafkaTemplate` 和直接调用 consumer 方法测试；真实 Kafka 联调放 T9 验收。
- Redis 相关服务优先通过 mock `StringRedisTemplate` 或抽象小接口测试；真实 Redis 联调放 T9 验收。
- 前端至少运行 `npm run build`；交互复杂时用浏览器冒烟验证收藏、评分、详情、统计面板。
- 包结构或 Mapper 调整后必须运行 `mvn test`，并用 `rg "\b(ResearchTagRepository|PaperRepository|PaperTagRepository|UserRepository)\b" java-service/src` 检查旧 Repository 名称没有回归。

## 风险与应对

1. **Kafka 本机启动和依赖配置增加复杂度**  
   应对: README 写清本机 Kafka 启动、topic 创建和环境变量；Java 测试不依赖真实 Kafka，真实联调放 T9。

2. **Kafka 重试导致行为重复落库**  
   应对: 所有消息带 `event_id`，MySQL 唯一键保证重复消息幂等。

3. **Redis 缓存不可用影响最近浏览和统计**  
   应对: 收藏/评分和行为最终落库不依赖 Redis；最近浏览可降级到 MySQL 查询或返回明确错误；统计缓存失败时可直接聚合但记录日志。

4. **前端工作台继续膨胀导致维护困难**  
   应对: T7/T8 控制改动范围，新增用户行为状态和管理员统计分区清晰；必要时先拆出小组件，但不做无关大重构。

5. **论文列表/详情从匿名变为登录访问影响旧测试**  
   应对: 这是 Spec 003 的明确边界，更新 controller 测试和 README；前端入口本来在登录后 dashboard，不影响主用户流程。

## 验收方式

1. 执行 `sql/001-auth-and-foundation.sql`、`sql/002-paper-and-tags.sql`、`sql/003-user-behavior.sql`。
2. 启动 MySQL、Redis、Kafka 和 Java 服务。
3. Java 测试通过: `mvn test`。
4. 前端构建通过: `npm run build`。
5. 登录普通用户，收藏论文，刷新列表和详情后状态仍为已收藏。
6. 取消收藏后，“我的收藏”不再展示该论文。
7. 设置 1-5 星评分，修改评分后列表和详情显示新评分。
8. 连续打开同一论文详情多次，Kafka 消费后 `paper_behavior_events` 中出现多条 `PAPER_DETAIL_VIEW`。
9. 搜索标题关键词、作者或年份，Kafka 消费后出现 `PAPER_SEARCH`，并保存关键词、作者、年份。
10. 标签筛选后出现 `TAG_FILTER`。
11. 点击外部 URL 后出现 `EXTERNAL_URL_CLICK`。
12. 点击下载入口后出现 `DOWNLOAD_CLICK`，同时 Spec 002 下载尝试记录仍保存成功或失败状态。
13. 最近浏览入口能较快展示刚打开过的论文。
14. 管理员打开行为统计面板，能看到事件总数、类型分布、收藏总数、评分人数、平均评分和 Top 论文。

## Review 结论

- 2026-05-26: 用户确认 plan 通过，开始按任务实施。
- 本机 Kafka 第一版按默认 `127.0.0.1:9092`、topic `paper.behavior.events`、groupId `paper-behavior-writer` 推进；如本机实际路径不同，在 T9 README/lesson 中记录。
- 接受论文列表和详情改为登录后访问。
- 接受最近浏览第一版优先读 Redis ZSet，MySQL 作为后续兜底优化。
- 接受管理员统计第一版使用 Redis 短 TTL 缓存，后续再做聚合表或定时统计任务。

## 修订历史

- 2026-05-26: 初稿，按用户要求将行为消息队列改为 Kafka。
- 2026-05-26: Plan review 通过，进入 T1。
