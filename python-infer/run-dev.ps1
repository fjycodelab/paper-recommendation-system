param(
    [int] $Port = 8001
)

$ErrorActionPreference = "Stop"

$python = "D:\Anaconda\Anaconda\envs\cd-agent\python.exe"

if (-not (Test-Path $python)) {
    throw "cd-agent Python not found: $python"
}

$env:PYTHON_INFER_PORT = "$Port"
& $python app/main.py
