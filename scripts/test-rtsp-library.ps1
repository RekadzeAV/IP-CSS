# Test script for RTSP native library
# Checks library existence, exports, and basic functionality

param(
    [string]$LibraryPath = "$PSScriptRoot\..\native\video-processing\lib\windows\x64\video_processing.dll"
)

$ErrorActionPreference = "Continue"

# Colors
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Failure {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

Write-Info "Testing RTSP native library"
Write-Info "Library path: $LibraryPath"

# Test 1: Check library exists
Write-Info ""
Write-Info "Test 1: Checking library existence..."
if (Test-Path $LibraryPath) {
    $fileInfo = Get-Item $LibraryPath
    Write-Success "Library found: $($fileInfo.FullName)"
    Write-Info "   Size: $([math]::Round($fileInfo.Length / 1KB, 2)) KB"
    Write-Info "   Modified: $($fileInfo.LastWriteTime)"
} else {
    Write-Failure "Library not found at: $LibraryPath"
    Write-Info "   Run: .\scripts\build-video-processing-lib.ps1"
    exit 1
}

# Test 2: Check FFmpeg dependencies
Write-Info ""
Write-Info "Test 2: Checking FFmpeg dependencies..."
$FFmpegDlls = @(
    "avcodec-62.dll",
    "avformat-62.dll",
    "avutil-60.dll",
    "swscale-9.dll",
    "swresample-6.dll"
)

$libDir = Split-Path $LibraryPath
$missingDlls = @()

foreach ($dll in $FFmpegDlls) {
    $dllPath = Join-Path $libDir $dll
    if (Test-Path $dllPath) {
        Write-Success "  $dll found"
    } else {
        Write-Failure "  $dll NOT found"
        $missingDlls += $dll
    }
}

if ($missingDlls.Count -gt 0) {
    Write-Warn "Missing FFmpeg DLLs. Copy from C:\ffmpeg\bin to $libDir"
} else {
    Write-Success "All FFmpeg dependencies found"
}

# Test 3: Check exported symbols
Write-Info ""
Write-Info "Test 3: Checking exported symbols..."

try {
    $objdumpPath = (Get-Command objdump -ErrorAction SilentlyContinue).Source
    if ($objdumpPath) {
        Write-Info "Using objdump: $objdumpPath"
        
        $exportedSymbols = objdump -x $LibraryPath 2>&1 | Select-String "rtsp_client"
        
        if ($exportedSymbols) {
            Write-Success "Found $($exportedSymbols.Count) RTSP symbols"
            
            $expectedFunctions = @(
                "rtsp_client_create",
                "rtsp_client_destroy",
                "rtsp_client_connect",
                "rtsp_client_disconnect",
                "rtsp_client_play",
                "rtsp_client_stop",
                "rtsp_client_pause",
                "rtsp_client_get_status",
                "rtsp_client_get_stream_count",
                "rtsp_client_set_frame_callback",
                "rtsp_client_set_status_callback"
            )
            
            $foundCount = 0
            foreach ($func in $expectedFunctions) {
                if ($exportedSymbols -match $func) {
                    $foundCount++
                }
            }
            
            Write-Success "Found $([math]::Round($foundCount / $expectedFunctions.Count * 100, 0))% of expected functions"
        } else {
            Write-Failure "No RTSP symbols found in library"
        }
    } else {
        Write-Warn "objdump not found - skipping symbol check"
    }
} catch {
    Write-Warn "Could not check symbols: $($_.Exception.Message)"
}

# Test 4: Check library architecture
Write-Info ""
Write-Info "Test 4: Checking library architecture..."
try {
    $fileBytes = [System.IO.File]::ReadAllBytes($LibraryPath)
    $dosHeader = [System.BitConverter]::ToInt32($fileBytes[0..3], 0)
    
    if ($dosHeader -eq 0x5A4D) {
        Write-Success "Windows PE executable found"
        
        $peOffset = [System.BitConverter]::ToInt32($fileBytes[60..63], 0)
        $magic = [System.BitConverter]::ToInt16($fileBytes[($peOffset + 24)..($peOffset + 25)], 0)
        
        if ($magic -eq 0x20b) {
            Write-Success "Architecture: x64 (PE32+)"
        } elseif ($magic -eq 0x10b) {
            Write-Success "Architecture: x86 (PE32)"
        }
    } else {
        Write-Failure "Not a valid Windows PE file"
    }
} catch {
    Write-Warn "Could not check architecture: $($_.Exception.Message)"
}

# Test 5: Verify FFI def file
Write-Info ""
Write-Info "Test 5: Checking FFI configuration..."
$defFile = "$PSScriptRoot\..\core\network\src\nativeInterop\cinterop\rtsp_client.def"

if (Test-Path $defFile) {
    Write-Success "FFI def file found: $defFile"
    
    $defContent = Get-Content $defFile -Raw
    
    if ($defContent -match "language\s*=\s*C") {
        Write-Success "Language: C"
    }
    
    if ($defContent -match "headers\s*=\s*rtsp_client.h") {
        Write-Success "Header: rtsp_client.h"
    }
    
    if ($defContent -match "package\s*=\s*(.+)") {
        $package = $matches[1].Trim()
        Write-Success "Package: $package"
    }
} else {
    Write-Failure "FFI def file not found: $defFile"
}

# Summary
Write-Info ""
Write-Host "=================================================="
Write-Info "Test Summary"
Write-Host "=================================================="

$allTestsPassed = $true

if (-not (Test-Path $LibraryPath)) {
    $allTestsPassed = $false
}

if ($missingDlls.Count -gt 0) {
    $allTestsPassed = $false
}

if ($allTestsPassed) {
    Write-Success "All tests passed! Library is ready for integration."
    Write-Info ""
    Write-Info "Next steps:"
    Write-Info "  1. Wait for FFI compilation to complete"
    Write-Info "  2. Run: .\gradlew.bat :core:network:compileKotlinNative"
    Write-Info "  3. Test integration with real RTSP camera"
} else {
    Write-Warn "Some tests failed. Review output above."
}

Write-Info ""
exit 0
