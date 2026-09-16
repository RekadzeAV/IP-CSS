# Script: Update RTSP URLs in Database
# Date: 2026-06-09
# Purpose: Update camera RTSP URLs with credentials in PostgreSQL database

$host = "localhost"
$port = "5432"
$database = "surveillance"
$user = "surveillance"
$password = $env:DB_PASSWORD

$cameras = @(
    @{Id = "3f723935-6057-455f-99c3-4fcd8cb107a0"; Name = "Camera 17"; IP = "192.168.10.17"},
    @{Id = "62fb2974-c2d7-4880-af30-9165c96e8a8d"; Name = "Camera 20"; IP = "192.168.10.20"},
    @{Id = "d854beef-38ae-4490-8b31-926bc8149a4b"; Name = "Camera 21"; IP = "192.168.10.21"},
    @{Id = "895061a2-14d2-4500-ac74-24ecde706499"; Name = "Camera 22"; IP = "192.168.10.22"},
    @{Id = "0d32d3c8-a97b-4df8-a8f4-6944177d43f1"; Name = "Camera 23"; IP = "192.168.10.23"},
    @{Id = "ae30bc74-5231-456a-9bd1-5baa099358ff"; Name = "Camera 24"; IP = "192.168.10.24"},
    @{Id = "d0da42c5-596b-4cea-9593-f28ef34537dd"; Name = "Camera 26"; IP = "192.168.10.26"}
)

Write-Output "=========================================="
Write-Output "  Update RTSP URLs in Database"
Write-Output "=========================================="
Write-Output ""

# Check if DB_PASSWORD is set
if ([string]::IsNullOrWhiteSpace($password)) {
    Write-Output "❌ DB_PASSWORD environment variable is not set!"
    Write-Output "Please set it before running this script:"
    Write-Output '  $env:DB_PASSWORD="your_password"'
    exit 1
}

Write-Output "Database: $database@$host:$port"
Write-Output "User: $user"
Write-Output ""

# Build RTSP URL update queries
$queries = @()
foreach ($camera in $cameras) {
    $rtspUrl = "rtsp://survival:1234567890qazxs@$($camera.IP):554/stream1"
    $username = "survival"
    $password = "1234567890qazxs"
    
    $query = @"
UPDATE camera
SET 
    url = '$rtspUrl',
    username = '$username',
    password = '$password',
    updated_at = CURRENT_TIMESTAMP
WHERE id = '$($camera.Id)';
"@
    
    $queries += $query
}

# Execute queries
Write-Output "=== Updating Camera RTSP URLs ==="
$successCount = 0
$failCount = 0

foreach ($camera in $cameras) {
    $rtspUrl = "rtsp://survival:1234567890qazxs@$($camera.IP):554/stream1"
    
    Write-Output "`nUpdating $($camera.Name) ($($camera.IP))..."
    Write-Output "  RTSP URL: $rtspUrl"
    
    # Execute update query using psql
    $psqlCommand = "PGPASSWORD='$password' psql -h $host -p $port -U $user -d $database -c \""
    $psqlCommand += "UPDATE camera SET url = 'rtsp://survival:1234567890qazxs@$($camera.IP):554/stream1', "
    $psqlCommand += "username = 'survival', password = '1234567890qazxs', updated_at = CURRENT_TIMESTAMP "
    $psqlCommand += "WHERE id = '$($camera.Id)';\""
    
    try {
        $result = Invoke-Expression $psqlCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Output "  ✅ Updated successfully"
            $successCount++
        } else {
            Write-Output "  ❌ Update failed: $result"
            $failCount++
        }
    } catch {
        Write-Output "  ❌ Error: $($_.Exception.Message)"
        $failCount++
    }
}

# Summary
Write-Output "`n=========================================="
Write-Output "  Update Summary"
Write-Output "=========================================="
Write-Output ""
Write-Output "Total Cameras: $($cameras.Count)"
Write-Output "Updated: $successCount"
Write-Output "Failed: $failCount"
Write-Output ""

if ($failCount -eq 0) {
    Write-Output "✅ All camera RTSP URLs updated successfully!"
    
    # Verify updates
    Write-Output "`n=== Verification ==="
    $verifyQuery = "SELECT id, name, url, username FROM camera WHERE id IN ('" + ($cameras.Id -join "','") + "');"
    
    $verifyCommand = "PGPASSWORD='$password' psql -h $host -p $port -U $user -d $database -c \"" + $verifyQuery + "\""
    
    try {
        $verifyResult = Invoke-Expression $verifyCommand 2>&1
        Write-Output $verifyResult
    } catch {
        Write-Output "⚠️  Verification query failed: $($_.Exception.Message)"
    }
} else {
    Write-Output "⚠️  Some updates failed. Please check the errors above."
}

Write-Output ""
Write-Output "=========================================="
