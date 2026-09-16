# Script: Configure NAS Recording
# Date: 2026-06-09
# Purpose: Configure NAS storage paths and recording settings

$nasHost = $env:NAS_PRIMARY_HOST ?: "192.168.10.37"
$nasShare = "/storage/recordings"
$localBase = "data/recordings"

Write-Output "=========================================="
Write-Output "  Configure NAS Recording"
Write-Output "=========================================="
Write-Output ""

Write-Output "NAS Configuration:"
Write-Output "  Host: $nasHost"
Write-Output "  Share: $nasShare"
Write-Output "  Local Base: $localBase"
Write-Output ""

# Check if NAS is accessible
Write-Output "=== Step 1: Check NAS Connectivity ==="
$nasOnline = Test-Connection $nasHost -Count 2 -Quiet -ErrorAction SilentlyContinue

if ($nasOnline) {
    Write-Output "✅ NAS $nasHost is ONLINE"
} else {
    Write-Output "❌ NAS $nasHost is OFFLINE"
    exit 1
}

# Create local directory structure
Write-Output "`n=== Step 2: Create Local Directory Structure ==="
$directories = @(
    "$localBase/cameras",
    "$localBase/cameras/17",
    "$localBase/cameras/20",
    "$localBase/cameras/21",
    "$localBase/cameras/22",
    "$localBase/cameras/23",
    "$localBase/cameras/24",
    "$localBase/cameras/26",
    "$localBase/temp",
    "$localBase/archive"
)

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Output "  Created: $dir"
    } else {
        Write-Output "  Exists: $dir"
    }
}

# Test NFS mount (if enabled)
Write-Output "`n=== Step 3: Test NFS Mount ==="
$nfsEnabled = $env:NFS_ENABLED -eq "true"

if ($nfsEnabled) {
    Write-Output "NFS mount is enabled"
    Write-Output "Checking NAS share accessibility..."
    
    # Try to access NAS share via SMB
    $smbPath = "\\$nasHost\storage"
    
    if (Test-Path $smbPath) {
        Write-Output "✅ SMB share accessible: $smbPath"
        
        # Create NAS recording directories
        Write-Output "`nCreating NAS directories..."
        $cameras = @(17, 20, 21, 22, 23, 24, 26)
        
        foreach ($cameraId in $cameras) {
            $cameraPath = "$smbPath\recordings\camera_$cameraId"
            if (-not (Test-Path $cameraPath)) {
                New-Item -ItemType Directory -Path $cameraPath -Force | Out-Null
                Write-Output "  Created: $cameraPath"
            } else {
                Write-Output "  Exists: $cameraPath"
            }
        }
    } else {
        Write-Output "⚠️  SMB share not accessible (may need to mount manually)"
        Write-Output "  Command: net use Z: \\$nasHost\storage /user:admin password"
    }
} else {
    Write-Output "ℹ️  NFS mount disabled - using local storage"
    Write-Output "  To enable, set NFS_ENABLED=true and mount NAS"
}

# Create configuration file
Write-Output "`n=== Step 4: Create NAS Recording Configuration ==="

$nasConfig = @{
    nasEnabled = $nfsEnabled
    nasHost = $nasHost
    nasSharePath = $nasShare
    localBasePath = $localBase
    cameras = @(
        @{id = "3f723935-6057-455f-99c3-4fcd8cb107a0"; number = 17; ip = "192.168.10.17"},
        @{id = "62fb2974-c2d7-4880-af30-9165c96e8a8d"; number = 20; ip = "192.168.10.20"},
        @{id = "d854beef-38ae-4490-8b31-926bc8149a4b"; number = 21; ip = "192.168.10.21"},
        @{id = "895061a2-14d2-4500-ac74-24ecde706499"; number = 22; ip = "192.168.10.22"},
        @{id = "0d32d3c8-a97b-4df8-a8f4-6944177d43f1"; number = 23; ip = "192.168.10.23"},
        @{id = "ae30bc74-5231-456a-9bd1-5baa099358ff"; number = 24; ip = "192.168.10.24"},
        @{id = "d0da42c5-596b-4cea-9593-f28ef34537dd"; number = 26; ip = "192.168.10.26"}
    )
    recording = @{
        mode = "continuous"
        retentionDays = 30
        fps = 15
        resolution = "1920x1080"
        codec = "h264"
    }
    storage = @{
        primary = $nasHost
        fallback = "local"
        thresholdPercent = 85
    }
}

$nasConfig | ConvertTo-Json -Depth 10 | Out-File "config/nas_recording.json" -Encoding UTF8
Write-Output "✅ Created: config/nas_recording.json"

# Summary
Write-Output "`n=========================================="
Write-Output "  Configuration Summary"
Write-Output "=========================================="
Write-Output ""
Write-Output "NAS Recording Configuration:"
Write-Output "  NAS Host: $nasHost"
Write-Output "  NAS Share: $nasShare"
Write-Output "  Local Base: $localBase"
Write-Output "  NFS Enabled: $nfsEnabled"
Write-Output ""
Write-Output "Cameras Configured: $($nasConfig.cameras.Count)"
Write-Output "Recording Mode: $($nasConfig.recording.mode)"
Write-Output "Retention: $($nasConfig.recording.retentionDays) days"
Write-Output ""
Write-Output "Next Steps:"
Write-Output "  1. If using NAS: Mount NFS share on host"
Write-Output "  2. Update docker-compose.yml with NFS volume"
Write-Output "  3. Restart containers: docker-compose restart"
Write-Output "  4. Test recording to NAS"
Write-Output ""
Write-Output "=========================================="
