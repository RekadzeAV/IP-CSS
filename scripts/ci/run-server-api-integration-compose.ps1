$ErrorActionPreference = "Stop"

$composeFile = "docker-compose.integration.yml"

function Cleanup {
    try {
        docker compose -f $composeFile down -v --remove-orphans | Out-Null
    } catch {
        Write-Host "[ci] cleanup skipped: $($_.Exception.Message)"
    }
}

function Wait-Healthy([string]$containerName) {
    $maxAttempts = 40
    for ($i = 1; $i -le $maxAttempts; $i++) {
        $status = ""
        try {
            $status = docker inspect -f "{{.State.Health.Status}}" $containerName
        } catch {
            $status = ""
        }

        if ($status -eq "healthy") {
            Write-Host "[ci] $containerName is healthy"
            return
        }

        Write-Host "[ci] waiting for $containerName health ($i/$maxAttempts), current='$status'"
        Start-Sleep -Seconds 3
    }

    throw "[ci] $containerName did not become healthy in time"
}

try {
    Write-Host "[ci] starting integration dependencies via docker compose"
    docker compose -f $composeFile up -d postgres redis

    Wait-Healthy "ipcss-it-postgres"
    Wait-Healthy "ipcss-it-redis"

    Write-Host "[ci] running server API integration smoke tests"
    $env:DB_MODE = "postgres"
    $env:DATABASE_URL = "jdbc:postgresql://localhost:55432/ipcss_test"
    $env:DATABASE_USER = "$env:DATABASE_PASSWORD"
    $env:DATABASE_PASSWORD = "$env:DATABASE_PASSWORD"
    $env:ENABLE_FLYWAY = "true"
    $env:REDIS_HOST = "localhost"
    $env:REDIS_PORT = "56379"
    $env:API_GLOBAL_RATE_LIMIT_ENABLED = "false"

    .\gradlew.bat :server:api:test `
        --tests "*DatabaseComposeIntegrationTest" `
        --tests "*CameraRoutesAuthIntegrationTest" `
        --tests "*AuditRoutesIntegrationTest" `
        --tests "*HealthReadyIntegrationTest" `
        --no-daemon
}
finally {
    Cleanup
}
