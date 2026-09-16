# Test RTSP client with live stream
Write-Host "=== RTSP Live Stream Test ===" -ForegroundColor Cyan

# Check prerequisites
Write-Host "`n[1/3] Checking prerequisites..." -ForegroundColor Yellow

if (!(Test-Path "native\video-processing\lib\windows\x64\video_processing.dll")) {
    Write-Host "ERROR: video_processing.dll not found" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Native library found" -ForegroundColor Green

if (!(Test-NetConnection -ComputerName localhost -Port 8554 -InformationLevel Quiet)) {
    Write-Host "ERROR: MediaMTX not accessible on port 8554" -ForegroundColor Red
    exit 1
}
Write-Host "✓ MediaMTX accessible" -ForegroundColor Green

# Run test
Write-Host "`n[2/3] Running RTSP client test..." -ForegroundColor Yellow

$env:PATH = "native\video-processing\lib\windows\x64;" + $env:PATH

$testCode = @'
package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspStreamType
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    println("=== NativeRtspClient Live Stream Test ===")
    
    val client = NativeRtspClient()
    val handle = client.create()
    
    if (handle == 0L) {
        println("ERROR: Failed to create RTSP client handle")
        return
    }
    
    println("✓ Client handle created: $handle")
    
    val frameReceived = AtomicBoolean(false)
    val frameCount = AtomicInteger(0)
    
    client.setStatusCallback(handle) { status, message ->
        println("Status: $status - ${message ?: "no message"}")
    }
    
    client.setFrameCallback(handle, RtspStreamType.VIDEO) { frame ->
        if (!frameReceived.getAndSet(true)) {
            println("✓ First video frame received at timestamp: ${frame.timestamp}ms")
        }
        frameCount.incrementAndGet()
        if (frameCount.get() % 30 == 0) {
            println("  Frames received: ${frameCount.get()}")
        }
    }
    
    println("\nConnecting to rtsp://localhost:8554/test...")
    val connected = client.connect(
        handle = handle,
        url = "rtsp://localhost:8554/test",
        username = null,
        password = null,
        timeoutMs = 5000
    )
    
    if (!connected) {
        println("ERROR: Failed to connect to RTSP server")
        println("  MediaMTX may not be running or stream not available")
        client.destroy(handle)
        return
    }
    
    println("✓ Connected successfully")
    
    println("\nStarting playback...")
    val played = client.play(handle)
    
    if (!played) {
        println("ERROR: Failed to start playback")
        client.destroy(handle)
        return
    }
    
    println("✓ Playback started")
    
    println("\nReceiving frames for 10 seconds...")
    Thread.sleep(10000)
    
    println("\n✓ Frame count after 10s: ${frameCount.get()}")
    
    if (frameCount.get() > 0) {
        println("SUCCESS: Video frames received!")
    } else {
        println("WARNING: No video frames received")
    }
    
    println("\nStopping playback...")
    client.stop(handle)
    println("✓ Playback stopped")
    
    println("\nDisconnecting...")
    client.disconnect(handle)
    println("✓ Disconnected")
    
    client.destroy(handle)
    println("✓ Client destroyed")
    
    println("\n=== Test Complete ===")
    println("Total frames received: ${frameCount.get()}")
}
'@

Set-Content -Path "TestRunner.kt" -Value $testCode -Encoding UTF8

Write-Host "Compiling test..." -ForegroundColor Yellow
.\gradlew.bat :core:network:compileKotlinDesktop --no-daemon 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running test..." -ForegroundColor Yellow
    # Use Kotlin script runner
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    java -cp "core/network/build/classes/kotlin/desktop/main" `
         -Djava.library.path="native\video-processing\lib\windows\x64" `
         kotlin.jvm.JvmStaticTestKt 2>&1 | Select-String -Pattern "ERROR|SUCCESS|WARNING|✓|Status|Frames|Connecting|Connected|Test Complete"
} else {
    Write-Host "Compilation failed" -ForegroundColor Red
}

Remove-Item "TestRunner.kt" -ErrorAction SilentlyContinue

Write-Host "`n[3/3] Cleanup..." -ForegroundColor Yellow
Write-Host "✓ Test complete" -ForegroundColor Green
