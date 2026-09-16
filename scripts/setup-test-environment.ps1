# Load VideoDecoder test env vars into the current session (dot-source), from config\test-cameras.local.json or .test-env.
# Typical usage: . .\scripts\setup-test-environment.ps1
# Help (run with -File so exit does not affect a dot-sourced parent): powershell -File .\scripts\setup-test-environment.ps1 -ShowHelp

param(
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Dot-source this script to set process env vars for VideoDecoder tests (local JSON or .test-env)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  . .\scripts\setup-test-environment.ps1"
    Write-Host "  powershell -NoProfile -File .\scripts\setup-test-environment.ps1 -ShowHelp"
    Write-Host ""
    Write-Host "Priority:"
    Write-Host "  1) config\test-cameras.local.json (first camera + defaults)"
    Write-Host "  2) Existing .test-env in repo root"
    Write-Host "  3) Create .test-env template if none"
    Write-Host ""
    Write-Host "Exit codes (only when run with -File, not when dot-sourced):"
    Write-Host "  0  This help printed"
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $ProjectRoot ".test-env"
$LocalCameraConfig = Join-Path $ProjectRoot "config\test-cameras.local.json"

Write-Host "=== VideoDecoder Test Environment Setup ===" -ForegroundColor Cyan
Write-Host ""

# 1) Приоритет: локальный JSON-конфиг камер
if (Test-Path $LocalCameraConfig) {
    Write-Host "Found local camera config. Loading..." -ForegroundColor Green
    try {
        $cfg = Get-Content $LocalCameraConfig -Raw | ConvertFrom-Json
        $first = $cfg.cameras | Select-Object -First 1
        if (-not $first) {
            throw "No cameras in config"
        }

        $path = if ($first.rtspPath) { $first.rtspPath } else { "/stream" }
        $port = if ($first.rtspPort) { [int]$first.rtspPort } else { 554 }
        $user = if ($first.username) { $first.username } else { $cfg.defaults.username }
        $pass = if ($first.password) { $first.password } else { $cfg.defaults.password }
        $cameraHost = $first.host

        if (-not $cameraHost) { throw "Camera host is missing" }
        if (-not $user) { $user = "admin" }
        if (-not $pass) { $pass = "password" }

        $rtspUrl = "rtsp://$cameraHost`:$port$path"
        $env:TEST_CAMERA_H264_URL = $rtspUrl
        $env:TEST_CAMERA_H265_URL = $rtspUrl
        $env:TEST_CAMERA_MJPEG_URL = $rtspUrl
        $env:TEST_CAMERA_URL = $rtspUrl
        $env:TEST_RTSP_URL = $rtspUrl

        $env:TEST_CAMERA_H264_USERNAME = $user
        $env:TEST_CAMERA_H264_PASSWORD = $pass
        $env:TEST_CAMERA_USERNAME = $user
        $env:TEST_CAMERA_PASSWORD = $pass
        $env:TEST_RTSP_USERNAME = $user
        $env:TEST_RTSP_PASSWORD = $pass

        Write-Host "Loaded camera data from config/test-cameras.local.json" -ForegroundColor Green
    } catch {
        Write-Host "Failed to parse local camera config, fallback to .test-env/template" -ForegroundColor Yellow
    }
}

# 2) Fallback: существующий .test-env
if ((-not $env:TEST_CAMERA_H264_URL) -and (Test-Path $EnvFile)) {
    Write-Host "Found existing .test-env file. Loading..." -ForegroundColor Green
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*export\s+(\w+)="?([^"]+)"?') {
            $name = $matches[1]
            $value = $matches[2]
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    Write-Host "Environment variables loaded from .test-env" -ForegroundColor Green
} elseif (-not $env:TEST_CAMERA_H264_URL) {
    Write-Host "No .test-env file found. Creating template..." -ForegroundColor Yellow

    $template = @"
# VideoDecoder Test Environment Configuration
# Edit this file with your camera URLs and credentials

# H.264 Camera
export TEST_CAMERA_H264_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_H264_USERNAME="admin"
export TEST_CAMERA_H264_PASSWORD="password"

# H.265 Camera
export TEST_CAMERA_H265_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_H265_USERNAME="admin"
export TEST_CAMERA_H265_PASSWORD="password"

# MJPEG Camera
export TEST_CAMERA_MJPEG_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_MJPEG_USERNAME="admin"
export TEST_CAMERA_MJPEG_PASSWORD="password"

# Generic (used if specific codec URLs not set)
export TEST_CAMERA_URL="rtsp://camera-ip:554/stream"
export TEST_CAMERA_USERNAME="admin"
export TEST_CAMERA_PASSWORD="password"

# Test Configuration
export TEST_DURATION_SECONDS=30
export TEST_FRAME_COUNT=750
"@

    Set-Content -Path $EnvFile -Value $template
    Write-Host "Template created at: $EnvFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "Please edit $EnvFile with your camera URLs and credentials" -ForegroundColor Yellow
    Write-Host "Then run: . .\scripts\setup-test-environment.ps1" -ForegroundColor Yellow
    return
}

# Проверка обязательных переменных
if (-not $env:TEST_CAMERA_H264_URL -and -not $env:TEST_CAMERA_URL) {
    Write-Host "WARNING: No camera URLs configured!" -ForegroundColor Yellow
    Write-Host "Please edit $EnvFile and set TEST_CAMERA_H264_URL or TEST_CAMERA_URL" -ForegroundColor Yellow
    Write-Host ""
}

# Установка значений по умолчанию
if (-not $env:TEST_CAMERA_H264_URL -and $env:TEST_CAMERA_URL) {
    $env:TEST_CAMERA_H264_URL = $env:TEST_CAMERA_URL
}

if (-not $env:TEST_CAMERA_H265_URL -and $env:TEST_CAMERA_URL) {
    $env:TEST_CAMERA_H265_URL = $env:TEST_CAMERA_URL
}

if (-not $env:TEST_CAMERA_MJPEG_URL -and $env:TEST_CAMERA_URL) {
    $env:TEST_CAMERA_MJPEG_URL = $env:TEST_CAMERA_URL
}

if (-not $env:TEST_CAMERA_USERNAME) {
    $env:TEST_CAMERA_USERNAME = if ($env:TEST_CAMERA_H264_USERNAME) { $env:TEST_CAMERA_H264_USERNAME } else { "admin" }
}

if (-not $env:TEST_CAMERA_PASSWORD) {
    $env:TEST_CAMERA_PASSWORD = if ($env:TEST_CAMERA_H264_PASSWORD) { $env:TEST_CAMERA_H264_PASSWORD } else { "password" }
}

# Вывод текущей конфигурации
Write-Host ""
Write-Host "Current Test Configuration:" -ForegroundColor Cyan
Write-Host ("  H.264 URL: " + ($(if ($env:TEST_CAMERA_H264_URL) { $env:TEST_CAMERA_H264_URL } else { "not set" })))
Write-Host ("  H.265 URL: " + ($(if ($env:TEST_CAMERA_H265_URL) { $env:TEST_CAMERA_H265_URL } else { "not set" })))
Write-Host ("  MJPEG URL: " + ($(if ($env:TEST_CAMERA_MJPEG_URL) { $env:TEST_CAMERA_MJPEG_URL } else { "not set" })))
Write-Host ("  Username: " + ($(if ($env:TEST_CAMERA_USERNAME) { $env:TEST_CAMERA_USERNAME } else { "not set" })))
Write-Host ("  Password: " + ($(if ($env:TEST_CAMERA_PASSWORD) { "***hidden***" } else { "not set" })))
Write-Host ("  Test Duration: " + ($(if ($env:TEST_DURATION_SECONDS) { $env:TEST_DURATION_SECONDS } else { "30" })) + " seconds")
Write-Host ""
Write-Host "Environment ready for testing!" -ForegroundColor Green
Write-Host "Run: .\scripts\test-video-decoder.ps1"
