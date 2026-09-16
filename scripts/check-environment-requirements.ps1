# Check Environment Requirements (PowerShell)
# Проверяет наличие всех необходимых компонентов, библиотек и инструментов
# для сборки и тестирования проекта IP-CSS

param(
    [switch]$Verbose,
    [switch]$Quick,
    [string]$Report
)

# Цвета
$Colors = @{
    OK = '[32m'
    WARN = '[33m'
    FAIL = '[31m'
    END = '[0m'
    BOLD = '[1m'
}

function Write-Color($Color, $Message) {
    Write-Host "$($Colors[$Color])$Message$($Colors.END)"
}

function Write-Bold($Message) {
    Write-Host "$($Colors.BOLD)$Message$($Colors.END)"
}

function Test-CommandExists($Command) {
    return $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

function Get-CommandVersion($Command, $VersionArgs) {
    try {
        $result = & $Command $VersionArgs 2>&1
        return $result[0]
    } catch {
        return "unknown"
    }
}

$Checks = @()
$Errors = @()
$Warnings = @()

Write-Bold "Проверка окружения проекта IP-CSS"
Write-Host ("=" * 60)
Write-Host ""

# Python
Write-Bold "Python и скрипты"
if (Test-CommandExists "python") {
    $version = Get-CommandVersion "python" @("--version")
    $Checks += [PSCustomObject]@{
        Name = "Python"
        Required = $true
        Installed = $true
        Version = $version
        Message = "✅ Python установлен: $version"
    }
} else {
    $Checks += [PSCustomObject]@{
        Name = "Python"
        Required = $true
        Installed = $false
        Version = $null
        Message = "❌ Python не найден"
    }
    $Errors += "Python требуется для скриптов CI"
}

if (Test-CommandExists "pip") {
    $version = Get-CommandVersion "pip" @("--version")
    $Checks += [PSCustomObject]@{
        Name = "pip"
        Required = $true
        Installed = $true
        Version = $version
        Message = "✅ pip установлен: $version"
    }
} else {
    $Checks += [PSCustomObject]@{
        Name = "pip"
        Required = $true
        Installed = $false
        Version = $null
        Message = "❌ pip не найден"
    }
    $Errors += "pip требуется для установки Python зависимостей"
}

# Java
Write-Bold "`nJava и Gradle"
if (Test-CommandExists "java") {
    $version = Get-CommandVersion "java" @("-version")
    $Checks += [PSCustomObject]@{
        Name = "Java"
        Required = $true
        Installed = $true
        Version = $version
        Message = "✅ Java установлен: $version"
    }
    
    # Проверка версии
    if ($version -match "version ""(\d+)") {
        $majorVersion = [int]$Matches[1]
        if ($majorVersion -lt 17) {
            $Checks[-1].Message = "⚠️ Java версия $majorVersion слишком старая (требуется 17+)"
            $Warnings += "Java версия $majorVersion ниже требуемой 17+"
        }
    }
} else {
    $Checks += [PSCustomObject]@{
        Name = "Java"
        Required = $true
        Installed = $false
        Version = $null
        Message = "❌ Java не найден"
    }
    $Errors += "Java (JDK 17+) требуется для сборки проекта"
}

# Gradle Wrapper
if (Test-Path "gradlew.bat") {
    try {
        $version = & .\gradlew.bat --version 2>&1 | Select-Object -First 1
        $Checks += [PSCustomObject]@{
            Name = "Gradle Wrapper"
            Required = $true
            Installed = $true
            Version = $version
            Message = "✅ Gradle Wrapper найден: $version"
        }
    } catch {
        $Checks += [PSCustomObject]@{
            Name = "Gradle Wrapper"
            Required = $true
            Installed = $false
            Version = $null
            Message = "❌ Gradle Wrapper не работает"
        }
        $Errors += "Gradle Wrapper не работает"
    }
} else {
    $Checks += [PSCustomObject]@{
        Name = "Gradle Wrapper"
        Required = $true
        Installed = $false
        Version = $null
        Message = "❌ Gradle Wrapper не найден (файл gradlew.bat отсутствует)"
    }
    $Errors += "Gradle Wrapper не найден"
}

# Node.js (опционально)
if (-not $Quick) {
    Write-Bold "`nNode.js и npm"
    if (Test-CommandExists "node") {
        $version = Get-CommandVersion "node" @("--version")
        $Checks += [PSCustomObject]@{
            Name = "Node.js"
            Required = $true
            Installed = $true
            Version = $version
            Message = "✅ Node.js установлен: $version"
        }
    } else {
        $Checks += [PSCustomObject]@{
            Name = "Node.js"
            Required = $true
            Installed = $false
            Version = $null
            Message = "❌ Node.js не найден"
        }
        $Warnings += "Node.js требуется для веб-модулей"
    }

    if (Test-CommandExists "npm") {
        $version = Get-CommandVersion "npm" @("--version")
        $Checks += [PSCustomObject]@{
            Name = "npm"
            Required = $true
            Installed = $true
            Version = $version
            Message = "✅ npm установлен: $version"
        }
    } else {
        $Checks += [PSCustomObject]@{
            Name = "npm"
            Required = $true
            Installed = $false
            Version = $null
            Message = "❌ npm не найден"
        }
        $Warnings += "npm требуется для веб-модулей"
    }
}

# Docker (опционально)
if (-not $Quick) {
    Write-Bold "`nDocker"
    if (Test-CommandExists "docker") {
        $version = Get-CommandVersion "docker" @("--version")
        $Checks += [PSCustomObject]@{
            Name = "Docker"
            Required = $false
            Installed = $true
            Version = $version
            Message = "✅ Docker установлен: $version"
        }
    } else {
        $Checks += [PSCustomObject]@{
            Name = "Docker"
            Required = $false
            Installed = $false
            Version = $null
            Message = "⚠️ Docker не найден (опционально)"
        }
        $Warnings += "Docker требуется для интеграционных тестов"
    }
}

# Системные ресурсы
Write-Bold "`nСистемные ресурсы"

# RAM
$ramInfo = Get-CimInstance Win32_OperatingSystem
$totalRamGB = [math]::Round($ramInfo.TotalVisibleMemorySize / 1MB, 1)
if ($totalRamGB -ge 8) {
    $Checks += [PSCustomObject]@{
        Name = "RAM"
        Required = $true
        Installed = $true
        Version = "$totalRamGB GB"
        Message = "✅ RAM: $totalRamGB GB (достаточно)"
    }
} else {
    $Checks += [PSCustomObject]@{
        Name = "RAM"
        Required = $true
        Installed = $true
        Version = "$totalRamGB GB"
        Message = "⚠️ RAM: $totalRamGB GB (рекомендуется 8GB+)"
    }
    $Warnings += "Мало RAM: $totalRamGB GB (рекомендуется 8GB+)"
}

# Disk Space
$drive = Get-PSDrive C
$freeSpaceGB = [math]::Round($drive.Free / 1GB, 1)
if ($freeSpaceGB -ge 50) {
    $Checks += [PSCustomObject]@{
        Name = "Disk Space"
        Required = $true
        Installed = $true
        Version = "$freeSpaceGB GB"
        Message = "✅ Disk Space: $freeSpaceGB GB свободно (достаточно)"
    }
} else {
    $Checks += [PSCustomObject]@{
        Name = "Disk Space"
        Required = $true
        Installed = $true
        Version = "$freeSpaceGB GB"
        Message = "⚠️ Disk Space: $freeSpaceGB GB свободно (рекомендуется 50GB+)"
    }
    $Warnings += "Мало свободного места: $freeSpaceGB GB (рекомендуется 50GB+)"
}

# Вывод результатов
Write-Host ""
Write-Bold "Результаты:"
Write-Host ("-" * 60)

foreach ($check in $Checks) {
    Write-Host $check.Message
    if ($Verbose -and $check.Path) {
        Write-Host "  Путь: $($check.Path)"
    }
}

Write-Host ""
Write-Host ("=" * 60)
Write-Host "Всего проверок: $($Checks.Count)"
Write-Host "Пройдено: $(($Checks | Where-Object { $_.Installed }).Count)"
Write-Host "Не пройдено: $(($Checks | Where-Object { -not $_.Installed }).Count)"

$requiredChecks = $Checks | Where-Object { $_.Required }
$requiredPassed = ($requiredChecks | Where-Object { $_.Installed }).Count
Write-Host "Обязательные: $requiredPassed/$($requiredChecks.Count)"

if ($Errors.Count -gt 0) {
    Write-Host ""
    Write-Color "FAIL" "Ошибки:"
    foreach ($error in $Errors) {
        Write-Host "  $error"
    }
}

if ($Warnings.Count -gt 0) {
    Write-Host ""
    Write-Color "WARN" "Предупреждения:"
    foreach ($warning in $Warnings) {
        Write-Host "  $warning"
    }
}

Write-Host ""

$allRequiredPassed = ($requiredChecks | Where-Object { -not $_.Installed }).Count -eq 0
if ($allRequiredPassed) {
    Write-Color "OK" "✅ Все обязательные проверки пройдены!"
} else {
    Write-Color "FAIL" "❌ Некоторые обязательные проверки не пройдены!"
}

# Сохранение отчёта
if ($Report) {
    $reportDir = Split-Path $Report -Parent
    if ($reportDir -and -not (Test-Path $reportDir)) {
        New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
    }
    
    $reportData = [PSCustomObject]@{
        timestamp = Get-Date -Format "o"
        platform = $env:COMPUTERNAME
        checks = $Checks
        summary = [PSCustomObject]@{
            total_checks = $Checks.Count
            passed = ($Checks | Where-Object { $_.Installed }).Count
            failed = ($Checks | Where-Object { -not $_.Installed }).Count
            required_passed = $requiredPassed
            required_total = $requiredChecks.Count
            all_required_passed = $allRequiredPassed
        }
        errors = $Errors
        warnings = $Warnings
    }
    
    $reportData | ConvertTo-Json -Depth 10 | Out-File -FilePath $Report -Encoding UTF8
    Write-Host ""
    Write-Host "Отчёт сохранён: $Report"
}

# Возврат кода
if ($allRequiredPassed) {
    exit 0
} else {
    exit 1
}
