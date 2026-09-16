#!/usr/bin/env pwsh
# Test Certificate Pinning Configuration
# Проверяет корректность настройки certificate pinning

param(
    [string]$BaseUrl = "https://localhost:8443",
    [string]$PinFile = "config/certificate-pins.json",
    [switch]$GeneratePins,
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Test Certificate Pinning Configuration

Usage:
  .\scripts\test-certificate-pinning.ps1 [-GeneratePins]

Options:
  -BaseUrl    HTTPS URL (default: https://localhost:8443)
  -PinFile    Certificate pins file (default: config/certificate-pins.json)
  -GeneratePins Generate certificate pins
  -ShowHelp   Show this help

Examples:
  # Test certificate pinning
  .\scripts\test-certificate-pinning.ps1

  # Generate certificate pins
  .\scripts\test-certificate-pinning.ps1 -GeneratePins

"@
    exit 0
}

Write-Host "Certificate Pinning Test" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
Write-Host "Base URL: $BaseUrl" -ForegroundColor White
Write-Host "Pin File: $PinFile" -ForegroundColor White
Write-Host ""

$results = @{
    passed = 0
    failed = 0
    warnings = 0
}

if ($GeneratePins) {
    Write-Host "Generating Certificate Pins" -ForegroundColor Yellow
    Write-Host ""
    
    try {
        $host = $BaseUrl.Replace("https://", "").Split(":")[0]
        $port = if ($BaseUrl.Contains(":")) { $BaseUrl.Split(":")[2] } else { 443 }
        
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.Connect($host, $port)
        
        $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, { $true }, $false)
        $sslStream.AuthenticateAsClient($host)
        
        $cert = $sslStream.RemoteCertificate
        $certBytes = $cert.GetRawCertData()
        $certBase64 = [Convert]::ToBase64String($certBytes)
        
        $sha256 = [System.Security.Cryptography.HashAlgorithm]::Create("SHA256")
        $hash = $sha256.ComputeHash($certBytes)
        $pin = [Convert]::ToBase64String($hash)
        
        $pinConfig = @{
            host = $host
            port = $port
            certificate = @{
                subject = $cert.Subject
                issuer = $cert.Issuer
                notBefore = $cert.NotBefore.ToString("o")
                notAfter = $cert.NotAfter.ToString("o")
                thumbprint = $cert.GetCertHashString()
                pin = $pin
            }
            generated = (Get-Date -Format "o")
        }
        
        $pinJson = $pinConfig | ConvertTo-Json -Depth 5
        $pinFileDir = Split-Path -Parent $PinFile
        if (!(Test-Path $pinFileDir)) {
            New-Item -ItemType Directory -Path $pinFileDir -Force | Out-Null
        }
        Set-Content -Path $PinFile -Value $pinJson -Encoding UTF8
        
        Write-Host "  SUCCESS: Certificate pins generated" -ForegroundColor Green
        Write-Host "  Pin file: $PinFile" -ForegroundColor Gray
        Write-Host "  Pin: $pin" -ForegroundColor Gray
        
        $sslStream.Close()
        $tcpClient.Close()
        
        $results.passed++
    } catch {
        Write-Host "  FAILED: $_" -ForegroundColor Red
        $results.failed++
    }
    
    Write-Host ""
} else {
    # Test 1: Pin File Exists
    Write-Host "Test 1: Pin File Exists" -ForegroundColor Yellow
    if (Test-Path $PinFile) {
        Write-Host "  SUCCESS: Pin file found: $PinFile" -ForegroundColor Green
        $results.passed++
    } else {
        Write-Host "  FAILED: Pin file not found: $PinFile" -ForegroundColor Red
        Write-Host "  Run with -GeneratePins to create pin file" -ForegroundColor Gray
        $results.failed++
    }
    
    # Test 2: Load Pin Configuration
    Write-Host "Test 2: Load Pin Configuration" -ForegroundColor Yellow
    if (Test-Path $PinFile) {
        try {
            $pinConfig = Get-Content $PinFile -Raw | ConvertFrom-Json
            
            if ($pinConfig.pin) {
                Write-Host "  SUCCESS: Pin configuration loaded" -ForegroundColor Green
                Write-Host "  Host: $($pinConfig.host)" -ForegroundColor Gray
                Write-Host "  Pin: $($pinConfig.pin.Substring(0, 20))..." -ForegroundColor Gray
                $results.passed++
            } else {
                Write-Host "  FAILED: Invalid pin configuration" -ForegroundColor Red
                $results.failed++
            }
        } catch {
            Write-Host "  FAILED: $_" -ForegroundColor Red
            $results.failed++
        }
    }
    
    # Test 3: Verify Certificate Against Pins
    Write-Host "Test 3: Verify Certificate Against Pins" -ForegroundColor Yellow
    if (Test-Path $PinFile) {
        try {
            $pinConfig = Get-Content $PinFile -Raw | ConvertFrom-Json
            $host = $BaseUrl.Replace("https://", "").Split(":")[0]
            $port = if ($BaseUrl.Contains(":")) { $BaseUrl.Split(":")[2] } else { 443 }
            
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, { $true }, $false)
            $sslStream.AuthenticateAsClient($host)
            
            $cert = $sslStream.RemoteCertificate
            $sha256 = [System.Security.Cryptography.HashAlgorithm]::Create("SHA256")
            $hash = $sha256.ComputeHash($cert.GetRawCertData())
            $currentPin = [Convert]::ToBase64String($hash)
            
            if ($currentPin -eq $pinConfig.pin) {
                Write-Host "  SUCCESS: Certificate pin matches" -ForegroundColor Green
                $results.passed++
            } else {
                Write-Host "  FAILED: Certificate pin mismatch" -ForegroundColor Red
                Write-Host "  Expected: $($pinConfig.pin.Substring(0, 20))..." -ForegroundColor Gray
                Write-Host "  Current:  $($currentPin.Substring(0, 20))..." -ForegroundColor Gray
                $results.failed++
            }
            
            $sslStream.Close()
            $tcpClient.Close()
        } catch {
            Write-Host "  FAILED: $_" -ForegroundColor Red
            $results.failed++
        }
    }
    
    # Test 4: Certificate Expiry
    Write-Host "Test 4: Certificate Expiry" -ForegroundColor Yellow
    if (Test-Path $PinFile) {
        try {
            $pinConfig = Get-Content $PinFile -Raw | ConvertFrom-Json
            
            if ($pinConfig.certificate.notAfter) {
                $expiry = [DateTime]::Parse($pinConfig.certificate.notAfter)
                $daysUntilExpiry = ($expiry - (Get-Date)).Days
                
                Write-Host "  Expiry: $($expiry.ToString('yyyy-MM-dd'))" -ForegroundColor Gray
                Write-Host "  Days until expiry: $daysUntilExpiry" -ForegroundColor Gray
                
                if ($daysUntilExpiry -gt 30) {
                    Write-Host "  SUCCESS: Certificate valid for more than 30 days" -ForegroundColor Green
                    $results.passed++
                } elseif ($daysUntilExpiry -gt 7) {
                    Write-Host "  WARNING: Certificate expires in less than 30 days" -ForegroundColor Yellow
                    $results.warnings++
                } else {
                    Write-Host "  FAILED: Certificate expires in less than 7 days" -ForegroundColor Red
                    $results.failed++
                }
            } else {
                Write-Host "  WARNING: Certificate expiry date not found" -ForegroundColor Yellow
                $results.warnings++
            }
        } catch {
            Write-Host "  WARNING: Could not verify expiry: $_" -ForegroundColor Yellow
            $results.warnings++
        }
    }
}

# Summary
Write-Host ""
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "Passed: $($results.passed)" -ForegroundColor Green
Write-Host "Failed: $($results.failed)" -ForegroundColor $(if ($results.failed -gt 0) { "Red" } else { "Green" })
Write-Host "Warnings: $($results.warnings)" -ForegroundColor Yellow
Write-Host ""

if ($results.failed -gt 0) {
    Write-Host "FAILED: Certificate pinning test completed with errors" -ForegroundColor Red
    exit 1
} else {
    Write-Host "SUCCESS: Certificate pinning test passed" -ForegroundColor Green
    exit 0
}
