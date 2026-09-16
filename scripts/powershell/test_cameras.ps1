:# Test Camera API with Cookie Authentication

$loginUrl = "http://localhost:8080/api/v1/auth/login"
$camerasUrl = "http://localhost:8080/api/v1/cameras"

# Login
$loginBody = '{"username":"admin","password":"admin123"}'
$loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -ContentType "application/json" -Body $loginBody -SessionVariable session

Write-Output "=== LOGIN RESULT ==="
$loginResponse | ConvertTo-Json

# Get cookies from session
$uri = [System.Uri]::new($camerasUrl)
$cookies = $session.Cookies.GetCookies($uri)
Write-Output "`n=== COOKIES ==="
$cookies | ForEach-Object { Write-Output "$($_.Name) = $($_.Value.Substring(0, [Math]::Min(50, $_.Value.Length)))..." }

# Get cameras
$headers = @{
    Cookie = ($cookies | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join "; "
}

Write-Output "`n=== REQUESTING CAMERAS ==="
try {
    $camerasResponse = Invoke-RestMethod -Uri $camerasUrl -Method GET -Headers $headers
    Write-Output "SUCCESS!"
    $camerasResponse | ConvertTo-Json -Depth 5
} catch {
    Write-Output "ERROR: $($_.Exception.Message)"
    Write-Output "Status: $($_.Exception.Response.StatusCode)"
}
