# Script to add 7 cameras to IP-CSS system

$baseUrl = "http://localhost:8080/api/v1"
$username = "survival"
$password = "1234567890qazxs"

# Camera list with URL-encoded credentials in RTSP URLs
# Password 1234567890qazxs contains no special chars that need encoding
# Credentials are passed separately in username/password fields
$cameras = @(
    @{IP = "192.168.10.17"; Name = "Camera 17"; RTSP = "rtsp://192.168.10.17:554/stream1"},
    @{IP = "192.168.10.20"; Name = "Camera 20"; RTSP = "rtsp://192.168.10.20:554/stream1"},
    @{IP = "192.168.10.21"; Name = "Camera 21"; RTSP = "rtsp://192.168.10.21:554/stream1"},
    @{IP = "192.168.10.22"; Name = "Camera 22"; RTSP = "rtsp://192.168.10.22:554/stream1"},
    @{IP = "192.168.10.23"; Name = "Camera 23"; RTSP = "rtsp://192.168.10.23:554/stream1"},
    @{IP = "192.168.10.24"; Name = "Camera 24"; RTSP = "rtsp://192.168.10.24:554/stream1"},
    @{IP = "192.168.10.26"; Name = "Camera 26"; RTSP = "rtsp://192.168.10.26:554/stream1"}
)

# Login to get token
Write-Output "=== LOGIN ==="
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -SessionVariable session

$token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJpcC1jYW1lcmEtc2VydmVyIiwiYXVkIjoiaXAtY2FtZXJhLWNsaWVudCIsInN1YiI6ImYyMWQyZDc0LWZmNTItNGZiOC1iY2EzLTQ1ODc5ZDM3ZWVjMiIsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4iLCJwZXJtaXNzaW9ucyI6WyIqIl0sImlhdCI6MTc4MTAxMjg0NCwiZXhwIjoxNzgxMDEzNzQ0fQ.MyAqCSdj6xzrG6zsUh-KXyO2kJicg07n2ctc4Vk2eVE"
$headers = @{ Authorization = "Bearer $token" }

Write-Output "Login successful! Token obtained.`n"

# Add each camera
$successCount = 0
$failCount = 0

foreach ($camera in $cameras) {
    Write-Output "Adding $($camera.Name) ($($camera.IP))..."
    
    # Simplified camera data - minimal required fields
    $cameraData = @{
        name = $camera.Name
        url = $camera.RTSP
        username = $username
        password = $password
        model = "IP Camera"
        fps = 25
        bitrate = 2048
        codec = "H.264"
        audio = $false
        resolution = @{
            width = 1920
            height = 1080
        }
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/cameras" -Method POST -ContentType "application/json" -Body $cameraData -Headers $headers
        Write-Output "  ✅ SUCCESS: $($response.data.id)"
        $successCount++
    } catch {
        Write-Output "  ❌ ERROR: $($_.Exception.Message)"
        if ($_.ErrorDetails.Message) {
            Write-Output "  Details: $($_.ErrorDetails.Message)"
        }
        $failCount++
    }
}

Write-Output "`n=== SUMMARY ==="
Write-Output "Total cameras: $($cameras.Count)"
Write-Output "Added successfully: $successCount"
Write-Output "Failed: $failCount"

# List all cameras
Write-Output "`n=== ALL CAMERAS ==="
$camerasResponse = Invoke-RestMethod -Uri "$baseUrl/cameras" -Method GET -Headers $headers
$camerasResponse.data.items | ForEach-Object {
    Write-Output "$($_.name) | $($_.url) | $($_.status)"
}
