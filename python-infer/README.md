# Python Infer

FastAPI 推理服务目录。

## 职责

- 第一阶段提供 `/health` 健康检查。
- 后续接入 embedding 和相似度计算接口。
- 不处理登录、权限和业务数据库写入。

## 当前状态

T8 已完成 FastAPI 服务骨架、`cd-agent` 环境配置和端到端健康检查。当前提供 `/health`，暂不加载真实模型。

## 本机运行

Python 服务固定使用 Conda 环境 `cd-agent`:

```text
D:\Anaconda\Anaconda\envs\cd-agent\python.exe
```

首次安装依赖:

```powershell
& "D:\Anaconda\Anaconda\envs\cd-agent\python.exe" -m pip install -r requirements.txt
```

启动服务:

```powershell
.\run-dev.ps1
```

PyCharm 中可以直接运行 `app/main.py`:

- Python interpreter: `D:\Anaconda\Anaconda\envs\cd-agent\python.exe`
- Working directory: `D:\code\codex_2\lencode\javacode\python-infer`
- Script path: `D:\code\codex_2\lencode\javacode\python-infer\app\main.py`

健康检查:

```powershell
Invoke-RestMethod http://127.0.0.1:8001/health
```

## 常见问题

如果启动时报 `address ('127.0.0.1', 8001)` 已被占用，说明旧的 Python 服务还在运行，或者 PyCharm 重复启动了同一个服务。

先查占用进程:

```powershell
Get-NetTCPConnection -LocalAddress 127.0.0.1 -LocalPort 8001 | Select-Object LocalAddress,LocalPort,State,OwningProcess
```

如果确认是旧的 Python 推理服务，在 PyCharm 里停止旧运行配置后再启动。临时换端口时，Python 和 Java 要一起改:

```powershell
$env:PYTHON_INFER_PORT="18001"
$env:APP_PYTHON_INFER_URL="http://127.0.0.1:18001"
```
