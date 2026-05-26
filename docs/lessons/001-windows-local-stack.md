# Lesson 001: Windows 本机多服务启动

> 日期: 2026-05-25
> 对应: Spec 001 登录认证与项目基础骨架

## 结论

- 带空格路径必须全程加引号，例如 `D:\study software\...`。
- Maven 在当前 shell 里不一定有 `mvn`，本机固定使用 `D:\study software\apache-maven-3.9.13\bin\mvn.cmd` 更稳定。
- Maven 本地仓库按用户约定固定为 `D:\study software\maven-repository`，IDEA 也应配置到同一路径。
- Python 推理服务固定使用 Conda 环境 `D:\Anaconda\Anaconda\envs\cd-agent`，PyCharm 直接运行 `python-infer/app/main.py`。
- 前端使用 VSCode 运行，Node/npm 走用户级环境变量: `NODE_HOME=C:\Program Files\nodejs`，`Path` 包含 `C:\Program Files\nodejs` 和 `C:\Users\fjy\AppData\Roaming\npm`。
- VSCode PowerShell 终端额外通过 `C:\Users\fjy\Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1` 补齐 Node/npm PATH，避免 VSCode 继承旧环境后找不到 `npm`。
- Redis 配置和数据放在项目外的 `D:\study software\redis`，项目只记录启动与验证命令。

## 避坑

- 8080 如果已经被 IDEA 中的旧 Java 进程占用，前端代理会打到旧接口，表现为登录 404。处理方式是重启 IDEA 里的最新 Java 服务，或临时使用 `JAVA_SERVICE_PORT` 和 `VITE_API_PROXY_TARGET` 指定新端口。
- 修改 Windows 环境变量后，已打开的 VSCode 终端不会自动刷新；需要新开终端，必要时重启 VSCode。若仍找不到 `npm`，检查 PowerShell profile 是否加载。
- 前端构建产物、日志、`node_modules`、Python `__pycache__` 都不要提交。
- 不要批量删除目录；临时文件如需清理，只删除明确的单个文件。
