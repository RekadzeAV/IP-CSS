# Quick install core build components via Chocolatey (Administrator required)

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Quick install OpenJDK 17, Node.js LTS, CMake, FFmpeg via Chocolatey (Administrator required)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\quick-install.ps1 -ShowHelp"
    Write-Host "  .\scripts\quick-install.ps1"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Finished (per-package choco errors are printed; process still exits 0)"
    Write-Host "  1  Not running as Administrator"
    exit 0
}

Write-Host "Quick install for IP-CSS build (Chocolatey)" -ForegroundColor Cyan
Write-Host ""

$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ERROR: Administrator rights are required." -ForegroundColor Red
    Write-Host "Run PowerShell as Administrator, then:" -ForegroundColor Yellow
    Write-Host "  .\scripts\quick-install.ps1" -ForegroundColor White
    exit 1
}

if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Installing Chocolatey..." -ForegroundColor Yellow
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    $machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $env:Path = $machinePath + ";" + $userPath
}

Write-Host "Installing packages via Chocolatey..." -ForegroundColor Cyan
Write-Host ""

$packages = @(
    @{ Name = "openjdk17"; Desc = "OpenJDK 17" },
    @{ Name = "nodejs-lts"; Desc = "Node.js LTS" },
    @{ Name = "cmake"; Desc = "CMake" },
    @{ Name = "ffmpeg"; Desc = "FFmpeg" }
)

foreach ($pkg in $packages) {
    Write-Host "Installing $($pkg.Desc)..." -ForegroundColor Yellow
    choco install $pkg.Name -y --no-progress
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] $($pkg.Desc) installed" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $($pkg.Desc) install error" -ForegroundColor Red
    }
    Write-Host ""
}

$machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
$userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
$env:Path = $machinePath + ";" + $userPath

Write-Host "Done." -ForegroundColor Green
Write-Host ""
Write-Host "Restart the terminal so PATH changes apply." -ForegroundColor Yellow
