import os
import socket
from contextlib import closing

from fastapi import FastAPI
from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    service: str
    modelReady: bool


app = FastAPI(title="Paper Python Infer", version="0.1.0")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="UP",
        service="python-infer",
        modelReady=False,
    )


def _read_port() -> int:
    raw_port = os.getenv("PYTHON_INFER_PORT", "8001")
    try:
        return int(raw_port)
    except ValueError as exc:
        raise SystemExit(f"PYTHON_INFER_PORT must be an integer, got: {raw_port}") from exc


def _is_port_available(host: str, port: int) -> bool:
    with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
        try:
            sock.bind((host, port))
        except OSError:
            return False
    return True


def _run_dev_server() -> None:
    import uvicorn

    host = os.getenv("PYTHON_INFER_HOST", "127.0.0.1")
    port = _read_port()

    # 提前检查端口，避免 PyCharm 重复启动时只看到 Uvicorn 的底层报错。
    if not _is_port_available(host, port):
        raise SystemExit(
            f"Python 推理服务端口 {host}:{port} 已被占用。"
            "请先停止旧的 PyCharm/run-dev 进程，或设置 "
            "PYTHON_INFER_PORT=18001 并同步更新 Java 的 APP_PYTHON_INFER_URL。"
        )

    uvicorn.run("app.main:app", host=host, port=port)


if __name__ == "__main__":
    _run_dev_server()
