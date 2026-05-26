# Frontend

Vue/Vite + Element Plus 前端目录。

## 职责

- 提供注册页、登录页和综合仪表盘。
- 登录后保存 token，刷新页面时恢复当前用户。
- 只调用 Java 服务 API，不直接访问 MySQL、Redis 或 Python 服务。

## 当前状态

T8 已完成前端启动和端到端验收。当前前端提供登录、注册、刷新恢复当前用户和仪表盘。

## 本机运行

默认把 `/api` 代理到 `http://127.0.0.1:8080`:

```powershell
npm install
npm run dev
```

如果 IDEA 里 8080 正在跑旧服务，可以临时指定代理目标:

```powershell
$env:VITE_API_PROXY_TARGET = "http://127.0.0.1:18080"
npm run dev
```

构建检查:

```powershell
npm run build
```

如果 VSCode 终端仍提示 `npm` 不是可识别命令，先关闭 VSCode 再重新打开；环境变量已写入用户级 `Path`，旧终端不会自动刷新。

访问:

```text
http://127.0.0.1:5173/login
```
