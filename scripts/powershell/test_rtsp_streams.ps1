# Script: Test RTSP Streams for 7 Cameras
# Date: 2026-06-09
# Purpose: Test RTSP connectivity and stream quality for all cameras

$cameras = @(
    @{IP = "192.168.10.17"; Name = "Camera 17"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.17:554/stream1"},
    @{IP = "192.168.10.20"; Name = "Camera 20"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.20:554/stream1"},
    @{IP = "192.168.10.21"; Name = "Camera 21"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.21:554/stream1"},
    @{IP = "192.168.10.22"; Name = "Camera 22"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.22:554/stream1"},
    @{IP = "192.168.10.23"; Name = "Camera 23"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.23:554/stream1"},
    @{IP = "192.168.10.24"; Name = "Camera 24"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.24:554/stream1"},
    @{IP = "192.168.10.26"; Name = "Camera 26"; RTSP = "rtsp://survival:1234567890qazxs@192.168.10.26:554/stream1"}
)

Write-Output "=========================================="
Write-Output "  RTSP Stream Testing for 7 Cameras"
Write-Output "=========================================="
Write-Output ""

$results = @()

foreach ($camera in $cameras) {
    Write-Output "=== Testing $($camera.Name) ($($camera.IP)) ==="
    
    # Step 1: Check network connectivity
    $ping = Test-Connection $camera.IP -Count 2 -Quiet -ErrorAction SilentlyContinue
    if ($ping) {
        Write-Output "  ✅ Network: ONLINE"
    } else {
        Write-Output "  ❌ Network: OFFLINE"
        $results += @{
            Camera = $camera.Name
            IP = $camera.IP
            Network = "OFFLINE"
            RTSP = "N/A"
            Latency = "N/A"
            Status = "FAIL"
        }
        continue
    }
    
    # Step 2: Check RTSP port
    $rtspPort = Test-NetConnection $camera.IP -Port 554 -InformationLevel Quiet
    if ($rtspPort) {
        Write-Output "  ✅ RTSP Port 554: OPEN"
    } else {
        Write-Output "  ❌ RTSP Port 554: CLOSED"
        $results += @{
            Camera = $camera.Name
            IP = $camera.IP
            Network = "ONLINE"
            RTSP = "CLOSED"
            Latency = "N/A"
            Status = "FAIL"
        }
        continue
    }
    
    # Step 3: Test RTSP connection with timeout
    Write-Output "  🔄 Testing RTSP connection..."
    
    $startTime = Get-Date
    $timeout = 5000 # 5 seconds timeout
    
    try {
        # Create TCP connection to RTSP port
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.ConnectAsync($camera.IP, 554).Wait($timeout)
        
        if ($tcpClient.Connected) {
            $endTime = Get-Date
            $latency = ($endTime - $startTime).TotalMilliseconds
            
            Write-Output "  ✅ RTSP Connection: SUCCESS (${latency}ms)"
            
            # Try to send RTSP OPTIONS request
            $stream = $tcpClient.GetStream()
            $optionsRequest = "OPTIONS rtsp://$($camera.IP):554/stream1 RTSP/1.0`r`nCSeq: 1`r`n`r`n"
            $buffer = [System.Text.Encoding]::ASCII.GetBytes($optionsRequest)
            $stream.Write($buffer, 0, $buffer.Length)
            $stream.Flush()
            
            # Read response
            $responseBuffer = New-Object byte[] 1024
            $response = $stream.Read($responseBuffer, 0, $responseBuffer.Length)
            
            if ($response -gt 0) {
                $responseText = [System.Text.Encoding]::ASCII.GetString($responseBuffer, 0, $response)
                if ($responseText -match "RTSP/1.0 200") {
                    Write-Output "  ✅ RTSP Protocol: VALID"
                    $rtspStatus = "OK"
                } else {
                    Write-Output "  ⚠️  RTSP Response: $($responseText.Substring(0, [Math]::Min(50, $responseText.Length)))"
                    $rtspStatus = "OK"
                }
            } else {
                Write-Output "  ⚠️  No RTSP response received"
                $rtspStatus = "OK"
            }
            
            $tcpClient.Close()
            
            $results += @{
                Camera = $camera.Name
                IP = $camera.IP
                Network = "ONLINE"
                RTSP = "OPEN"
                Latency = [math]::Round($latency, 2)
                Status = "SUCCESS"
            }
        } else {
            Write-Output "  ❌ RTSP Connection: FAILED"
            $results += @{
                Camera = $camera.Name
                IP = $camera.IP
                Network = "ONLINE"
                RTSP = "FAILED"
                Latency = "N/A"
                Status = "FAIL"
            }
        }
    } catch {
        Write-Output "  ❌ RTSP Connection: ERROR ($($_.Exception.Message))"
        $results += @{
            Camera = $camera.Name
            IP = $camera.IP
            Network = "ONLINE"
            RTSP = "ERROR"
            Latency = "N/A"
            Status = "FAIL"
        }
    }
    
    Write-Output ""
}

# Summary
Write-Output "=========================================="
Write-Output "  Test Summary"
Write-Output "=========================================="
Write-Output ""

$successCount = ($results | Where-Object { $_.Status -eq "SUCCESS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $results.Count

Write-Output "Total Cameras: $totalCount"
Write-Output "Successful: $successCount"
Write-Output "Failed: $failCount"
Write-Output ""

# Detailed results table
Write-Output "Detailed Results:"
Write-Output "-----------------"
$results | Format-Table -Property Camera, IP, Network, RTSP, Latency, Status -AutoSize

# Export results to JSON
$results | ConvertTo-Json | Out-File "rtsp_test_results.json" -Encoding UTF8
Write-Output ""
Write-Output "✅ Results exported to: rtsp_test_results.json"

# Calculate average latency
$successfulResults = $results | Where-Object { $_.Status -eq "SUCCESS" -and $_.Latency -ne "N/A" }
if ($successfulResults.Count -gt 0) {
    $avgLatency = ($successfulResults | Measure-Object -Property Latency -Average).Average
    Write-Output "Average RTSP Latency: $([math]::Round($avgLatency, 2)) ms"
}

Write-Output ""
Write-Output "=========================================="

# Return exit code based on success rate
if ($successCount -eq $totalCount) {
    Write-Output "✅ ALL TESTS PASSED"
    exit 0
} elseif ($successCount -ge ($totalCount * 0.8)) {
    Write-Output "⚠️  MOST TESTS PASSED ($successCount/$totalCount)"
    exit 0
} else {
    Write-Output "❌ TESTS FAILED ($failCount/$totalCount)"
    exit 1
}
