# Script: NAS Integration Setup for IP-CSS
# Date: 2026-06-09
# Purpose: Configure NAS storage and LDAP integration

$NAS_IP = "192.168.10.37"
$LDAP_BASE_DN = "dc=surveillance,dc=local"
$LDAP_BIND_DN = "cn=admin,dc=surveillance,dc=local"
$RECORDINGS_PATH = "/storage/recordings"

Write-Output "=========================================="
Write-Output "  NAS Integration Setup for IP-CSS"
Write-Output "=========================================="
Write-Output ""

# Step 1: Test NAS Connectivity
Write-Output "=== STEP 1: Testing NAS Connectivity ==="
$nasOnline = Test-Connection $NAS_IP -Count 2 -Quiet -ErrorAction SilentlyContinue
if ($nasOnline) {
    Write-Output "✅ NAS $NAS_IP is ONLINE"
} else {
    Write-Output "❌ NAS $NAS_IP is OFFLINE"
    exit 1
}

# Step 2: Test Port Availability
Write-Output "`n=== STEP 2: Testing Port Availability ==="
$ports = @{
    "SMB" = 445
    "LDAP" = 389
    "SSH" = 22
}

foreach ($portName in $ports.Keys) {
    $port = $ports[$portName]
    $result = Test-NetConnection $NAS_IP -Port $port -InformationLevel Quiet
    if ($result) {
        Write-Output "✅ $portName ($port): OPEN"
    } else {
        Write-Output "❌ $portName ($port): CLOSED"
    }
}

# Step 3: Test LDAP Connectivity
Write-Output "`n=== STEP 3: Testing LDAP Connectivity ==="
try {
    $ldapConnection = New-Object System.DirectoryServices.DirectoryEntry("LDAP://$NAS_IP/$LDAP_BASE_DN")
    Write-Output "✅ LDAP connection successful"
    Write-Output "   Base DN: $LDAP_BASE_DN"
    Write-Output "   Server: ldap://$NAS_IP:389"
} catch {
    Write-Output "⚠️  LDAP connection test failed (may require authentication)"
    Write-Output "   Error: $($_.Exception.Message)"
}

# Step 4: Create NAS Configuration File
Write-Output "`n=== STEP 4: Creating NAS Configuration ==="

$nasConfig = @{
    nas = @{
        enabled = $true
        hosts = @(
            "192.168.10.37",
            "192.168.10.38",
            "192.168.10.39",
            "192.168.10.40"
        )
        primary_host = "192.168.10.37"
        protocol = "NFS"
        share_path = "/storage/recordings"
        mount_point = "/data/recordings"
    }
    ldap = @{
        enabled = $true
        server = "ldap://$NAS_IP:389"
        base_dn = $LDAP_BASE_DN
        bind_dn = $LDAP_BIND_DN
        user_search_filter = "(uid=%(user)s)"
        group_search_filter = "(member=%(user_dn)s)"
    }
    storage = @{
        first_contour = @{
            cameras = @(
                "192.168.10.17",
                "192.168.10.20",
                "192.168.10.21",
                "192.168.10.22",
                "192.168.10.23",
                "192.168.10.24",
                "192.168.10.26"
            )
            retention_days = 30
            recording_mode = "continuous"
        }
    }
}

$nasConfig | ConvertTo-Json -Depth 10 | Out-File -FilePath "config/nas_integration.json" -Encoding UTF8
Write-Output "✅ Created config/nas_integration.json"

# Step 5: Create Environment Variables for Docker
Write-Output "`n=== STEP 5: Creating Environment Variables ==="

$envVars = @"
# NAS Integration Environment Variables
# Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

# NAS Configuration
NAS_PRIMARY_HOST=192.168.10.37
NAS_HOSTS=192.168.10.37,192.168.10.38,192.168.10.39,192.168.10.40
NAS_PROTOCOL=NFS
NAS_SHARE_PATH=/storage/recordings
NAS_MOUNT_POINT=/data/recordings

# LDAP Configuration
AUTH_LDAP_SERVER=ldap://192.168.10.37:389
AUTH_LDAP_BASE_DN=dc=surveillance,dc=local
AUTH_LDAP_BIND_DN=cn=admin,dc=surveillance,dc=local
AUTH_LDAP_BIND_PASSWORD=CHANGE_ME_IN_PRODUCTION
AUTH_LDAP_USER_SEARCH_FILTER=(uid=%(user)s)
AUTH_LDAP_GROUP_SEARCH_FILTER=(member=%(user_dn)s)

# Storage Configuration
STORAGE_FIRST_CONTOUR_CAMERAS=192.168.10.17,192.168.10.20,192.168.10.21,192.168.10.22,192.168.10.23,192.168.10.24,192.168.10.26
STORAGE_RETENTION_DAYS=30
STORAGE_RECORDING_MODE=continuous
"@

$envVars | Out-File -FilePath ".env.nas" -Encoding UTF8
Write-Output "✅ Created .env.nas"

# Step 6: Create Mount Script (Linux/Docker)
Write-Output "`n=== STEP 6: Creating Mount Script ==="

$mountScript = @"
#!/bin/bash
# NAS Mount Script for IP-CSS
# Date: $(Get-Date -Format "yyyy-MM-dd")

NAS_IP="192.168.10.37"
SHARE_PATH="/storage/recordings"
MOUNT_POINT="/data/recordings"

echo "=== NAS Mount Script ==="
echo "NAS: $NAS_IP"
echo "Share: $SHARE_PATH"
echo "Mount: $MOUNT_POINT"

# Create mount point if it doesn't exist
sudo mkdir -p $MOUNT_POINT

# Unmount if already mounted
if mountpoint -q $MOUNT_POINT; then
    echo "Unmounting existing mount..."
    sudo umount $MOUNT_POINT
fi

# Mount NFS share
echo "Mounting NFS share..."
sudo mount -t nfs $NAS_IP:$SHARE_PATH $MOUNT_POINT

# Verify mount
if mountpoint -q $MOUNT_POINT; then
    echo "✅ NAS mounted successfully"
    df -h $MOUNT_POINT
else
    echo "❌ Failed to mount NAS"
    exit 1
fi

# Set permissions
sudo chown -R 1000:1000 $MOUNT_POINT
sudo chmod -R 755 $MOUNT_POINT

echo "✅ NAS integration complete"
"@

$mountScript | Out-File -FilePath "scripts/mount_nas.sh" -Encoding UTF8
Write-Output "✅ Created scripts/mount_nas.sh"

# Step 7: Create Docker Volume Configuration
Write-Output "`n=== STEP 7: Creating Docker Volume Configuration ==="

$dockerComposeFragment = @"
# Add to docker-compose.yml under surveillance-api service:
volumes:
  - ./data/recordings:/data/recordings:rw
  # Or use NFS mount:
  # - nas-recordings:/data/recordings

volumes:
  nas-recordings:
    driver: local
    driver_opts:
      type: nfs
      o: addr=192.168.10.37,rw,nolock,hard,intr
      device: ":/storage/recordings"
"@

$dockerComposeFragment | Out-File -FilePath "docs/NAS_DOCKER_CONFIG.md" -Encoding UTF8
Write-Output "✅ Created docs/NAS_DOCKER_CONFIG.md"

# Summary
Write-Output "`n=========================================="
Write-Output "  NAS Integration Setup Complete!"
Write-Output "=========================================="
Write-Output ""
Write-Output "Created Files:"
Write-Output "  ✅ config/nas_integration.json"
Write-Output "  ✅ .env.nas"
Write-Output "  ✅ scripts/mount_nas.sh"
Write-Output "  ✅ docs/NAS_DOCKER_CONFIG.md"
Write-Output ""
Write-Output "Next Steps:"
Write-Output "  1. Review and update .env.nas with actual LDAP password"
Write-Output "  2. Run scripts/mount_nas.sh on the server"
Write-Output "  3. Update docker-compose.yml with volume configuration"
Write-Output "  4. Restart Docker containers"
Write-Output ""
Write-Output "Commands:"
Write-Output "  chmod +x scripts/mount_nas.sh"
Write-Output "  sudo ./scripts/mount_nas.sh"
Write-Output "  docker-compose down"
Write-Output "  docker-compose up -d"
Write-Output ""
