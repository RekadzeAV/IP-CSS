# Generate Mock Data for Testing
# Creates emulation of real cameras and test results

param(
    [string]$OutputDir = "diagnostics/mock-data",
    [int]$CameraCount = 5,
    [switch]$GenerateAll,
    [switch]$GenerateRTSP,
    [switch]$GenerateHLS,
    [switch]$GenerateScreenshot,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Generate Mock Data for Testing

Usage:
  .\scripts\generate-mock-data.ps1 [-GenerateAll]

Options:
  -OutputDir       Output directory for mock data (default: diagnostics/mock-data)
  -CameraCount     Number of cameras (default: 5)
  -GenerateAll     Generate all data
  -GenerateRTSP    Generate RTSP mock data
  -GenerateHLS     Generate HLS mock data
  -GenerateScreenshot Generate Screenshot mock data
  -ShowHelp        Show this help

Examples:
  # Generate all mock data
  .\scripts\generate-mock-data.ps1 -GenerateAll

  # Generate only RTSP
  .\scripts\generate-mock-data.ps1 -GenerateRTSP

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$OutputDir = Join-Path $projectRoot $OutputDir

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

Write-Host "Mock Data Generation" -ForegroundColor Cyan
Write-Host "Timestamp: $timestamp" -ForegroundColor White
Write-Host "Output: $OutputDir" -ForegroundColor White
Write-Host ""

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "  SUCCESS: $Message" -ForegroundColor Green
}

function Write-Step {
    param([string]$Message)
    Write-Host "  -> $Message" -ForegroundColor Yellow
}

# ============================================================================
# Generate RTSP Mock Data
# ============================================================================

if ($GenerateAll -or $GenerateRTSP) {
    Write-Host ""
    Write-Host "Phase 1: RTSP Mock Data" -ForegroundColor Cyan
    
    $rtspDir = Join-Path $OutputDir "rtsp-mock"
    if (!(Test-Path $rtspDir)) {
        New-Item -ItemType Directory -Path $rtspDir -Force | Out-Null
    }
    
    # Generate mock RTSP tests
    for ($i = 1; $i -le $CameraCount; $i++) {
        $cameraId = "camera-$i"
        $testFile = Join-Path $rtspDir "rtsp-test-$cameraId-$timestamp.md"
        
        $connectionTime = Get-Random -Minimum 50 -Maximum 200
        $reconnectTime = Get-Random -Minimum 100 -Maximum 500
        
        $testContent = @"
# RTSP Test Report: $cameraId

**Timestamp:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Camera ID:** $cameraId  
**Status:** PASSED

## Connection Test

- **RTSP URL:** rtsp://192.168.1.10$i:554/stream1
- **Connection Time:** ${connectionTime}ms
- **Status:** Connected

## Video Stream Test

- **Codec:** H.264
- **Resolution:** 1920x1080
- **FPS:** 25
- **Bitrate:** 4096 kbps
- **Status:** Streaming

## Audio Stream Test

- **Codec:** AAC
- **Sample Rate:** 44100 Hz
- **Channels:** 2
- **Status:** Streaming

## Reconnect Test

- **Attempts:** 3
- **Success Rate:** 100%
- **Average Reconnect Time:** ${reconnectTime}ms

## Summary

All tests passed successfully. Camera is fully operational.
"@
        
        Set-Content -Path $testFile -Value $testContent -Encoding UTF8
        Write-Success "RTSP test: $cameraId"
    }
    
    # Generate summary
    $summaryFile = Join-Path $rtspDir "rtsp-summary-$timestamp.md"
    $summaryContent = @"
# RTSP Mock Summary

**Generated:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Total Cameras:** $CameraCount  
**Status:** All Passed

## Results

| Camera | Status | Connection | Video | Audio |
|--------|--------|------------|-------|-------|
"@
    
    for ($i = 1; $i -le $CameraCount; $i++) {
        $summaryContent += "| camera-$i | PASSED | OK | OK | OK |`n"
    }
    
    Set-Content -Path $summaryFile -Value $summaryContent -Encoding UTF8
    Write-Success "RTSP summary created"
}

# ============================================================================
# Generate HLS Mock Data
# ============================================================================

if ($GenerateAll -or $GenerateHLS) {
    Write-Host ""
    Write-Host "Phase 2: HLS Mock Data" -ForegroundColor Cyan
    
    $hlsDir = Join-Path $OutputDir "hls-mock"
    if (!(Test-Path $hlsDir)) {
        New-Item -ItemType Directory -Path $hlsDir -Force | Out-Null
    }
    
    # Generate mock HLS tests
    $hlsTestFile = Join-Path $hlsDir "hls-runtime-stability-test-$timestamp.md"
    
    $segmentSize = Get-Random -Minimum 500 -Maximum 1500
    $totalData = Get-Random -Minimum 15000 -Maximum 45000
    $reconnectTime = Get-Random -Minimum 200 -Maximum 800
    $cpuUsage = Get-Random -Minimum 10 -Maximum 30
    $memoryUsage = Get-Random -Minimum 100 -Maximum 300
    $diskIo = Get-Random -Minimum 5 -Maximum 20
    
    $hlsContent = @"
# HLS Runtime Stability Test

**Timestamp:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Duration:** 120 seconds  
**Status:** PASSED

## Test Configuration

- **Test Duration:** 120s
- **Segment Duration:** 4s
- **Playlist Size:** 3
- **Expected Segments:** 30

## Results

### Long-Run Test

- **Segments Generated:** 30
- **Success Rate:** 100%
- **Average Segment Size:** ${segmentSize}KB
- **Total Data:** ${totalData}KB

### Cleanup Test

- **Old Segments Cleaned:** 27
- **Cleanup Success:** 100%
- **Orphaned Files:** 0

### Reconnect Test

- **Reconnect Attempts:** 3
- **Success Rate:** 100%
- **Average Reconnect Time:** ${reconnectTime}ms

## Performance Metrics

- **CPU Usage:** ${cpuUsage}%
- **Memory Usage:** ${memoryUsage}MB
- **Disk I/O:** ${diskIo}MB/s

## Summary

All HLS tests passed successfully. Pipeline is stable and operational.
"@
    
    Set-Content -Path $hlsTestFile -Value $hlsContent -Encoding UTF8
    Write-Success "HLS test created"
    
    # Generate HLS playlist mock
    $playlistFile = Join-Path $hlsDir "playlist.m3u8"
    $playlistContent = "#EXTM3U`n"
    $playlistContent += "#EXT-X-VERSION:3`n"
    $playlistContent += "#EXT-X-TARGETDURATION:4`n"
    $playlistContent += "#EXT-X-MEDIA-SEQUENCE:0`n"
    
    for ($i = 0; $i -lt 10; $i++) {
        $playlistContent += "#EXTINF:4.0,`n"
        $playlistContent += "segment_$i.ts`n"
    }
    
    $playlistContent += "#EXT-X-ENDLIST`n"
    
    Set-Content -Path $playlistFile -Value $playlistContent -Encoding UTF8
    Write-Success "HLS playlist created"
}

# ============================================================================
# Generate Screenshot Mock Data
# ============================================================================

if ($GenerateAll -or $GenerateScreenshot) {
    Write-Host ""
    Write-Host "Phase 3: Screenshot Mock Data" -ForegroundColor Cyan
    
    $screenshotDir = Join-Path $OutputDir "screenshot-mock"
    if (!(Test-Path $screenshotDir)) {
        New-Item -ItemType Directory -Path $screenshotDir -Force | Out-Null
    }
    
    # Generate mock screenshot tests
    $screenshotTestFile = Join-Path $screenshotDir "screenshot-pipeline-test-$timestamp.md"
    
    $screenshotContent = @"
# Screenshot Pipeline Test

**Timestamp:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Status:** PASSED

## Test Configuration

- **Total Screenshots:** $CameraCount
- **Format:** JPEG
- **Quality:** 85%
- **Resolution:** 1920x1080

## Results

| Camera | Status | Size | Time |
|--------|--------|------|------|
"@
    
    $totalSize = 0
    for ($i = 1; $i -le $CameraCount; $i++) {
        $size = Get-Random -Minimum 200000 -Maximum 500000
        $time = Get-Random -Minimum 100 -Maximum 500
        $totalSize += $size
        $sizeKb = [math]::Round($size/1024, 1)
        $screenshotContent += "| camera-$i | PASSED | ${sizeKb}KB | ${time}ms |`n"
    }
    
    $avgCaptureTime = Get-Random -Minimum 150 -Maximum 400
    $totalMb = [math]::Round($totalSize/1024/1024, 2)
    
    $screenshotContent += @"

## Performance Metrics

- **Average Capture Time:** ${avgCaptureTime}ms
- **Average File Size:** ${totalMb}MB
- **Success Rate:** 100%

## Summary

All screenshot tests passed successfully. Pipeline is operational.
"@
    
    Set-Content -Path $screenshotTestFile -Value $screenshotContent -Encoding UTF8
    Write-Success "Screenshot test created"
    
    # Generate placeholder JPEG files
    for ($i = 1; $i -le $CameraCount; $i++) {
        $jpegFile = Join-Path $screenshotDir "screenshot-camera-$i-$timestamp.jpg"
        # Create minimal valid JPEG (1x1 pixel)
        $jpegBytes = @(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0xFF, 0xD9)
        Set-Content -Path $jpegFile -Value $jpegBytes -Encoding Byte
        Write-Success "Screenshot: camera-$i"
    }
}

# ============================================================================
# Generate Summary Report
# ============================================================================

Write-Host ""
Write-Host "Phase 4: Summary Report" -ForegroundColor Cyan

$summaryFile = Join-Path $OutputDir "mock-data-summary-$timestamp.md"

$summaryContent = @"
# Mock Data Summary

**Generated:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Timestamp:** $timestamp  
**Output Directory:** $OutputDir

## Generated Data

### RTSP Mock Data

- **Location:** $rtspDir
- **Tests Generated:** $CameraCount
- **Status:** Complete

### HLS Mock Data

- **Location:** $hlsDir
- **Tests Generated:** 1
- **Status:** Complete

### Screenshot Mock Data

- **Location:** $screenshotDir
- **Tests Generated:** 1
- **Screenshots:** $CameraCount
- **Status:** Complete

## Usage

This mock data can be used for:

1. Testing scripts without real cameras
2. Demonstrating functionality
3. Debugging and development
4. Training and documentation

## Next Steps

1. Run tests with mock data
2. Verify correct result processing
3. Proceed to tests with real cameras

---

*Generated by generate-mock-data.ps1*
"@

Set-Content -Path $summaryFile -Value $summaryContent -Encoding UTF8
Write-Success "Summary created: $summaryFile"

# ============================================================================
# Final Summary
# ============================================================================

Write-Host ""
Write-Host "Generation Complete!" -ForegroundColor Green
Write-Host "All mock data in: $OutputDir" -ForegroundColor Cyan
