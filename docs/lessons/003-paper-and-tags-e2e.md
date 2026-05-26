# Lesson 003: Spec 002 端到端验收与下载链路

> 日期: 2026-05-26
> 对应: Spec 002 / T10

## 结论

- Spec 002 的主链路在 Java 服务内闭环，Python 推理服务未启动时 `/api/health` 会是 `DEGRADED`，但论文、标签、arXiv 导入和下载验收不受影响。
- arXiv 导入接口请求体字段是 `query` 和 `maxResults`，不是 `searchQuery`。
- arXiv 一次导入上限保持 100 条；重复导入依赖 `source='arXiv' + source_paper_id` 去重。
- PDF 下载目录必须放在 git 仓库之外，当前默认是 `D:\code\codex_2\lencode\paper-downloads`。
- 下载接口只记录状态、文件名、文件大小、本机路径和失败原因，不在系统内做 PDF 对外分发。

## 本次验收结果

- Java 测试通过: `Tests run: 95, Failures: 0, Errors: 0, Skipped: 0`。
- 前端构建通过: `npm run build` 成功。
- arXiv 真实导入通过: `requested=100, imported=100, skipped=0, failed=0`。
- 真实 PDF 下载成功: `SUCCESS`，文件 `paper-101-1779785086503-dummy.pdf`，13264 bytes。
- 失败下载记录成功: 非法下载链接返回 `FAILED`，记录失败原因。
- 管理员软删除、回收站查询、恢复通过；普通用户可查看列表和详情。
- MySQL 复核: `papers=102`、`ACTIVE=102`、`DELETED=0`、下载记录 `SUCCESS=1`、`FAILED=1`、`research_tags=28`。

## 避坑

- Windows `Start-Process` 启动 Maven 时，带空格的 `-Dmaven.repo.local=D:\study software\maven-repository` 必须作为一个完整带引号参数，否则 Maven 会报 `Unknown lifecycle phase "software\maven-repository"`。
- MySQL Shell 在沙箱用户下可能无法写 `C:\Users\fjy\AppData\Roaming\MySQL\mysqlsh\mysqlsh.log`，会导致读库命令失败；本机验收可在正常用户环境执行。
- PowerShell 控制台有时会把 Java 返回的中文消息显示成乱码，不代表数据库或接口内容损坏；验收结论优先看状态码、状态字段和计数。
- 真实下载要同时覆盖成功和失败。成功可用小型公开 PDF 验证保存链路，失败可用非法 URL 验证失败原因记录。
- 不要清理或批量删除下载目录；需要清理时只删除明确的单个文件，或让用户手动处理。

## 下一步影响

- Spec 003 可以直接复用 `paper_download_attempts` 和论文详情入口，继续做下载点击、浏览、收藏、评分等行为记录。
- Spec 004/005 再接 Python embedding 和推荐逻辑；不要把 embedding 或推荐提前塞回 Spec 002。
