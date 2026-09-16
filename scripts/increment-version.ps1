# Скрипт автоматического инкремента версии продукта IP-CSS
# Версия: 1.0
# Дата создания: Январь 2026
#
# Использование:
#   .\scripts\increment-version.ps1          # Инкремент версии на 0.0.1
#   .\scripts\increment-version.ps1 -DryRun  # Показать новую версию без изменения

param(
    [Parameter(Mandatory=$false)]
    [switch]$DryRun = $false,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Bump Alfa-x.y.z patch in gradle.properties (optional doc headers); use -DryRun to preview only."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\increment-version.ps1 -ShowHelp"
    Write-Host "  .\scripts\increment-version.ps1 [-DryRun]"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Success or dry-run completed"
    Write-Host "  1  gradle.properties missing or version parse failed"
    exit 0
}

# Цвета для вывода
$ErrorColor = "Red"
$SuccessColor = "Green"
$InfoColor = "Cyan"
$WarningColor = "Yellow"

function Write-Info {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $InfoColor
}

function Write-Success {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $SuccessColor
}

function Write-Error {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $ErrorColor
}

function Write-Warning {
    param([string]$Message)
    Write-Host $Message -ForegroundColor $WarningColor
}

# Получение версии проекта из gradle.properties
function Get-CurrentVersion {
    $gradleProps = Join-Path $PSScriptRoot "..\gradle.properties"
    if (Test-Path $gradleProps) {
        $content = Get-Content $gradleProps -Raw
        if ($content -match 'version=(.+)') {
            return $matches[1].Trim()
        }
    }
    return "Alfa-0.0.1"
}

# Инкремент версии проекта (увеличивает последнюю цифру на 1)
function Increment-Version {
    param([string]$Version)

    if ($Version -match '^Alfa-(\d+)\.(\d+)\.(\d+)$') {
        $major = [int]$matches[1]
        $minor = [int]$matches[2]
        $patch = [int]$matches[3]

        # Инкрементируем последнюю цифру (patch) на 1
        $patch++

        return "Alfa-$major.$minor.$patch"
    }

    Write-Error "Не удалось распарсить версию: $Version"
    exit 1
}

# Обновление версии в gradle.properties
function Update-VersionInGradleProps {
    param(
        [string]$NewVersion,
        [bool]$IsDryRun
    )

    $gradleProps = Join-Path $PSScriptRoot "..\gradle.properties"

    if ($IsDryRun) {
        Write-Info "DRY RUN: Версия будет изменена на: $NewVersion"
        return
    }

    if (-not (Test-Path $gradleProps)) {
        Write-Error "Файл gradle.properties не найден: $gradleProps"
        exit 1
    }

    # Обновляем версию в gradle.properties
    $content = Get-Content $gradleProps -Raw
    $content = $content -replace '^version=.*', "version=$NewVersion"
    Set-Content -Path $gradleProps -Value $content -NoNewline

    Write-Success "[OK] Version updated in gradle.properties: $NewVersion"
}

# Обновление версии в документах (опционально)
function Update-VersionInDocuments {
    param(
        [string]$NewVersion,
        [bool]$IsDryRun
    )

    if ($IsDryRun) {
        Write-Info "DRY RUN: Версия в документах будет обновлена на: $NewVersion"
        return
    }

    $projectRoot = Split-Path $PSScriptRoot -Parent
    $docs = @(
        Join-Path $projectRoot "README.md",
        Join-Path $projectRoot "DOCUMENTATION_INDEX.md"
    )

    # Обновляем версию в основных документах
    foreach ($doc in $docs) {
        if (Test-Path $doc) {
            $content = Get-Content $doc -Raw
            $content = $content -replace '\*\*Версия проекта:\*\* Alfa-\d+\.\d+\.\d+', "**Версия проекта:** $NewVersion"
            Set-Content -Path $doc -Value $content -NoNewline
        }
    }

    Write-Info "[OK] Version updated in main documents"
}

# Главная функция
function Main {
    $gradleProps = Join-Path $PSScriptRoot "..\gradle.properties"

    if (-not (Test-Path $gradleProps)) {
        Write-Error "Файл gradle.properties не найден: $gradleProps"
        exit 1
    }

    Write-Info "=========================================="
    Write-Info "Автоматическое обновление версии продукта"
    Write-Info "=========================================="

    $currentVersion = Get-CurrentVersion
    Write-Info "Текущая версия: $currentVersion"

    $newVersion = Increment-Version -Version $currentVersion
    Write-Info "Новая версия: $newVersion"

    if ($DryRun) {
        Write-Warning "РЕЖИМ ПРОВЕРКИ: изменения не будут применены"
    }

    Update-VersionInGradleProps -NewVersion $newVersion -IsDryRun $DryRun
    Update-VersionInDocuments -NewVersion $newVersion -IsDryRun $DryRun

    if (-not $DryRun) {
        Write-Success "=========================================="
        Write-Success "Версия успешно обновлена!"
        Write-Success "Текущая версия: $currentVersion → $newVersion"
        Write-Success "=========================================="
    } else {
        Write-Info "=========================================="
        Write-Info "Проверка завершена. Для применения изменений запустите без -DryRun"
        Write-Info "=========================================="
    }
}

# Запуск
Main
