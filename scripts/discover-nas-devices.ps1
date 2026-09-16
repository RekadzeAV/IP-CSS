# Скрипт для автоматического обнаружения NAS устройств и камер в сети
# Usage: .\scripts\discover-nas-devices.ps1 [-NetworkRange 192.168.1.0/24]

[CmdletBinding()]
param(
    [string]$NetworkRange = "192.168.1.0/24",
    [switch]$ScanCameras,
    [switch]$ScanNAS,
    [int]$Timeout = 1000,
    [string]$OutputFile = "docs/reports/network-discovery-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
)

$ErrorActionPreference = "Stop"

Write-Host "Начинаем сканирование сети: $NetworkRange" -ForegroundColor Cyan

# Парсим сетевой диапазон
$subnet = $NetworkRange -replace '/\d+$', ''
$powers = [int]($NetworkRange -split '/' | Select-Object -Last 1)
$hosts = [Math]::Pow(2, (32 - $powers)) - 2

Write-Host "Сканируем диапазон: $subnet с подсетью /$powers"
Write-Host "Ожидаемое количество хостов: $hosts"

$discoveredDevices = @{
    NAS = @()
    Cameras = @()
    ScanTime = Get-Date
    Network = $NetworkRange
}

# Функция для проверки доступности хоста
function Test-Host {
    param([string]$IP, [int]$Timeout)
    
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $async = $tcp.BeginConnect($IP, 80, $null, $null)
        $wait = $async.AsyncWaitHandle.WaitOne($Timeout)
        
        if ($wait) {
            $tcp.Close()
            return $true
        }
        else {
            $tcp.Close()
            return $false
        }
    }
    catch {
        return $false
    }
}

# Функция для обнаружения Synology NAS
function Test-Synology {
    param([string]$IP)
    
    $ports = @(5000, 5001, 80, 443)
    
    foreach ($port in $ports) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $async = $tcp.BeginConnect($IP, $port, $null, $null)
            $wait = $async.AsyncWaitHandle.WaitOne(500)
            
            if ($wait) {
                $tcp.Close()
                return $true
            }
        }
        catch {
            continue
        }
    }
    
    return $false
}

# Функция для обнаружения QNAP NAS
function Test-QNAP {
    param([string]$IP)
    
    $ports = @(8080, 8081, 80, 443)
    
    foreach ($port in $ports) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $async = $tcp.BeginConnect($IP, $port, $null, $null)
            $wait = $async.AsyncWaitHandle.WaitOne(500)
            
            if ($wait) {
                $tcp.Close()
                return $true
            }
        }
        catch {
            continue
        }
    }
    
    return $false
}

# Функция для обнаружения камер (RTSP/ONVIF)
function Test-Camera {
    param([string]$IP)
    
    # Проверяем RTSP порт
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $async = $tcp.BeginConnect($IP, 554, $null, $null)
        $wait = $async.AsyncWaitHandle.WaitOne(500)
        
        if ($wait) {
            $tcp.Close()
            return @{Found = $true; Type = "RTSP"; Port = 554}
        }
    }
    catch {
        # RTSP не доступен, пробуем ONVIF
    }
    
    # Проверяем ONVIF порт
    $onvifPorts = @(80, 8080, 8081)
    foreach ($port in $onvifPorts) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $async = $tcp.BeginConnect($IP, $port, $null, $null)
            $wait = $async.AsyncWaitHandle.WaitOne(500)
            
            if ($wait) {
                $tcp.Close()
                return @{Found = $true; Type = "ONVIF"; Port = $port}
            }
        }
        catch {
            continue
        }
    }
    
    return @{Found = $false}
}

# Функция для получения информации об устройстве
function Get-DeviceInfo {
    param([string]$IP)
    
    $info = @{
        IP = $IP
        DiscoverTime = Get-Date
    }
    
    # Пробуем получить информацию через HTTP
    try {
        $response = Invoke-WebRequest -Uri "http://$IP" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
        if ($response) {
            $info.WebServer = $response.Headers.'Server'
            $info.Title = $response.Content | Select-String -Pattern '<title>(.*?)</title>' | ForEach-Object { $_.Matches.Groups[1].Value }
        }
    }
    catch {
        $info.WebServer = "Unknown"
    }
    
    return $info
}

# Основной цикл сканирования
$hostRange = $subnet -split '\.' | ForEach-Object { [int]$_ }
$lastOctetStart = $hostRange[3]
$lastOctetEnd = $lastOctetStart + [Math]::Min($hosts, 254)

Write-Host "Запускаем параллельное сканирование..." -ForegroundColor Yellow

# Сканирование NAS устройств
if ($ScanNAS -or -not $ScanCameras) {
    Write-Host "`n[1/2] Поиск NAS устройств..." -ForegroundColor Cyan
    
    $nasJobs = @()
    for ($i = 1; $i -le 254; $i++) {
        $ip = "$($hostRange[0]).$($hostRange[1]).$($hostRange[2]).$i"
        
        $job = Start-Job -ScriptBlock {
            param($IP)
            $syno = Test-Synology -IP $IP
            $qnap = Test-QNAP -IP $IP
            
            if ($syno) {
                return @{Type = "Synology"; IP = $IP; Status = "Found"}
            }
            elseif ($qnap) {
                return @{Type = "QNAP"; IP = $IP; Status = "Found"}
            }
            else {
                return $null
            }
        } -ArgumentList $ip
        
        $nasJobs += $job
        
        # Ограничиваем количество одновременных задач
        if ($nasJobs.Count -ge 50) {
            $completed = $nasJobs | Where-Object { $_.State -eq "Completed" }
            foreach ($job in $completed) {
                $result = Receive-Job -Job $job
                if ($result) {
                    $nasDevice = Get-DeviceInfo -IP $result.IP
                    $nasDevice.Type = $result.Type
                    $discoveredDevices.NAS += $nasDevice
                    Write-Host "  OK Найдено NAS: $($result.Type) - $($result.IP)" -ForegroundColor Green
                }
                Remove-Job -Job $job
            }
            $nasJobs = $nasJobs | Where-Object { $_.State -ne "Completed" }
        }
    }
    
    # Ждем завершения всех задач
    $nasJobs | Wait-Job
    foreach ($job in $nasJobs) {
        $result = Receive-Job -Job $job
        if ($result) {
            $nasDevice = Get-DeviceInfo -IP $result.IP
            $nasDevice.Type = $result.Type
            $discoveredDevices.NAS += $nasDevice
            Write-Host "  OK Найдено NAS: $($result.Type) - $($result.IP)" -ForegroundColor Green
        }
        Remove-Job -Job $job
    }
}

# Сканирование камер
if ($ScanCameras) {
    Write-Host "`n[2/2] Поиск камер..." -ForegroundColor Cyan
    
    $cameraJobs = @()
    for ($i = 1; $i -le 254; $i++) {
        $ip = "$($hostRange[0]).$($hostRange[1]).$($hostRange[2]).$i"
        
        $job = Start-Job -ScriptBlock {
            param($IP)
            Test-Camera -IP $IP
        } -ArgumentList $ip
        
        $cameraJobs += $job
        
        if ($cameraJobs.Count -ge 50) {
            $completed = $cameraJobs | Where-Object { $_.State -eq "Completed" }
            foreach ($job in $completed) {
                $result = Receive-Job -Job $job
                if ($result.Found) {
                    $camera = Get-DeviceInfo -IP $job.ArgumentList[0]
                    $camera.Type = $result.Type
                    $camera.Port = $result.Port
                    $discoveredDevices.Cameras += $camera
                    Write-Host "  OK Найдена камера: $($result.Type) - $($camera.IP):$($result.Port)" -ForegroundColor Green
                }
                Remove-Job -Job $job
            }
            $cameraJobs = $cameraJobs | Where-Object { $_.State -ne "Completed" }
        }
    }
    
    # Ждем завершения
    $cameraJobs | Wait-Job
    foreach ($job in $cameraJobs) {
        $result = Receive-Job -Job $job
        if ($result.Found) {
            $camera = Get-DeviceInfo -IP $job.ArgumentList[0]
            $camera.Type = $result.Type
            $camera.Port = $result.Port
            $discoveredDevices.Cameras += $camera
            Write-Host "  OK Найдена камера: $($result.Type) - $($camera.IP):$($result.Port)" -ForegroundColor Green
        }
        Remove-Job -Job $job
    }
}

# Сохраняем результаты
$discoveredDevices.NAS | ForEach-Object { $_ | Add-Member -NotePropertyName "ScanType" -NotePropertyValue "NAS" }
$discoveredDevices.Cameras | ForEach-Object { $_ | Add-Member -NotePropertyName "ScanType" -NotePropertyValue "Camera" }

$allDevices = $discoveredDevices.NAS + $discoveredDevices.Cameras
$allDevices | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputFile -Encoding UTF8

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Результаты сканирования:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "NAS устройств найдено: $($discoveredDevices.NAS.Count)" -ForegroundColor $(if ($discoveredDevices.NAS.Count -gt 0) { "Green" } else { "Yellow" })
Write-Host "Камер найдено: $($discoveredDevices.Cameras.Count)" -ForegroundColor $(if ($discoveredDevices.Cameras.Count -gt 0) { "Green" } else { "Yellow" })
Write-Host "Результаты сохранены: $OutputFile" -ForegroundColor Cyan

# Автоконфигурация для найденных устройств
if ($discoveredDevices.NAS.Count -gt 0) {
    Write-Host "`nРекомендуемые настройки для field validation:" -ForegroundColor Yellow
    
    foreach ($nas in $discoveredDevices.NAS) {
        Write-Host "  - $($nas.Type): $($nas.IP)" -ForegroundColor White
    }
}
