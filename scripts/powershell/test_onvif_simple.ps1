# Script: ONVIF Integration Testing (Simplified)
# Date: 2026-06-09
# Purpose: Test ONVIF connectivity for all cameras

$cameras = @(
    @{IP = "192.168.10.17"; Name = "Camera 17"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"},
    @{IP = "192.168.10.20"; Name = "Camera 20"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"},
    @{IP = "192.168.10.21"; Name = "Camera 21"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"},
    @{IP = "192.168.10.22"; Name = "Camera 22"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"},
    @{IP = "192.168.10.23"; Name = "Camera 23"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"},
    @{IP = "192.168.10.24"; Name = "Camera 24"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"},
    @{IP = "192.168.10.26"; Name = "Camera 26"; Username = $env:CAMERA_USERNAME; Password = "$env:CAMERA_PASSWORD"}
)

Write-Output "=========================================="
Write-Output "  ONVIF Integration Testing (Simplified)"
Write-Output "=========================================="
Write-Output ""

$results = @()

foreach ($camera in $cameras) {
    Write-Output "=== Testing $($camera.Name) ($($camera.IP)) ==="
    
    # Test common ONVIF ports
    $onvifPorts = @(80, 8080, 8081)
    $foundPort = $null
    
    foreach ($port in $onvifPorts) {
        $result = Test-NetConnection $camera.IP -Port $port -InformationLevel Quiet
        if ($result) {
            $foundPort = $port
            Write-Output "  вњ… Port ${port}: OPEN (possible ONVIF)"
            break
        }
    }
    
    if (-not $foundPort) {
        Write-Output "  вќЊ No ONVIF port found"
        $results += @{
            Camera = $camera.Name
            IP = $camera.IP
            Port = "NONE"
            Status = "FAIL"
        }
        continue
    }
    
    # Try HTTP GET to common ONVIF endpoints
    $endpoints = @("/onvif/device_service", "/onvif", "/cam1/preview", "/webcam")
    $foundEndpoint = $null
    
    foreach ($endpoint in $endpoints) {
        try {
            $url = "http://$($camera.IP):$foundPort$endpoint"
            $response = Invoke-WebRequest -Uri $url -Method GET -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            
            if ($response.StatusCode -eq 200) {
                $foundEndpoint = $endpoint
                Write-Output "  вњ… Endpoint found: $endpoint"
                break
            }
        } catch {
            # Continue to next endpoint
        }
    }
    
    if ($foundEndpoint) {
        Write-Output "  вњ… ONVIF/HTTP: ACCESSIBLE"
        $results += @{
            Camera = $camera.Name
            IP = $camera.IP
            Port = $foundPort
            Status = "SUCCESS"
        }
    } else {
        Write-Output "  вљ пёЏ  Port open but no ONVIF endpoint found"
        $results += @{
            Camera = $camera.Name
            IP = $camera.IP
            Port = $foundPort
            Status = "PARTIAL"
        }
    }
    
    Write-Output ""
}

# Summary
Write-Output "=========================================="
Write-Output "  Summary"
Write-Output "=========================================="
Write-Output ""

$successCount = ($results | Where-Object { $_.Status -eq "SUCCESS" }).Count
$partialCount = ($results | Where-Object { $_.Status -eq "PARTIAL" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Output "Total: $($results.Count)"
Write-Output "Success: $successCount"
Write-Output "Partial: $partialCount"
Write-Output "Fail: $failCount"
Write-Output ""

$results | Format-Table -AutoSize

$results | ConvertTo-Json | Out-File "onvif_results.json"
Write-Output "Results saved to: onvif_results.json"
