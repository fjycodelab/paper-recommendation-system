$ErrorActionPreference = "Stop"

$npm = "C:\Program Files\nodejs\npm.cmd"

if (-not (Test-Path $npm)) {
    throw "npm not found: $npm"
}

& $npm install
