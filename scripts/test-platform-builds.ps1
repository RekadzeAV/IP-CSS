<#
.SYNOPSIS
Test platform builds with detailed logging
#>

param(
    [string]$Timestamp = (Get-Date -Format "yyyyMMdd_HHmmss")
)

$ErrorActionPreference = "Stop"
$rootDir = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $rootDir "build-logs"
$logFile = Join-Path $logDir "build-summary_${Timestamp}.txt"

# Ensure log directory exists
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$timestamp] $Message"
    Add-Content -Path $logFile -Value "[$timestamp] $Message"
}

function Test-Build {
    param(
        [string]$TaskName,
        [string]$Platform,
        [string]$GradleArgs = ""
    )
    
    Write-Log "=========================================="
    Write-Log "Testing: $TaskName ($Platform)"
    Write-Log "=========================================="
    
    $startTime = Get-Date
    $logPath = Join-Path $logDir "platform-${Platform}-${Timestamp}.log"
    
    try {
        Push-Location $rootDir
        
        # Run gradle with detailed logging
        $gradleCmd = "./gradlew $TaskName --info --debug --stacktrace $GradleArgs 2>&1"
        $output = Invoke-Expression $gradleCmd | Tee-Object -FilePath $logPath
        
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalSeconds
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "PASS: $TaskName completed in $duration seconds"
            Write-Log "  Log: $logPath"
            return @{ Status = "PASS"; Duration = $duration; Log = $logPath }
        } else {
            Write-Log "FAIL: $TaskName failed after $duration seconds"
            Write-Log "  Log: $logPath"
            Write-Log "  Last 20 lines of output:"
            $output | Select-Object -Last 20 | ForEach-Object { Write-Log "    $_" }
            return @{ Status = "FAIL"; Duration = $duration; Log = $logPath }
        }
    }
    catch {
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalSeconds
        Write-Log "ERROR: $TaskName failed with exception after $duration seconds"
        Write-Log "  Error: $_"
        return @{ Status = "ERROR"; Duration = $duration; Log = $logPath }
    }
    finally {
        Pop-Location
    }
}

# Start summary
Write-Log "=========================================="
Write-Log "Platform Build Test Suite"
Write-Log "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Log "=========================================="
Write-Log ""

$results = @()

# Test 1: Core Common Desktop (JVM)
$results += Test-Build -TaskName ":core:common:compileKotlinDesktop" -Platform "core-common-desktop"

# Test 2: Core Common Android
$results += Test-Build -TaskName ":core:common:compileKotlinAndroid" -Platform "core-common-android"

# Test 3: Core Network Desktop (JVM)
$results += Test-Build -TaskName ":core:network:compileKotlinDesktop" -Platform "core-network-desktop"

# Test 4: Shared Desktop (JVM)
$results += Test-Build -TaskName ":shared:compileKotlinDesktop" -Platform "shared-desktop"

# Test 5: Server API (JVM)
$results += Test-Build -TaskName ":server:api:build" -Platform "server-api"

# Test 6: Core Security Desktop (JVM)
$results += Test-Build -TaskName ":core:security:compileKotlinDesktop" -Platform "core-security-desktop"

# Summary
Write-Log ""
Write-Log "=========================================="
Write-Log "Build Summary"
Write-Log "=========================================="

$passCount = ($results | Where-Object Status -eq "PASS").Count
$failCount = ($results | Where-Object Status -eq "FAIL").Count
$errorCount = ($results | Where-Object Status -eq "ERROR").Count
$totalDuration = ($results | Measure-Object -Property Duration -Sum).Sum

Write-Log "Total: $($results.Count) builds"
Write-Log "Passed: $passCount"
Write-Log "Failed: $failCount"
Write-Log "Errors: $errorCount"
Write-Log "Total time: $([math]::Round($totalDuration, 2)) seconds"
Write-Log ""

Write-Log "Detailed Results:"
$results | ForEach-Object {
    $statusIcon = switch ($_.Status) {
        "PASS" { "[OK]" }
        "FAIL" { "[FAIL]" }
        "ERROR" { "[ERR]" }
    }
    $logName = Split-Path -Leaf $_.Log
    Write-Log "  $statusIcon $($_.Status): $logName ($($_.Duration) seconds)"
}

Write-Log ""
Write-Log "=========================================="
Write-Log "Completed: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Log "=========================================="

# Exit with error if any builds failed
if ($failCount -gt 0 -or $errorCount -gt 0) {
    exit 1
}

exit 0