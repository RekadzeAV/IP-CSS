# Smoke checks for NAS package structure (PowerShell)
# Usage: .\scripts\test-nas-build.ps1 -PackageType synology -Arch x86_64

param(
    [string]$PackageType = "synology",
    [string]$Arch = "x86_64",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate NAS package structure for a target package/arch."
    Write-Host "Usage: .\scripts\test-nas-build.ps1 [-PackageType synology|qnap|asustor] [-Arch x86_64|arm64|armv7|rtd1296]"
    exit 0
}

$projectRoot = Split-Path -Parent $PSScriptRoot

if ($Arch -in @("x86_64", "amd64")) {
    $platformDir = Join-Path $projectRoot "platforms\nas-x86_64"
} elseif ($Arch -in @("arm64", "aarch64", "armv7", "arm", "rtd1296", "rtd")) {
    $platformDir = Join-Path $projectRoot "platforms\nas-arm"
} else {
    throw "Unsupported architecture: $Arch"
}

$packageDir = Join-Path $platformDir "packages\$PackageType"
if (-not (Test-Path $packageDir)) {
    throw "Package directory not found: $packageDir"
}

switch ($PackageType) {
    "synology" {
        if (-not (Test-Path (Join-Path $packageDir "INFO"))) { throw "Missing INFO" }
        foreach ($script in @("preinst", "postinst", "preuninst", "postuninst")) {
            if (-not (Test-Path (Join-Path $packageDir "scripts\$script"))) {
                throw "Missing scripts/$script"
            }
        }
    }
    "qnap" {
        if (-not (Test-Path (Join-Path $packageDir "QPKG.INFO"))) { throw "Missing QPKG.INFO" }
        if (-not (Test-Path (Join-Path $packageDir "scripts\start.sh"))) { throw "Missing scripts/start.sh" }
    }
    "asustor" {
        if (-not (Test-Path (Join-Path $packageDir "INFO"))) { throw "Missing INFO" }
        foreach ($script in @("preinst", "postinst", "preuninst", "postuninst")) {
            if (-not (Test-Path (Join-Path $packageDir "scripts\$script"))) {
                throw "Missing scripts/$script"
            }
        }
    }
    default {
        throw "Unsupported package type: $PackageType"
    }
}

$gradlew = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found"
}

Write-Host "Smoke check passed for $PackageType/$Arch" -ForegroundColor Green
