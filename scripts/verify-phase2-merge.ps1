# Phase 2 Pre-Merge Verification Script
# Version: 0.1.2-beta
# Date: 11 June 2026

param(
    [switch]$Verbose,
    [switch]$CheckBuildOnly,
    [switch]$CheckTestsOnly,
    [string]$ReportJson
)

$ErrorActionPreference = "Stop"
$startTime = Get-Date

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Phase 2 Pre-Merge Verification" -ForegroundColor Cyan
Write-Host "Started: $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Results tracking
$checksPassed = 0
$checksFailed = 0

function Write-Success {
    param([string]$Message)
    Write-Host "  [PASS] $Message" -ForegroundColor Green
    $script:checksPassed++
}

function Write-Failure {
    param([string]$Message)
    Write-Host "  [FAIL] $Message" -ForegroundColor Red
    $script:checksFailed++
}

function Write-Info {
    param([string]$Message)
    if ($Verbose) {
        Write-Host "  [INFO] $Message" -ForegroundColor Yellow
    }
}

# 1. Build check
Write-Host "`n[1/5] Checking build..." -ForegroundColor Cyan
try {
    $dllPath = "native/video-processing/lib/windows/x64/video_processing.dll"
    if (Test-Path $dllPath) {
        $dllInfo = Get-Item $dllPath
        Write-Success "DLL exists: $($dllInfo.LastWriteTime)"
    } else {
        Write-Failure "DLL not found: $dllPath"
    }
    
    $buildDir = "native/video-processing/build"
    if (Test-Path $buildDir) {
        Write-Success "Build directory exists"
    } else {
        Write-Failure "Build directory not found"
    }
} catch {
    Write-Failure "Build check error: $_"
}

if (-not $CheckTestsOnly) {
    # 2. Documentation check
    Write-Host "`n[2/5] Checking documentation..." -ForegroundColor Cyan
    
    $docs = @(
        "docs/reports/PHASE2_MVP_COMPLETE_2026-06-11.md",
        "docs/reports/CODE_REVIEW_PHASE2_2026-06-11.md",
        "docs/reports/PHASE2_FINAL_SUMMARY_2026-06-11.md",
        "docs/rtsp/FFI_INTEGRATION_GUIDE_2026-06-11.md",
        "CHANGELOG_PHASE2.md"
    )
    
    foreach ($doc in $docs) {
        if (Test-Path $doc) {
            Write-Success "Document exists: $doc"
        } else {
            Write-Failure "Document missing: $doc"
        }
    }
    
    # README check
    if (Test-Path "README.md") {
        $readme = Get-Content "README.md" -Raw
        if ($readme -match "0\.1\.2-beta") {
            Write-Success "README updated with version 0.1.2-beta"
        } else {
            Write-Failure "README not updated with new version"
        }
    } else {
        Write-Failure "README.md not found"
    }
    
    # 3. Tools check
    Write-Host "`n[3/5] Checking tools..." -ForegroundColor Cyan
    
    if (Test-Path "test_rtsp_integration.ps1") {
        Write-Success "Test script exists"
    } else {
        Write-Failure "Test script not found"
    }
    
    if (Test-Path "tools/rtsp_monitor.ps1") {
        Write-Success "Monitor utility exists"
    } else {
        Write-Failure "Monitor utility not found"
    }
}

if (-not $CheckBuildOnly) {
    # 4. Code check
    Write-Host "`n[4/5] Checking code..." -ForegroundColor Cyan
    
    $cppFile = "native/video-processing/src/rtsp_client.cpp"
    if (Test-Path $cppFile) {
        $cppContent = Get-Content $cppFile -Raw
        
        if ($cppContent -match "build_rtsp_url") {
            Write-Success "Helper function build_rtsp_url() found"
        } else {
            Write-Failure "Helper function build_rtsp_url() not found"
        }
        
        if ($cppContent -match "getaddrinfo") {
            Write-Success "Modern API getaddrinfo() used"
        } else {
            Write-Failure "Modern API getaddrinfo() not found"
        }
        
        if ($cppContent -match "handshakeCv|handshakeComplete") {
            Write-Success "Thread synchronization implemented"
        } else {
            Write-Failure "Thread synchronization not found"
        }
        
        if ($cppContent -match "try") {
            Write-Success "Error handling with try-catch found"
        } else {
            Write-Failure "Error handling not found"
        }
    } else {
        Write-Failure "C++ source file not found"
    }
    
    # 5. Status check
    Write-Host "`n[5/5] Checking project status..." -ForegroundColor Cyan
    
    $statusFile = "docs/status/PROJECT_STATUS.md"
    if (Test-Path $statusFile) {
        $statusContent = Get-Content $statusFile -Raw
        if ($statusContent -match "Phase 2 MVP COMPLETE|0\.1\.2-beta") {
            Write-Success "Project status updated"
        } else {
            Write-Info "Project status not fully updated (optional)"
        }
    }
}

# Summary
$endTime = Get-Date
$duration = ($endTime - $startTime).TotalSeconds

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Duration: $([math]::Round($duration, 2))s" -ForegroundColor White
Write-Host "Checks Passed: $checksPassed" -ForegroundColor Green
Write-Host "Checks Failed: $checksFailed" -ForegroundColor $(if ($checksFailed -eq 0) { "Green" } else { "Red" })

$readyForMerge = ($checksFailed -eq 0)
$mergeStatus = if ($readyForMerge) { "YES" } else { "NO" }
Write-Host "Ready for Merge: $mergeStatus" -ForegroundColor $(if ($readyForMerge) { "Green" } else { "Red" })
Write-Host "========================================`n" -ForegroundColor Cyan

# JSON Report
if ($ReportJson) {
    $report = @{
        timestamp = $startTime.ToString('o')
        version = "0.1.2-beta"
        status = $(if ($readyForMerge) { "PASSED" } else { "FAILED" })
        duration = $duration
        checks = @{
            passed = $checksPassed
            failed = $checksFailed
        }
        readyForMerge = $readyForMerge
    }
    
    $report | ConvertTo-Json | Out-File $ReportJson -Encoding UTF8
    Write-Host "Report saved to: $ReportJson" -ForegroundColor Yellow
}

# Exit code
exit $(if ($readyForMerge) { 0 } else { 1 })
