# Мониторинг ресурсов NAS во время field validation
# Usage: .\scripts\nas-resource-monitor.ps1 -Host 192.168.1.100 -Duration 24 -OutputPath "docs/reports/nas-validation"

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$Host,
    
    [Parameter(Mandatory=$false)]
    [string]$Username = "admin",
    
    [Parameter(Mandatory=$false)]
    [int]$Port = 22,
    
    [Parameter(Mandatory=$false)]
    [string]$SSHKeyPath = "$env:USERPROFILE\.ssh\id_rsa",
    
    [Parameter(Mandatory=$false)]
    [int]$Duration = 24,  # часы
    
    [Parameter(Mandatory=$false)]
    [int]$Interval = 60,  # секунд
    
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = "docs/reports/nas-validation",
    
    [Parameter(Mandatory=$false)]
    [string]$Platform = "Synology",
    
    [Parameter(Mandatory=$false)]
    [switch]$UsePasswordAuth,
    
    [Parameter(Mandatory=$false)]
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"

# Создаем директорию для логов
New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = "$OutputPath\monitor-$Host-$timestamp.csv"
$errorLog = "$OutputPath\errors-$Host-$timestamp.log"

Write-Host "Запускаем мониторинг ресурсов для $Host" -ForegroundColor Cyan
Write-Host "Платформа: $Platform" -ForegroundColor Cyan
Write-Host "Длительность: $Duration часов" -ForegroundColor Cyan
Write-Host "Интервал: $Interval секунд" -ForegroundColor Cyan
Write-Host "Лог: $logFile" -ForegroundColor Cyan

# Инициализация CSV файла
@{
    Timestamp = "ISO8601"
    Host = $Host
    CPU_User = "%"
    CPU_System = "%"
    CPU_Idle = "%"
    Memory_Used_MB = "MB"
    Memory_Total_MB = "MB"
    Memory_Percent = "%"
    Disk_Used_GB = "GB"
    Disk_Total_GB = "GB"
    Disk_Percent = "%"
    Load_1m = "load"
    Load_5m = "load"
    Load_15m = "load"
    Network_Rx_MB = "MB"
    Network_Tx_MB = "MB"
    Process_Count = "count"
    Recording_Processes = "count"
    Error_Count = "count"
} | ConvertTo-Csv -NoTypeInformation | Out-File -FilePath $logFile -Encoding UTF8

$startTime = Get-Date
$endTime = $startTime.AddHours($Duration)
$iteration = 0
$errorCount = 0

# Функция для выполнения SSH команды
function Invoke-SSHCommand {
    param(
        [string]$Command,
        [string]$Host,
        [string]$Username,
        [int]$Port,
        [string]$SSHKeyPath,
        [bool]$UsePassword,
        [string]$Password
    )
    
    try {
        if ($UsePassword) {
            # Использование пароля (менее безопасно)
            $securePassword = ConvertTo-SecureString $Password -AsPlainText -Force
            $credential = New-Object System.Management.Automation.PSCredential($Username, $securePassword)
            
            # Примечание: для SSH с паролем может потребоваться plink или pssh
            # Это упрощенная реализация
            throw "Password auth not fully supported - используйте SSH keys"
        }
        else {
            # Использование SSH ключей
            $sshCommand = "ssh -i `"$SSHKeyPath`" -p $Port -o StrictHostKeyChecking=no -o ConnectTimeout=10 ${Username}@${Host} `"$Command`""
            
            $process = Start-Process -FilePath "ssh" -ArgumentList "-i", "`"$SSHKeyPath`"", "-p", $Port, "-o", "StrictHostKeyChecking=no", "-o", "ConnectTimeout=10", "${Username}@${Host}", $Command `
                -Wait -PassThru -RedirectStandardOutput "$OutputPath\temp-output.txt" -RedirectStandardError "$OutputPath\temp-error.txt"
            
            $output = Get-Content "$OutputPath\temp-output.txt" -Raw
            $errors = Get-Content "$OutputPath\temp-error.txt" -Raw
            
            Delete-Item "$OutputPath\temp-output.txt" -ErrorAction SilentlyContinue
            Delete-Item "$OutputPath\temp-error.txt" -ErrorAction SilentlyContinue
            
            if ($process.ExitCode -eq 0) {
                return @{Success = $true; Output = $output; ExitCode = $process.ExitCode}
            }
            else {
                return @{Success = $false; Output = $output; Errors = $errors; ExitCode = $process.ExitCode}
            }
        }
    }
    catch {
        return @{Success = $false; Error = $_.Exception.Message}
    }
}

# Функция для сбора метрик CPU
function Get-CpuMetrics {
    param([string]$Platform)
    
    switch ($Platform) {
        "Synology" {
            $result = Invoke-SSHCommand -Command "top -bn1 | grep 'Cpu(s)'" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
            if ($result.Success) {
                $cpuLine = $result.Output | Select-String "Cpu(s)"
                # Парсим: 12.3%us, 5.2%sy, 80.5%id
                $user = [double]($cpuLine -replace '.*?(\d+\.?\d*)%us.*', '$1')
                $system = [double]($cpuLine -replace '.*?(\d+\.?\d*)%sy.*', '$1')
                $idle = [double]($cpuLine -replace '.*?(\d+\.?\d*)%id.*', '$1')
                return @{User = $user; System = $system; Idle = $idle}
            }
        }
        "QNAP" {
            $result = Invoke-SSHCommand -Command "cat /proc/stat | grep 'cpu '" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
            if ($result.Success) {
                $cpuStats = $result.Output -split '\s+'
                $user = [int]$cpuStats[1]
                $nice = [int]$cpuStats[2]
                $system = [int]$cpuStats[3]
                $idle = [int]$cpuStats[4]
                $total = $user + $nice + $system + $idle
                return @{
                    User = ($user / $total) * 100
                    System = ($system / $total) * 100
                    Idle = ($idle / $total) * 100
                }
            }
        }
        default {
            # Generic Linux
            $result = Invoke-SSHCommand -Command "top -bn1 | grep 'Cpu(s)'" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
            if ($result.Success) {
                # Fallback parsing
                return @{User = 0; System = 0; Idle = 100}
            }
        }
    }
    
    return @{User = 0; System = 0; Idle = 100}
}

# Функция для сбора метрик памяти
function Get-MemoryMetrics {
    param()
    
    $result = Invoke-SSHCommand -Command "free -m | grep 'Mem:'" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
    
    if ($result.Success) {
        $memLine = $result.Output -split '\s+'
        $total = [int]$memLine[1]
        $used = [int]$memLine[2]
        $percent = ($used / $total) * 100
        return @{Used_MB = $used; Total_MB = $total; Percent = $percent}
    }
    
    return @{Used_MB = 0; Total_MB = 0; Percent = 0}
}

# Функция для сбора метрик диска
function Get-DiskMetrics {
    param()
    
    $result = Invoke-SSHCommand -Command "df -h / | tail -1" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
    
    if ($result.Success) {
        $diskLine = $result.Output -split '\s+'
        $total = $diskLine[1]
        $used = $diskLine[2]
        $percent = $diskLine[4] -replace '%', ''
        
        # Конвертируем в GB
        $totalGB = ConvertTo-GB $total
        $usedGB = ConvertTo-GB $used
        
        return @{Used_GB = $usedGB; Total_GB = $totalGB; Percent = [int]$percent}
    }
    
    return @{Used_GB = 0; Total_GB = 0; Percent = 0}
}

# Функция конвертации в GB
function ConvertTo-GB {
    param([string]$Value)
    
    if ($Value -match '(\d+\.?\d*)([KMGT]?)B?') {
        $number = [double]$matches[1]
        $unit = $matches[2]
        
        switch ($unit) {
            'K' { return $number / 1024 / 1024 }
            'M' { return $number / 1024 }
            'G' { return $number }
            'T' { return $number * 1024 }
            default { return $number / 1024 / 1024 }  # bytes to GB
        }
    }
    
    return 0
}

# Функция для сбора метрик нагрузки
function Get-LoadAverage {
    param()
    
    $result = Invoke-SSHCommand -Command "uptime | awk -F'load average:' '{print \$2}'" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
    
    if ($result.Success) {
        $load = $result.Output -split ','
        return @{
            Load_1m = [double]($load[0] -replace '^\s+', '')
            Load_5m = [double]($load[1] -replace '^\s+', '')
            Load_15m = [double]($load[2] -replace '^\s+', '')
        }
    }
    
    return @{Load_1m = 0; Load_5m = 0; Load_15m = 0}
}

# Функция для сбора сетевых метрик
function Get-NetworkMetrics {
    param()
    
    $result = Invoke-SSHCommand -Command "ip -b -s link show eth0 | awk '/RX:/ {rx=$2} /TX:/ {tx=$2} END {print rx, tx}'" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
    
    if ($result.Success) {
        $bytes = $result.Output -split '\s+'
        $rxMB = [int]$bytes[0] / 1024 / 1024
        $txMB = [int]$bytes[1] / 1024 / 1024
        return @{Rx_MB = $rxMB; Tx_MB = $txMB}
    }
    
    return @{Rx_MB = 0; Tx_MB = 0}
}

# Функция для подсчета процессов
function Get-ProcessMetrics {
    param()
    
    $result = Invoke-SSHCommand -Command "ps aux | wc -l" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
    
    $processCount = 0
    $recordingProcesses = 0
    
    if ($result.Success) {
        $processCount = [int]$result.Output.Trim()
        
        # Считаем процессы записи
        $recResult = Invoke-SSHCommand -Command "ps aux | grep -E 'ffmpeg|recording|ipcamera' | grep -v grep | wc -l" -Host $Host -Username $Username -Port $Port -SSHKeyPath $SSHKeyPath -UsePassword $UsePasswordAuth -Password $Password
        if ($recResult.Success) {
            $recordingProcesses = [int]$recResult.Output.Trim()
        }
    }
    
    return @{Process_Count = $processCount; Recording_Processes = $recordingProcesses}
}

Write-Host "`nНачинаем сбор метрик..." -ForegroundColor Green

try {
    while ((Get-Date) -lt $endTime) {
        $iteration++
        $currentTimestamp = Get-Date
        
        Write-Host "[$($currentTimestamp.ToString('HH:mm:ss'))] Итерация $iteration / $(($endTime - $startTime).TotalHours -as [int])" -NoNewline
        
        try {
            # Собираем все метрики
            $cpu = Get-CpuMetrics -Platform $Platform
            $memory = Get-MemoryMetrics
            $disk = Get-DiskMetrics
            $load = Get-LoadAverage
            $network = Get-NetworkMetrics
            $processes = Get-ProcessMetrics
            
            # Записываем в CSV
            $rowData = @{
                Timestamp = $currentTimestamp.ToString('o')
                Host = $Host
                CPU_User = [math]::Round($cpu.User, 2)
                CPU_System = [math]::Round($cpu.System, 2)
                CPU_Idle = [math]::Round($cpu.Idle, 2)
                Memory_Used_MB = $memory.Used_MB
                Memory_Total_MB = $memory.Total_MB
                Memory_Percent = [math]::Round($memory.Percent, 2)
                Disk_Used_GB = [math]::Round($disk.Used_GB, 2)
                Disk_Total_GB = [math]::Round($disk.Total_GB, 2)
                Disk_Percent = $disk.Percent
                Load_1m = $load.Load_1m
                Load_5m = $load.Load_5m
                Load_15m = $load.Load_15m
                Network_Rx_MB = [math]::Round($network.Rx_MB, 2)
                Network_Tx_MB = [math]::Round($network.Tx_MB, 2)
                Process_Count = $processes.Process_Count
                Recording_Processes = $processes.Recording_Processes
                Error_Count = $errorCount
            }
            
            $rowData | ConvertTo-Csv -NoTypeInformation | Select-Object -Skip 1 | Out-File -FilePath $logFile -Append -Encoding UTF8
            
            # Проверка порогов
            if ($memory.Percent -gt 85 -or $cpu.User -gt 85) {
                Write-Host " ⚠️ КРИТИЧНО!" -ForegroundColor Red
                "$currentTimestamp - CRITICAL: Memory $($memory.Percent)% or CPU $($cpu.User)%" | Out-File -FilePath $errorLog -Append
            }
            elseif ($memory.Percent -gt 70 -or $cpu.User -gt 70) {
                Write-Host " ⚠️ WARNING" -ForegroundColor Yellow
                "$currentTimestamp - WARNING: Memory $($memory.Percent)% or CPU $($cpu.User)%" | Out-File -FilePath $errorLog -Append
            }
            else {
                Write-Host " ✓ OK (CPU: $($cpu.User)%, Mem: $($memory.Percent)%)" -ForegroundColor Green
            }
        }
        catch {
            $errorCount++
            Write-Host " ✗ ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red
            "$currentTimestamp - ERROR: $($_.Exception.Message)" | Out-File -FilePath $errorLog -Append
        }
        
        # Ждем до следующей итерации
        if ((Get-Date) -lt $endTime) {
            Start-Sleep -Seconds $Interval
        }
    }
}
catch {
    Write-Host "Критическая ошибка мониторинга: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$endTimeActual = Get-Date
$duration = $endTimeActual - $startTime

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Мониторинг завершен!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Общая длительность: $duration" -ForegroundColor Cyan
Write-Host "Итераций: $iteration" -ForegroundColor Cyan
Write-Host "Ошибок: $errorCount" -ForegroundColor $(if ($errorCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "Лог метрик: $logFile" -ForegroundColor Cyan
Write-Host "Лог ошибок: $errorLog" -ForegroundColor Cyan

# Генерируем сводку
$snapshotMetrics = @{
    Host = $Host
    Platform = $Platform
    StartTime = $startTime
    EndTime = $endTimeActual
    Duration = $duration
    TotalIterations = $iteration
    TotalErrors = $errorCount
    LogFile = $logFile
    ErrorLogFile = $errorLog
}

$snapshotMetrics | ConvertTo-Json | Out-File -FilePath "$OutputPath\snapshot-$Host-$timestamp.json" -Encoding UTF8

Write-Host "`nСнимок сохранен: $($snapshotMetrics | ConvertTo-Json)" -ForegroundColor Cyan
