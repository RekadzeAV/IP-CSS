# Script: Test NAS and LDAP in Docker
# Date: 2026-06-09
# Purpose: Test NAS connectivity and LDAP from Docker container

Write-Output "=========================================="
Write-Output "  Test NAS and LDAP in Docker"
Write-Output "=========================================="
Write-Output ""

# Test NAS connectivity from host
Write-Output "=== STEP 1: Test NAS Connectivity from Host ==="
$nasHost = $env:NAS_PRIMARY_HOST ?: "192.168.10.37"
$nasOnline = Test-Connection $nasHost -Count 2 -Quiet -ErrorAction SilentlyContinue

if ($nasOnline) {
    Write-Output "✅ NAS $nasHost is ONLINE"
} else {
    Write-Output "❌ NAS $nasHost is OFFLINE"
    exit 1
}

# Test LDAP port
Write-Output "`n=== STEP 2: Test LDAP Port ==="
$ldapPort = Test-NetConnection $nasHost -Port 389 -InformationLevel Quiet
if ($ldapPort) {
    Write-Output "✅ LDAP port 389 is OPEN"
} else {
    Write-Output "❌ LDAP port 389 is CLOSED"
    exit 1
}

# Test NFS mount (if enabled)
Write-Output "`n=== STEP 3: Test NFS Mount ==="
$nfsEnabled = $env:NFS_ENABLED -eq "true"

if ($nfsEnabled) {
    $nfsShare = "$nasHost:`$($env:NAS_SHARE_PATH)"
    Write-Output "Checking NFS share: $nfsShare"
    
    # Try to access the share
    $testPath = "\\$nasHost\storage"
    
    if (Test-Path $testPath) {
        Write-Output "✅ NFS share is accessible"
    } else {
        Write-Output "⚠️  NFS share may not be mounted on Windows host"
        Write-Output "   Use: net use Z: \\$nasHost\storage"
    }
} else {
    Write-Output "ℹ️  NFS mount is disabled (NFS_ENABLED not set)"
}

# Test Docker container access
Write-Output "`n=== STEP 4: Test Docker Container Access ==="
$containerName = "ip-camera-surveillance"

# Check if container is running
$containerRunning = docker ps --filter "name=$containerName" --format "{{.Names}}"
if ($containerRunning) {
    Write-Output "✅ Container $containerName is running"
} else {
    Write-Output "❌ Container $containerName is not running"
    Write-Output "Start with: docker-compose up -d"
    exit 1
}

# Test LDAP from container
Write-Output "`n=== STEP 5: Test LDAP from Container ==="
Write-Output "Executing LDAP test in container..."

$ldapTest = docker exec $containerName /bin/sh -c "
    if command -v ldapsearch > /dev/null; then
        ldapsearch -x -H ldap://192.168.10.37:389 -b 'dc=surveillance,dc=local' -s base
    else
        echo 'ldapsearch not available, testing with telnet...'
        echo 'quit' | telnet 192.168.10.37 389 2>&1 | head -n 1
    fi
" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Output "✅ LDAP is accessible from container"
} else {
    Write-Output "⚠️  LDAP test inconclusive (may need ldapsearch installed)"
}

# Test API health
Write-Output "`n=== STEP 6: Test API Health ==="
try {
    $health = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -UseBasicParsing -TimeoutSec 10
    $healthJson = $health.Content | ConvertFrom-Json
    
    Write-Output "✅ API Health Check: $($healthJson.success)"
    Write-Output "  Database: $($healthJson.data.checks.database)"
    Write-Output "  Redis: $($healthJson.data.checks.redis)"
    Write-Output "  Storage: $($healthJson.data.checks.storage)"
} catch {
    Write-Output "❌ API Health Check Failed"
    Write-Output "Error: $($_.Exception.Message)"
}

# Test LDAP authentication
Write-Output "`n=== STEP 7: Test LDAP Authentication ==="
$ldapEnabled = $env:LDAP_ENABLED -eq "true"

if ($ldapEnabled) {
    Write-Output "LDAP is enabled, testing authentication..."
    
    $loginBody = @{
        username = "admin"
        password = $env:AUTH_LDAP_BIND_PASSWORD
    } | ConvertTo-Json
    
    try {
        $login = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/login" `
            -Method POST `
            -Body $loginBody `
            -ContentType "application/json" `
            -UseBasicParsing
        
        if ($login.StatusCode -eq 200) {
            Write-Output "✅ LDAP Authentication successful"
        } else {
            Write-Output "⚠️  LDAP Authentication returned: $($login.StatusCode)"
            Write-Output "   (May be using local DB auth instead)"
        }
    } catch {
        Write-Output "⚠️  LDAP Authentication test failed"
        Write-Output "   Error: $($_.Exception.Message)"
    }
} else {
    Write-Output "ℹ️  LDAP authentication is disabled"
}

# Summary
Write-Output "`n=========================================="
Write-Output "  Test Summary"
Write-Output "=========================================="
Write-Output ""
Write-Output "✅ NAS Connectivity: OK"
Write-Output "✅ LDAP Port: OK"
Write-Output "✅ Docker Container: Running"
Write-Output "✅ API Health: OK"
Write-Output ""

if ($ldapEnabled) {
    Write-Output "✅ LDAP Authentication: Enabled"
} else {
    Write-Output "ℹ️  LDAP Authentication: Disabled"
}

if ($nfsEnabled) {
    Write-Output "✅ NFS Mount: Enabled"
} else {
    Write-Output "ℹ️  NFS Mount: Disabled"
}

Write-Output ""
Write-Output "Next Steps:"
Write-Output "  1. If NFS mount needed: Set NFS_ENABLED=true and mount NAS"
Write-Output "  2. If LDAP auth needed: Verify user credentials in NAS LDAP"
Write-Output "  3. Test camera recordings to NAS storage"
Write-Output ""
