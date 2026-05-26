param(
    [string] $ApiProxyTarget = ""
)

$ErrorActionPreference = "Stop"

$npm = "C:\Program Files\nodejs\npm.cmd"

if (-not (Test-Path $npm)) {
    throw "npm not found: $npm"
}

if ($ApiProxyTarget) {
    $env:VITE_API_PROXY_TARGET = $ApiProxyTarget
}

& $npm run dev
