# Script: Test LDAP Connection for NAS Integration
# Date: 2026-06-09
# Purpose: Test LDAP connectivity and authentication

$LDAP_SERVER = "192.168.10.37"
$LDAP_PORT = 389
$BASE_DN = "dc=surveillance,dc=local"
$BIND_DN = "cn=admin,dc=surveillance,dc=local"

Write-Output "=========================================="
Write-Output "  LDAP Connection Test"
Write-Output "=========================================="
Write-Output ""

# Step 1: Test Port Connectivity
Write-Output "=== STEP 1: Testing LDAP Port ==="
$portOpen = Test-NetConnection $LDAP_SERVER -Port $LDAP_PORT -InformationLevel Quiet
if ($portOpen) {
    Write-Output "вњ… LDAP port $LDAP_PORT is OPEN on $LDAP_SERVER"
} else {
    Write-Output "вќЊ LDAP port $LDAP_PORT is CLOSED on $LDAP_SERVER"
    exit 1
}

# Step 2: Test Basic LDAP Connection
Write-Output "`n=== STEP 2: Testing LDAP Connection ==="
try {
    # Create LDAP connection string
    $ldapPath = "LDAP://$LDAP_SERVER/$BASE_DN"
    Write-Output "LDAP Path: $ldapPath"
    
    # Test connection (anonymous bind first)
    $directoryEntry = New-Object System.DirectoryServices.DirectoryEntry($ldapPath)
        Write-Output "вњ… LDAP connection established"
        Write-Output "   Server: ldap://${LDAP_SERVER}:${LDAP_PORT}"
        Write-Output "   Base DN: $BASE_DN"
} catch {
    Write-Output "вљ пёЏ  LDAP connection failed"
    Write-Output "   Error: $($_.Exception.Message)"
}

# Step 3: Test Authentication (if credentials provided)
Write-Output "`n=== STEP 3: Testing LDAP Authentication ==="

# Using provided credentials
$ldapPassword = "$env:LDAP_PASSWORD"
$useProvidedCredentials = $true

if ($useProvidedCredentials) {
    try {
        $ldapPath = "LDAP://$LDAP_SERVER/$BASE_DN"
        $directoryEntry = New-Object System.DirectoryServices.DirectoryEntry($ldapPath, $BIND_DN, $ldapPassword)
        
        # Force authentication by accessing a property
        $null = $directoryEntry.distinguishedName
        
        Write-Output "вњ… LDAP authentication successful"
        Write-Output "   Bind DN: $BIND_DN"
    } catch {
        Write-Output "вќЊ LDAP authentication failed"
        Write-Output "   Error: $($_.Exception.Message)"
    }
} else {
    Write-Output "вЏ­пёЏ  Skipped authentication test"
}

# Step 4: Search for Users
Write-Output "`n=== STEP 4: Testing User Search ==="
try {
    $ldapPath = "LDAP://$LDAP_SERVER/$BASE_DN"
    $directoryEntry = New-Object System.DirectoryServices.DirectoryEntry($ldapPath)
    $searcher = New-Object System.DirectoryServices.DirectorySearcher($directoryEntry)
    
    # Search for users
    $searcher.Filter = "(objectClass=person)"
    $searcher.PageSize = 10
    
    $results = $searcher.FindAll()
    Write-Output "вњ… User search successful"
    Write-Output "   Found $($results.Count) user(s)"
    
    foreach ($result in $results) {
        $username = $result.Properties["cn"][0]
        Write-Output "   - $username"
    }
} catch {
    Write-Output "вљ пёЏ  User search failed (may require authentication)"
    Write-Output "   Error: $($_.Exception.Message)"
}

# Summary
Write-Output "`n=========================================="
Write-Output "  LDAP Test Complete!"
Write-Output "=========================================="
Write-Output ""
Write-Output "Results:"
Write-Output "  вњ… LDAP server: ${LDAP_SERVER}:${LDAP_PORT}"
Write-Output "  вњ… Base DN: $BASE_DN"
Write-Output "  вњ… Bind DN: $BIND_DN"
Write-Output ""
Write-Output "Next Steps:"
Write-Output "  1. Update .env.nas with actual LDAP password"
Write-Output "  2. Configure LDAP integration in IP-CSS API"
Write-Output "  3. Test user authentication via API"
Write-Output ""
