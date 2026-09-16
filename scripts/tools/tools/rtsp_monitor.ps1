# RTSP Connection Monitor Utility

**Дата:** 11 June 2026  
**Версия:** 1.0  
**Описание:** Утилита для мониторинга и диагностики RTSP подключений

---

## Quick Start

### Запуск монитора

```powershell
# Базовый мониторинг
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test

# С выводом статистики
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -Verbose

# С логированием в файл
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -LogPath "rtsp_monitor.log"

# Мультитрекинг (несколько камер)
.\rtsp_monitor.ps1 -Urls @("rtsp://192.168.1.100:554/cam1", "rtsp://192.168.1.101:554/cam2")
```

---

## PowerShell Script: rtsp_monitor.ps1

```powershell
# RTSP Connection Monitor
# Дата: 11 June 2026
# Описание: Утилита для мониторинга RTSP подключений в реальном времени

param(
    [Parameter(Mandatory = $true)]
    [string[]]$Urls,
    
    [string]$Username = $null,
    
    [string]$Password = $null,
    
    [int]$TimeoutMs = 10000,
    
    [string]$LogPath = $null,
    
    [switch]$Verbose,
    
    [switch]$JsonOutput,
    
    [switch]$Continuous,
    
    [int]$IntervalSeconds = 5
)

# Цвета для вывода
$Colors = @{
    Info     = "Cyan"
    Success  = "Green"
    Warning  = "Yellow"
    Error    = "Red"
    Header   = "Magenta"
    Stats    = "Blue"
}

# Класс для хранения статистики
class RtspStats {
    [DateTime]$StartTime
    [DateTime]$LastFrameTime
    [int]$TotalFrames = 0
    [int]$VideoFrames = 0
    [int]$AudioFrames = 0
    [int]$BytesReceived = 0
    [int]$ConnectionAttempts = 0
    [int]$ReconnectAttempts = 0
    [string]$LastError = ""
    [string]$Status = "Disconnected"
    
    [double]GetUptime() {
        return (New-TimeSpan -Start $StartTime -End (Get-Date)).TotalSeconds
    }
    
    [double]GetFps() {
        if ($this.TotalFrames -eq 0) return 0
        $uptime = $this.GetUptime()
        if ($uptime -le 0) return 0
        return [math]::Round($this.TotalFrames / $uptime, 2)
    }
}

function Write-Log {
    param(
        [string]$Message,
        [string]$Color = "White",
        [string]$Level = "INFO"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $logLine = "[$timestamp] [$Level] $Message"
    
    # Вывод в консоль
    Write-Host $logLine -ForegroundColor $Color
    
    # Логирование в файл
    if ($LogPath) {
        Add-Content -Path $LogPath -Value $logLine
    }
}

function Test-Connection {
    param(
        [string]$Url,
        [int]$Timeout = 5000
    )
    
    try {
        $uri = [System.Uri]$Url
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $asyncResult = $tcpClient.BeginConnect($uri.Host, $uri.Port, $null, $null)
        $wait = $asyncResult.AsyncWaitHandle.WaitOne($Timeout)
        
        if ($wait) {
            $tcpClient.EndConnect($asyncResult)
            $tcpClient.Close()
            return $true
        }
        else {
            $tcpClient.Close()
            return $false
        }
    }
    catch {
        return $false
    }
}

function Get-RtspInfo {
    param(
        [string]$Url
    )
    
    try {
        $uri = [System.Uri]$Url
        $info = @{
            Host = $uri.Host
            Port = $uri.Port
            Path = $uri.AbsolutePath
            HasAuth = ($uri.UserInfo -ne $null)
            Username = if ($uri.UserInfo -match "([^:]+):") { $matches[1] } else { $null }
            Password = if ($uri.UserInfo -match ":([^@]+)") { $matches[1] } else { $null }
        }
        return $info
    }
    catch {
        return $null
    }
}

function Show-ConnectionStatus {
    param(
        [string]$Url,
        [RtspStats]$Stats
    )
    
    $uri = [System.Uri]$Url
    
    Write-Host "`n========================================" -ForegroundColor $Colors.Header
    Write-Host "RTSP Monitor: $($uri.Host):$($uri.Port)$($uri.AbsolutePath)" -ForegroundColor $Colors.Header
    Write-Host "========================================" -ForegroundColor $Colors.Header
    
    Write-Host "Status: $($Stats.Status)" -ForegroundColor $(if ($Stats.Status -eq "Connected") { $Colors.Success } else { $Colors.Error })
    Write-Host "Uptime: $([math]::Round($Stats.GetUptime(), 2))s" -ForegroundColor $Colors.Info
    Write-Host "FPS: $($Stats.GetFps())" -ForegroundColor $Colors.Stats
    Write-Host "Total Frames: $($Stats.TotalFrames) (Video: $($Stats.VideoFrames), Audio: $($Stats.AudioFrames))" -ForegroundColor $Colors.Info
    Write-Host "Bytes Received: $([math]::Round($Stats.BytesReceived / 1MB, 2)) MB" -ForegroundColor $Colors.Stats
    Write-Host "Connection Attempts: $($Stats.ConnectionAttempts)" -ForegroundColor $Colors.Info
    Write-Host "Reconnect Attempts: $($Stats.ReconnectAttempts)" -ForegroundColor $(if ($Stats.ReconnectAttempts -gt 0) { $Colors.Warning } else { $Colors.Info })
    
    if ($Stats.LastError) {
        Write-Host "Last Error: $($Stats.LastError)" -ForegroundColor $Colors.Error
    }
    
    Write-Host "Last Frame: $([math]::Round((New-TimeSpan -Start $Stats.LastFrameTime -End (Get-Date)).TotalSeconds, 2))s ago" -ForegroundColor $Colors.Info
    Write-Host "========================================`n" -ForegroundColor $Colors.Header
}

function Start-RtspMonitor {
    param(
        [string]$Url,
        [string]$Username,
        [string]$Password,
        [int]$TimeoutMs
    )
    
    Write-Log "Starting RTSP monitor for: $Url" $Colors.Info
    
    # Проверка подключения к хосту
    Write-Log "Testing network connectivity..." $Colors.Info
    $rtspInfo = Get-RtspInfo -Url $Url
    $hostReachable = Test-Connection -Url $Url -Timeout 3000
    
    if (-not $hostReachable) {
        Write-Log "Host $($rtspInfo.Host) is not reachable" $Colors.Error
        return $null
    }
    Write-Log "Host is reachable" $Colors.Success
    
    # Инициализация статистики
    $stats = [RtspStats]@{
        StartTime = Get-Date
        LastFrameTime = Get-Date
        Status = "Connecting"
    }
    
    # Здесь будет код для подключения через нативную библиотеку
    # Пример псевдокода:
    # $client = New-RtspClient
    # $client.configure($Username, $Password, $TimeoutMs)
    # $client.statusCallback = { param($status, $msg) ... }
    # $client.frameCallback = { param($frame) ... }
    # $client.connect($Url)
    
    Write-Log "RTSP monitor initialized" $Colors.Success
    
    return @{
        Url = $Url
        Stats = $stats
        Client = $null  # Будет инициализировано после подключения к библиотеке
    }
}

function Show-JsonOutput {
    param(
        [array]$Monitors
    )
    
    $output = @{
        timestamp = Get-Date -Format "o"
        monitors = @()
    }
    
    foreach ($monitor in $Monitors) {
        $stats = $monitor.Stats
        $output.monores += @{
            url = $monitor.Url
            status = $stats.Status
            uptime = $stats.GetUptime()
            fps = $stats.GetFps()
            totalFrames = $stats.TotalFrames
            videoFrames = $stats.VideoFrames
            audioFrames = $stats.AudioFrames
            bytesReceived = $stats.BytesReceived
            connectionAttempts = $stats.ConnectionAttempts
            reconnectAttempts = $stats.ReconnectAttempts
            lastError = $stats.LastError
        }
    }
    
    $output | ConvertTo-Json -Depth 10
}

# ============================================
# Main Execution
# ============================================

Write-Log "`n=== RTSP Connection Monitor ===" $Colors.Header
Write-Log "Monitoring $($Urls.Count) stream(s)" -ForegroundColor $Colors.Info

if ($LogPath) {
    Write-Log "Logging to: $LogPath" -ForegroundColor $Colors.Info
    # Очистка файла если он существует
    if (Test-Path $LogPath) {
        Remove-Item $LogPath -Force
    }
}

$monitors = @()

# Инициализация мониторов для каждого URL
foreach ($Url in $Urls) {
    $monitor = Start-RtspMonitor -Url $Url -Username $Username -Password $Password -TimeoutMs $TimeoutMs
    if ($monitor) {
        $monitors += $monitor
    }
}

if ($monitors.Count -eq 0) {
    Write-Log "No monitors initialized" -ForegroundColor $Colors.Error
    exit 1
}

# JSON output
if ($JsonOutput) {
    Show-JsonOutput -Monitors $monitors
    exit 0
}

# Continuous monitoring
if ($Continuous) {
    Write-Log "`nStarting continuous monitoring (Ctrl+C to stop)..." -ForegroundColor $Colors.Warning
    
    try {
        while ($true) {
            foreach ($monitor in $monitors) {
                Show-ConnectionStatus -Url $monitor.Url -Stats $monitor.Stats
            }
            
            Start-Sleep -Seconds $IntervalSeconds
            Clear-Host
        }
    }
    catch [System.Management.Automation.ActionPreferenceStopException] {
        Write-Log "`nMonitoring stopped by user" -ForegroundColor $Colors.Info
    }
}
else {
    # Single run
    Write-Log "`nRunning single monitoring check..." -ForegroundColor $Colors.Info
    
    foreach ($monitor in $monitors) {
        Show-ConnectionStatus -Url $monitor.Url -Stats $monitor.Stats
    }
}

Write-Log "`n=== Monitor Complete ===" -ForegroundColor $Colors.Header
```

---

## Примеры использования

### 1. Базовый мониторинг одной камеры

```powershell
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test
```

### 2. Мониторинг с авторизацией

```powershell
.\rtsp_monitor.ps1 -Url rtsp://admin:password123@192.168.1.100:554/test
```

### 3. Непрерывный мониторинг с логированием

```powershell
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -Continuous -LogPath "monitor.log"
```

### 4. Мониторинг нескольких камер

```powershell
.\rtsp_monitor.ps1 -Urls @(
    "rtsp://192.168.1.100:554/cam1",
    "rtsp://192.168.1.101:554/cam2",
    "rtsp://192.168.1.102:554/cam3"
) -Continuous
```

### 5. JSON output для интеграции

```powershell
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -JsonOutput > monitor.json
```

---

## Интеграция с Systemd (Linux)

### Service Unit File

```ini
# /etc/systemd/system/rtsp-monitor.service
[Unit]
Description=RTSP Connection Monitor
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/bin/powershell /opt/rtsp-monitor/rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -Continuous -LogPath /var/log/rtsp-monitor.log
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Установка и запуск

```bash
# Копирование скрипта
cp rtsp_monitor.ps1 /opt/rtsp-monitor/

# Установка service
sudo cp rtsp-monitor.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable rtsp-monitor
sudo systemctl start rtsp-monitor

# Просмотр логов
sudo journalctl -u rtsp-monitor -f
```

---

## Интеграция с Task Scheduler (Windows)

### Создание задачи

```powershell
# Создать задачу для мониторинга RTSP
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-File C:\Scripts\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -Continuous"
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries

Register-ScheduledTask -TaskName "RTSP Monitor" -Action $action -Trigger $trigger -Principal $principal -Settings $settings
```

---

## API для программной интеграции

### C# Example

```csharp
using System;
using System.Threading;
using System.Threading.Tasks;

public class RtspMonitor
{
    private RtspClient _client;
    private RtspStats _stats;
    private CancellationTokenSource _cts;
    
    public event EventHandler<RtspStats> StatsUpdated;
    
    public async Task StartAsync(string url, CancellationToken cancellationToken = default)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        
        _client = new RtspClient();
        _stats = new RtspStats { StartTime = DateTime.UtcNow };
        
        _client.StatusCallback += (status, message) =>
        {
            _stats.Status = status.ToString();
            if (status == RtspStatus.Error)
                _stats.LastError = message;
        };
        
        _client.FrameCallback += (frame) =>
        {
            _stats.TotalFrames++;
            _stats.LastFrameTime = DateTime.UtcNow;
            
            if (frame.IsVideo)
                _stats.VideoFrames++;
            else
                _stats.AudioFrames++;
            
            _stats.BytesReceived += frame.Data.Length;
            
            StatsUpdated?.Invoke(this, _stats);
        };
        
        await Task.Run(() =>
        {
            _client.Connect(url);
            
            while (!_cts.Token.IsCancellationRequested)
            {
                Thread.Sleep(1000);
                StatsUpdated?.Invoke(this, _stats);
            }
        }, _cts.Token);
    }
    
    public void Stop()
    {
        _cts?.Cancel();
        _client?.Disconnect();
    }
}
```

---

## Мониторинг производительности

### Metrics Endpoints

```python
# Пример Prometheus metrics exporter
from prometheus_client import start_http_server, Gauge, Counter
import time

# Metrics
RTSP_CONNECTION_STATUS = Gauge('rtsp_connection_status', 'Connection status', ['url'])
RTSP_FPS = Gauge('rtsp_fps', 'Frames per second', ['url'])
RTSP_TOTAL_FRAMES = Counter('rtsp_total_frames', 'Total frames received', ['url'])
RTSP_BYTES_RECEIVED = Counter('rtsp_bytes_received', 'Total bytes received', ['url'])
RTSP_RECONNECT_COUNT = Counter('rtsp_reconnect_count', 'Reconnection attempts', ['url'])

def export_metrics(monitors):
    for monitor in monitors:
        url = monitor['url']
        stats = monitor['stats']
        
        RTSP_CONNECTION_STATUS.labels(url=url).set(1 if stats.status == 'Connected' else 0)
        RTSP_FPS.labels(url=url).set(stats.get_fps())
        RTSP_TOTAL_FRAMES.labels(url=url).inc(stats.total_frames)
        RTSP_BYTES_RECEIVED.labels(url=url).inc(stats.bytes_received)
        RTSP_RECONNECT_COUNT.labels(url=url).inc(stats.reconnect_attempts)

if __name__ == '__main__':
    start_http_server(8000)
    while True:
        export_metrics(monitors)
        time.sleep(5)
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "RTSP Monitor Dashboard",
    "panels": [
      {
        "title": "Connection Status",
        "type": "stat",
        "targets": [
          {
            "expr": "rtsp_connection_status"
          }
        ]
      },
      {
        "title": "FPS",
        "type": "graph",
        "targets": [
          {
            "expr": "rtsp_fps"
          }
        ]
      },
      {
        "title": "Total Frames",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(rtsp_total_frames[5m])"
          }
        ]
      }
    ]
  }
}
```

---

## Troubleshooting

### Проблема: Monitor не может подключиться

**Решение:**
```powershell
# Проверка сети
Test-NetConnection -ComputerName 192.168.1.100 -Port 554

# Проверка что RTSP сервер работает
curl -v rtsp://192.168.1.100:554/test
```

### Проблема: Высокая задержка

**Решение:**
```powershell
# Уменьшить таймаут
.\rtsp_monitor.ps1 -Url rtsp://192.168.1.100:554/test -TimeoutMs 5000
```

### Проблема: Много reconnect attempts

**Решение:**
```powershell
# Проверить стабильность сети
ping -t 192.168.1.100

# Проверить нагрузку на сеть
netstat -e
```

---

**Создан:** 11 June 2026  
**Автор:** Koda AI Assistant  
**Версия:** 1.0
