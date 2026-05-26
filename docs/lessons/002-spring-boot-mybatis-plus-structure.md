# Lesson 002: Spring Boot 包结构与 MyBatis-Plus Mapper 改造
> 日期: 2026-05-25
> 对应: Spec 002 / T4.5

## 问题

- 计划和 `AGENTS.md` 明确写的是 Spring Boot + MyBatis-Plus，但 T2-T4 初版后端使用了 `JdbcTemplate + Repository`，没有真正落到 Mapper 层。
- Java 包结构一开始偏扁平，`auth`、`tag`、`paper` 内混放 controller、service、dto、vo、entity、repository，IDEA 打开后不够像标准 Spring Boot 工程。
- 把文件移动到新目录后，只改物理路径不够，必须同步修 `package`、`import`、测试 mock 和 Spring 扫描配置。

## 结论

- Java 后端领域包固定按 `controller/service/mapper/entity/dto/vo` 分层，例如 `paper/controller/PaperController.java`、`paper/service/PaperService.java`、`paper/mapper/PaperMapper.java`。
- 持久层优先使用 MyBatis-Plus Mapper，不再为业务表新增 `JdbcTemplate Repository`。
- 简单 CRUD 用 `BaseMapper<T>`；复杂查询可以在 Mapper 接口里用 `@Select` + `<script>` 动态 SQL。
- MyBatis-Plus 实体必须有无参构造、可写 setter，并用 `@TableName`、`@TableId`、`@TableField` 显式绑定下划线字段。
- `@MapperScan` 不要直接放在启动类上；放到独立 `@Configuration`，避免 `@WebMvcTest` 切片测试加载 Mapper 但没有 `SqlSessionFactory`。

## 避坑

- 不能只移动文件夹不改包名；移动后立即跑 `mvn test`，让编译器暴露断掉的 import。
- Windows/PowerShell 机械搬文件后，注意 `.java` 文件可能带 UTF-8 BOM；如果 Maven 报 `非法字符: '\ufeff'`，先去掉 BOM 再继续查业务错误。
- Mapper 改造后，单元测试也要从旧 Repository mock 改成 Mapper mock，例如 `selectById`、`selectList`、`selectCount`、`insert`、`updateById`。
- `@WebMvcTest` 只测 controller，依赖用 `@MockBean`；不要让它加载数据库、Redis 或 MyBatis 真实配置。
- 旧 Repository 类名必须彻底消除，检查命令: `rg "\b(ResearchTagRepository|PaperRepository|PaperTagRepository|UserRepository)\b" java-service/src`。

## 本次落地

- 新增 `mybatis-plus-boot-starter` 依赖。
- 新增 `config/MyBatisPlusConfig.java` 管理 Mapper 扫描。
- `UserAccount`、`ResearchTag`、`Paper` 改成 MyBatis-Plus 实体。
- `UserMapper`、`ResearchTagMapper`、`PaperMapper`、`PaperTagMapper` 替换旧 Repository。
- 测试通过: `mvn test`，结果为 `Tests run: 65, Failures: 0, Errors: 0, Skipped: 4`。
