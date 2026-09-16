# PowerShell скрипты для подключения к Raspberry Pi

# Подключение к Raspberry Pi через SSH
function Connect-RPi {
    param(
        [string]$Host = (Get-Content credentials\.env | Select-String "RPI_HOST=" | Split-String -Delimiter "=" -Skip 1),
        [string]$User = "Andrey"
    )
    
    Write-Host "🔌 Подключение к Raspberry Pi..."
    Write-Host "   Host: $Host"
    Write-Host "   User: $User"
    Write-Host ""
    
    ssh "${User}@${Host}"
}

# Docker команды для Raspberry Pi
function Invoke-RPiDocker {
    param(
        [string]$Image = "",
        [string[]]$Args
    )
    
    $env:DOCKER_HOST = (Get-Content credentials\.env | Select-String "DOCKER_HOST=" | Split-String -Delimiter "=" -Skip 1)
    
    if ($Args -contains "ps") {
        docker -H $env:DOCKER_HOST ps -a
    }
    elseif ($Args -contains "pull") {
        docker -H $env:DOCKER_HOST pull $Image
    }
    elseif ($Args -contains "run") {
        docker -H $env:DOCKER_HOST run -d $Args
    }
    else {
        docker -H $env:DOCKER_HOST $Args
    }
}

# Проверка подключения
function Test-RPiConnection {
    Write-Host "🔍 Проверка подключения к Raspberry Pi..."
    
    $envFile = "credentials\.env"
    if (-not (Test-Path $envFile)) {
        Write-Host "❌ Ошибка: credentials\.env не найден!" -ForegroundColor Red
        return $false
    }
    
    $rpiHost = (Get-Content $envFile | Select-String "RPI_HOST=" | Split-String -Delimiter "=" -Skip 1)
    $portainerUrl = (Get-Content $envFile | Select-String "PORTAINER_URL=" | Split-String -Delimiter "=" -Skip 1)
    
    Write-Host "   RPI_HOST: $rpiHost"
    Write-Host "   PORTAINER: $portainerUrl"
    Write-Host ""
    
    # Проверка пинга
    $ping = Test-Connection -ComputerName $rpiHost -Count 1 -Quiet -ErrorAction SilentlyContinue
    if ($ping) {
        Write-Host "✅ Хост доступен" -ForegroundColor Green
    }
    else {
        Write-Host "❌ Хост не доступен" -ForegroundColor Red
    }
    
    return $ping
}

# Portainer URL
function Get-PortainerUrl {
    $url = (Get-Content credentials\.env | Select-String "PORTAINER_URL=" | Split-String -Delimiter "=" -Skip 1)
    Write-Host "📊 Portainer URL: $url"
    Write-Host "🔐 Логин: admin"
    Write-Host "💡 Откройте в браузере: $url"
    Start-Process $url
}
