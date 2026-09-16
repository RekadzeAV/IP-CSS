# Script: Start Docker with NAS Integration
# Date: 2026-06-09
# Purpose: Start Docker containers with LDAP and NAS integration

$envFile = ".env.nas"

Write-Output "=========================================="
Write-Output "  Docker Start with NAS Integration"
Write-Output "=========================================="
Write-Output ""

# Check if .env.nas exists
if (Test-Path $envFile) {
    Write-Output "✅ Loading environment from $envFile"
    
    # Read and export environment variables
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.*)$') {
            $varName = $matches[1].Trim()
            $varValue = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($varName, $varValue, "Process")
            Write-Output "  Exported: $varName"
        }
    }
} else {
    Write-Output "❌ $envFile not found!"
    Write-Output "Please create .env.nas file first."
    exit 1
}

Write-Output ""
Write-Output "=== Environment Variables ==="
Write-Output "LDAP_ENABLED: $([Environment]::GetEnvironmentVariable('LDAP_ENABLED'))"
Write-Output "AUTH_LDAP_SERVER: $([Environment]::GetEnvironmentVariable('AUTH_LDAP_SERVER'))"
Write-Output "AUTH_LDAP_BASE_DN: $([Environment]::GetEnvironmentVariable('AUTH_LDAP_BASE_DN'))"
Write-Output "NAS_PRIMARY_HOST: $([Environment]::GetEnvironmentVariable('NAS_PRIMARY_HOST'))"
Write-Output ""

# Check if NFS volume is enabled
$nfsEnabled = $env:NFS_ENABLED -eq "true"
if ($nfsEnabled) {
    Write-Output "⚠️  NFS volume mount is enabled"
    Write-Output "Make sure NAS is accessible and mounted on host"
    Write-Output ""
}

# Stop existing containers
Write-Output "=== Stopping existing containers ==="
docker-compose down
Write-Output ""

# Start containers
Write-Output "=== Starting containers ==="
docker-compose up -d
Write-Output ""

# Wait for services to be healthy
Write-Output "=== Waiting for services to be healthy ==="
Start-Sleep -Seconds 10

$retryCount = 0
$maxRetries = 10

while ($retryCount -lt $maxRetries) {
    Write-Output "Checking health... (attempt $($retryCount + 1)/$maxRetries)"
    
    $health = docker-compose ps --format json | ConvertFrom-Json
    $allHealthy = $true
    
    foreach ($service in $health) {
        if ($service.Status -notlike "*healthy*") {
            $allHealthy = $false
            Write-Output "  ⏳ $($service.Service): $($service.Status)"
        } else {
            Write-Output "  ✅ $($service.Service): healthy"
        }
    }
    
    if ($allHealthy) {
        Write-Output ""
        Write-Output "✅ All services are healthy!"
        break
    }
    
    Start-Sleep -Seconds 5
    $retryCount++
}

if ($retryCount -eq $maxRetries) {
    Write-Output ""
    Write-Output "⚠️  Timeout waiting for services to be healthy"
    Write-Output "Check logs with: docker-compose logs"
}

Write-Output ""
Write-Output "=== Service Status ==="
docker-compose ps

Write-Output ""
Write-Output "=== Health Check ==="
$healthCheck = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -UseBasicParsing
$healthJson = $healthCheck.Content | ConvertFrom-Json
Write-Output "Status: $($healthJson.success)"
Write-Output "Database: $($healthJson.data.checks.database)"
Write-Output "Redis: $($healthJson.data.checks.redis)"
Write-Output "FFmpeg: $($healthJson.data.checks.ffmpeg)"
Write-Output "Storage: $($healthJson.data.checks.storage)"

Write-Output ""
Write-Output "=========================================="
Write-Output "  Docker Started Successfully!"
Write-Output "=========================================="
Write-Output ""
Write-Output "Access URLs:"
Write-Output "  API: http://localhost:8080"
Write-Output "  Health: http://localhost:8080/api/v1/health"
Write-Output "  Web UI: http://localhost:3000 (if running)"
Write-Output ""
Write-Output "LDAP Configuration:"
Write-Output "  Enabled: $([Environment]::GetEnvironmentVariable('LDAP_ENABLED'))"
Write-Output "  Server: $([Environment]::GetEnvironmentVariable('AUTH_LDAP_SERVER'))"
Write-Output "  Base DN: $([Environment]::GetEnvironmentVariable('AUTH_LDAP_BASE_DN'))"
Write-Output ""
Write-Output "NAS Configuration:"
Write-Output "  Primary Host: $([Environment]::GetEnvironmentVariable('NAS_PRIMARY_HOST'))"
Write-Output "  Protocol: $([Environment]::GetEnvironmentVariable('NAS_PROTOCOL'))"
Write-Output "  Share Path: $([Environment]::GetEnvironmentVariable('NAS_SHARE_PATH'))"
Write-Output ""
Write-Output "Commands:"
Write-Output "  docker-compose logs -f          # View logs"
Write-Output "  docker-compose ps               # Check status"
Write-Output "  docker-compose down             # Stop containers"
Write-Output ""
