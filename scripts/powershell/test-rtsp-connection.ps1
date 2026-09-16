# Скрипт для быстрого тестирования RTSP подключения
# Проверяет доступность камеры, кодеки и задержку

param(
    [string]$Url = "",
    [string]$Username = "",
    [string]$Password = "",
    [int]$Timeout = 10,
    [switch]$Verbose
)

# Цветовой вывод
function Write-Success {
    param([string]$Text)
    Write-Host "✅ $Text" -ForegroundColor Green
}

function Write-Error {
    param([string]$Text)
    Write-Host "❌ $Text" -ForegroundColor Red
}

function Write-Warning {
    param([string]$Text)
    Write-Host "⚠️  $Text" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Text)
    Write-Host "ℹ️  $Text" -ForegroundColor Gray
}

function Write-Step {
    param([string]$Text)
    Write-Host "🔹 $Text" -ForegroundColor Cyan
}

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "$Text" -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
}

# Парсинг RTSP URL
function Parse-RtspUrl {
    param([string]$Url)
    
    $pattern = "^rtsp://(?<user>[^:@]+)?:(?<pass>[^@]+)?@(?<host>[^:]+):(?<port>\d+)/(?<path>.+)$"
    
    if ($Url -match $pattern) {
        return @{
            Username = $Matches.User
            Password = $Matches.Pass
            Host = $Matches.Host
            Port = [int]$Matches.Port
            Path = $Matches.Path
        }
    }
    
    # Без аутентификации
    $pattern = "^rtsp://(?<host>[^:]+):(?<port>\d+)/(?<path>.+)$"
    
    if ($Url -match $pattern) {
        return @{
            Username = ""
            Password = ""
            Host = $Matches.Host
            Port = [int]$Matches.Port
            Path = $Matches.Path
        }
    }
    
    return $null
}

# Проверка наличия команды
function Test-Command {
    param([string]$Name)
    return (Get-Command $Name -ErrorAction SilentlyContinue) -ne $null
}

# Основной тест
Write-Header "RTSP Connection Tester"

# Проверка URL
if ([string]::IsNullOrEmpty($Url)) {
    Write-Info "Введите RTSP URL: " -NoNewline
    $Url = Read-Host
    
    if ([string]::IsNullOrEmpty($Url)) {
        Write-Error "RTSP URL не указан"
        exit 1
    }
}

# Парсинг URL
$parsedUrl = Parse-RtspUrl $Url
if (-not $parsedUrl) {
    Write-Error "Неверный формат RTSP URL"
    Write-Info "Пример: rtsp://admin:password@192.168.1.100:554/stream"
    exit 1
}

$host = $parsedUrl.Host
$port = $parsedUrl.Port
$path = $parsedUrl.Path
$username = if ([string]::IsNullOrEmpty($Username)) { $parsedUrl.Username } else { $Username }
$password = if ([string]::IsNullOrEmpty($Password)) { $parsedUrl.Password } else { $Password }

Write-Info "RTSP URL: $Url"
Write-Info "Хост: $host"
Write-Info "Порт: $port"
Write-Info "Путь: $path"
Write-Info "Пользователь: $username"

Write-Host ""

# 1. Проверка сетевой доступности
Write-Step "1. Проверка сетевой доступности..."

try {
    $pingResult = Test-Connection -ComputerName $host -Count 2 -Quiet -ErrorAction Stop
    
    if ($pingResult) {
        Write-Success "Хост доступен (ping OK)"
    } else {
        Write-Warning "Хост не отвечает на ping"
    }
} catch {
    Write-Warning "Не удалось выполнить ping: $_"
}

# Проверка порта
Write-Info "Проверка порта $port..."

try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $tcpClient.ConnectAsync($host, $port).Wait($Timeout * 1000) | Out-Null
    
    if ($tcpClient.Connected) {
        Write-Success "Порт $port открыт"
        $tcpClient.Close()
    } else {
        Write-Error "Порт $port закрыт или недоступен"
    }
} catch {
    Write-Error "Не удалось подключиться к порту $port: $_"
}

Write-Host ""

# 2. Проверка с ffprobe (если доступен)
Write-Step "2. Проверка кодеков (ffprobe)..."

if (Test-Command "ffprobe") {
    $authPart = if ([string]::IsNullOrEmpty($username)) { "" } else { "$username`:$password@" }
    $probeUrl = "rtsp://$authPart$host:$port/$path"
    
    Write-Info "Запуск ffprobe..."
    
    $probeArgs = @("-rtsp_transport", "tcp", "-timeout", "$($Timeout * 1000000)", "-i", $probeUrl, "-v", "error", "-show_entries", "stream=codec_name,codec_type,width,height,r_frame_rate", "-of", "default=noprint_wrappers=1")
    
    try {
        $probeOutput = & ffprobe $probeArgs 2>&1 | Out-String
        
        if ($LASTEXITCODE -eq 0 -and $probeOutput) {
            Write-Success "Поток доступен"
            Write-Host ""
            Write-Info "Информация о потоке:"
            
            $codecName = $probeOutput | Select-String "codec_name=" | ForEach-Object { $_.Line.Split('=')[1] }
            $codecType = $probeOutput | Select-String "codec_type=" | ForEach-Object { $_.Line.Split('=')[1] }
            $width = $probeOutput | Select-String "width=" | ForEach-Object { $_.Line.Split('=')[1] }
            $height = $probeOutput | Select-String "height=" | ForEach-Object { $_.Line.Split('=')[1] }
            $fps = $probeOutput | Select-String "r_frame_rate=" | ForEach-Object { $_.Line.Split('=')[1] }
            
            Write-Host "  Видеокодек: $codecName" -ForegroundColor Gray
            Write-Host "  Разрешение: ${width}x${height}" -ForegroundColor Gray
            Write-Host "  FPS: $fps" -ForegroundColor Gray
        } else {
            Write-Warning "ffprobe не смог получить информацию о потоке"
            Write-Info "Возможно, камера требует аутентификацию или использует неподдерживаемый кодек"
        }
    } catch {
        Write-Warning "Ошибка ffprobe: $_"
    }
} else {
    Write-Warning "ffprobe не найден. Установите FFmpeg для детальной проверки"
    Write-Info "Скачайте: https://ffmpeg.org/download.html"
}

Write-Host ""

# 3. Проверка с VLC (если установлен)
Write-Step "3. Проверка с VLC..."

$vlcPath = $null

# Поиск VLC
$vlcLocations = @(
    "C:\Program Files\VideoLAN\VLC\vlc.exe",
    "C:\Program Files (x86)\VideoLAN\VLC\vlc.exe",
    "${env:ProgramFiles}\VideoLAN\VLC\vlc.exe"
)

foreach ($path in $vlcLocations) {
    if (Test-Path $path) {
        $vlcPath = $path
        break
    }
}

if ($vlcPath) {
    Write-Success "VLC найден: $vlcPath"
    Write-Info "В VLC откройте: Медиа → Открыть URL"
    Write-Info "Вставьте: $Url"
} else {
    Write-Warning "VLC не найден"
    Write-Info "Скачайте: https://www.videolan.org/vlc/"
}

Write-Host ""

# 4. Проверка с ffplay (если доступен)
Write-Step "4. Быстрый просмотр (ffplay)..."

if (Test-Command "ffplay") {
    Write-Info "Запуск ffplay для просмотра потока..."
    Write-Info "Нажмите Q для выхода"
    
    $authPart = if ([string]::IsNullOrEmpty($username)) { "" } else { "$username`:$password@" }
    $playUrl = "rtsp://$authPart$host:$port/$path"
    
    ffplay -rtsp_transport tcp -fflags nobuffer -probesize 32 -i $playUrl
} else {
    Write-Warning "ffplay не найден"
}

Write-Host ""

# 5. Проверка задержки
Write-Step "5. Тест задержки..."

if (Test-Command "ffprobe") {
    $authPart = if ([string]::IsNullOrEmpty($username)) { "" } else { "$username`:$password@" }
    $probeUrl = "rtsp://$authPart$host:$port/$path"
    
    Write-Info "Измерение задержки (3 измерения)..."
    
    $latencies = @()
    
    for ($i = 1; $i -le 3; $i++) {
        Write-Info "Измерение $i/3..." -NoNewline
        
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $connectTask = $tcpClient.ConnectAsync($host, $port)
            
            if ($connectTask.Wait(5000)) {
                $tcpClient.Close()
                $stopwatch.Stop()
                $latencies += $stopwatch.ElapsedMilliseconds
                Write-Success "$($stopwatch.ElapsedMilliseconds) ms"
            } else {
                Write-Error "Таймаут"
            }
        } catch {
            Write-Error "Ошибка"
        }
    }
    
    if ($latencies.Count -gt 0) {
        $avgLatency = ($latencies | Measure-Object -Average).Average
        Write-Host ""
        Write-Success "Средняя задержка: $([math]::Round($avgLatency, 2)) ms"
        
        if ($avgLatency -lt 100) {
            Write-Success "Отличная задержка (< 100ms)"
        } elseif ($avgLatency -lt 300) {
            Write-Success "Хорошая задержка (< 300ms)"
        } elseif ($avgLatency -lt 1000) {
            Write-Warning "Высокая задержка (> 1000ms)"
        } else {
            Write-Error "Критическая задержка (> 1000ms)"
        }
    }
}

Write-Host ""
Write-Header "Результаты"

# Итоговая оценка
$score = 0

if ($pingResult) { $score++ }
if ($tcpClient.Connected) { $score++ }

Write-Info "Сетевая доступность: $score/2"

if ($score -eq 2) {
    Write-Success "RTSP камера доступна и готова к использованию!"
} else {
    Write-Warning "Есть проблемы с подключением. Проверьте:"
    Write-Warning "  - Камера включена и подключена к сети"
    Write-Warning "  - Правильность IP-адреса"
    Write-Warning "  - Порт 554 открыт в брандмауэре"
    Write-Warning "  - Правильность username/password"
}

Write-Host ""
Write-Info "Для детальной проверки используйте:"
Write-Info "  ffprobe -rtsp_transport tcp -i rtsp://admin:pass@ip:554/stream"
Write-Info ""
