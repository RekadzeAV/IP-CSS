# Быстрая установка основных компонентов через Chocolatey
# Требуются права администратора

Write-Host "Быстрая установка компонентов для сборки IP-CSS" -ForegroundColor Cyan
Write-Host ""

# Проверка прав администратора
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ОШИБКА: Для установки компонентов требуются права администратора" -ForegroundColor Red
    Write-Host "Пожалуйста, запустите PowerShell от имени администратора и повторите команду:" -ForegroundColor Yellow
    Write-Host "  .\scripts\quick-install.ps1" -ForegroundColor White
    exit 1
}

# Установка Chocolatey (если не установлен)
if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Установка Chocolatey..." -ForegroundColor Yellow
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
}

Write-Host "Установка компонентов через Chocolatey..." -ForegroundColor Cyan
Write-Host ""

# Список пакетов для установки
$packages = @(
    @{Name="openjdk17"; Desc="OpenJDK 17"},
    @{Name="nodejs-lts"; Desc="Node.js LTS"},
    @{Name="cmake"; Desc="CMake"},
    @{Name="ffmpeg"; Desc="FFmpeg"}
)

foreach ($pkg in $packages) {
    Write-Host "Установка $($pkg.Desc)..." -ForegroundColor Yellow
    choco install $pkg.Name -y --no-progress
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ $($pkg.Desc) установлен" -ForegroundColor Green
    } else {
        Write-Host "✗ Ошибка при установке $($pkg.Desc)" -ForegroundColor Red
    }
    Write-Host ""
}

# Обновление PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

Write-Host "Установка завершена!" -ForegroundColor Green
Write-Host ""
Write-Host "ВАЖНО: Перезапустите терминал для применения изменений PATH" -ForegroundColor Yellow


