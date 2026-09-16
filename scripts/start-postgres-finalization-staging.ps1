# Starts local staging runtime for PostgreSQL finalization checks (1.5.6).
# Uses docker compose when available and waits for API health endpoint.
#
# Usage:
#   .\scripts\start-postgres-finalization-staging.ps1
#   .\scripts\start-postgres-finalization-staging.ps1 -BaseUrl "http://localhost:8080" -TimeoutSec 180

param(
    [switch]$ShowHelp,
    [string]$BaseUrl = "http://localhost:8080",
    [int]$TimeoutSec = 180
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Start local staging runtime for PostgreSQL finalization (docker compose + health wait)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\start-postgres-finalization-staging.ps1"
    Write-Host "  .\scripts\start-postgres-finalization-staging.ps1 -BaseUrl http://localhost:8080 -TimeoutSec 240"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -BaseUrl  API root used for /api/v1/health probe"
    Write-Host "  -TimeoutSec  Max seconds to wait for health after compose up"
    Write-Host ""
    Write-Host "Steps:"
    Write-Host "  If health is already OK: exit early. Else if docker in PATH: docker compose up -d from repo root, then poll health."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  API already healthy, or docker compose started and health became OK"
    Write-Host "  1  Docker not in PATH, or health endpoint did not become ready before -TimeoutSec"
    Write-Host "  Non-zero  docker-compose.yml missing (throw) or docker compose up failed (throw)"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$composeFile = Join-Path $projectRoot "docker-compose.yml"

if (-not (Test-Path $composeFile)) {
    throw "docker-compose.yml not found: $composeFile"
}

function Test-ApiHealth {
    param([string]$Url)
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri "$Url/api/v1/health" -TimeoutSec 5
        return ($resp.StatusCode -eq 200 -or $resp.StatusCode -eq 503)
    } catch {
        return $false
    }
}

if (Test-ApiHealth -Url $BaseUrl) {
    Write-Host "API already reachable at $BaseUrl" -ForegroundColor Green
    exit 0
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker is not available in PATH." -ForegroundColor Yellow
    Write-Host "Start server manually, then re-run suite:" -ForegroundColor Yellow
    Write-Host ".\scripts\run-postgres-finalization-suite.ps1 -BaseUrl `"$BaseUrl`" -Username `"admin`" -Password `"<pwd>`"" -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting docker compose services..." -ForegroundColor Cyan
Push-Location $projectRoot
try {
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up -d failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host "Waiting for API health at $BaseUrl ..." -ForegroundColor Cyan
$start = Get-Date
while (((Get-Date) - $start).TotalSeconds -lt $TimeoutSec) {
    if (Test-ApiHealth -Url $BaseUrl) {
        Write-Host "API is reachable." -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 5
}

Write-Host "Timeout waiting for API health endpoint." -ForegroundColor Red
exit 1
