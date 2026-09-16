# IP-CSS Local Docker Build and Run Script
# Builds and starts IP-CSS with all dependencies locally

$DOCKER_PATH = "C:\Program Files\Docker\Docker\resources\bin"
$env:Path = "$DOCKER_PATH;$env:Path"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  IP-CSS Local Docker Build & Run" -ForegroundColor Cyan
Write-Host "  Version: 1.0.0-beta" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
Write-Host "[1/6] Checking Docker..." -ForegroundColor Yellow
$dockerVersion = & docker --version
Write-Host "  $dockerVersion" -ForegroundColor Green

# Check Docker daemon
Write-Host "[2/6] Checking Docker daemon..." -ForegroundColor Yellow
$serverVersion = & docker info --format "{{.ServerVersion}}" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Docker daemon is running (v$serverVersion)" -ForegroundColor Green
} else {
    Write-Host "  ERROR: Docker daemon is not running!" -ForegroundColor Red
    Write-Host "  Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}

# Stop existing containers
Write-Host "[3/6] Stopping existing containers..." -ForegroundColor Yellow
& docker-compose -f docker-compose.local.yml down --remove-orphans 2>&1 | Out-Null

# Build Docker image
Write-Host "[4/6] Building Docker image..." -ForegroundColor Yellow
Write-Host "  This may take a few minutes..." -ForegroundColor Gray
& docker-compose -f docker-compose.local.yml build surveillance 2>&1 | ForEach-Object {
    if ($_ -match "Step|Successfully| --->") {
        Write-Host "  $_" -ForegroundColor Gray
    } else {
        Write-Host "  $_" -ForegroundColor DarkGray
    }
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Docker build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "  Docker image built successfully!" -ForegroundColor Green

# Pull required images
Write-Host "[5/6] Pulling required images..." -ForegroundColor Yellow
Write-Host "  - postgres:15-alpine" -ForegroundColor Gray
Write-Host "  - redis:7-alpine" -ForegroundColor Gray
Write-Host "  - bluenviron/mediamtx:latest" -ForegroundColor Gray
& docker-compose -f docker-compose.local.yml pull 2>&1 | ForEach-Object {
    if ($_ -match "Pulling|Pull complete|Already exists") {
        Write-Host "  $_" -ForegroundColor Gray
    }
}
Write-Host "  Images pulled successfully!" -ForegroundColor Green

# Start containers
Write-Host "[6/6] Starting containers..." -ForegroundColor Yellow
& docker-compose -f docker-compose.local.yml up -d 2>&1 | ForEach-Object {
    Write-Host "  $_" -ForegroundColor Gray
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Failed to start containers!" -ForegroundColor Red
    exit 1
}

# Wait for health checks
Write-Host ""
Write-Host "Waiting for services to become healthy..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# Show status
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Container Status" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
& docker-compose -f docker-compose.local.yml ps

# Show logs
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Access Points" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Web UI:    http://localhost:8080" -ForegroundColor Green
Write-Host "  Database:  localhost:5432 (user: surveillance)" -ForegroundColor Green
Write-Host "  Redis:     localhost:6379" -ForegroundColor Green
Write-Host "  RTSP:      localhost:8554" -ForegroundColor Green
Write-Host ""
Write-Host "  Default credentials: admin / admin_2026_secure" -ForegroundColor Yellow
Write-Host ""
Write-Host "  View logs: docker-compose -f docker-compose.local.yml logs -f" -ForegroundColor Gray
Write-Host "  Stop:      docker-compose -f docker-compose.local.yml down" -ForegroundColor Gray
Write-Host ""
Write-Host "IP-CSS is running locally!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
