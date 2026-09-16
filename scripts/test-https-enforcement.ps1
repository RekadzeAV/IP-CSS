#!/usr/bin/env pwsh
# Test HTTPS Enforcement Configuration
# Проверяет корректность настройки HTTPS и редиректов

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$HttpsUrl = "https://localhost:8443",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Test HTTPS Enforcement Configuration

Usage:
  .\scripts\test-https-enforcement.ps1 [-BaseUrl <url>] [-HttpsUrl <url>]

Options:
  -BaseUrl    HTTP URL (default: http://localhost:8080)
  -HttpsUrl   HTTPS URL (default: https://localhost:8443)
  -ShowHelp   Show this help

Examples:
  # Test with defaults
  .\scripts\test-https-enforcement.ps1

  # Test with custom URLs
  .\scripts\test-https-enforcement.ps1 -BaseUrl http://api.example.com -HttpsUrl https://api.example.com

"@
    exit 0
}

Write-Host "HTTPS Enforcement Test" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
Write-Host "HTTP Base: $BaseUrl" -ForegroundColor White
Write-Host "HTTPS Base: $HttpsUrl" -ForegroundColor White
Write-Host ""

$results = @{
    passed = 0
    failed = 0
    warnings = 0
}

# Test 1: HTTP Redirect
Write-Host "Test 1: HTTP to HTTPS Redirect" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/v1/health" -MaximumRedirection 0 -ErrorAction SilentlyContinue
    
    if ($response.StatusCode -eq 301 -or $response.StatusCode -eq 302) {
        Write-Host "  SUCCESS: HTTP redirects to HTTPS (Status: $($response.StatusCode))" -ForegroundColor Green
        $results.passed++
    } else {
        Write-Host "  WARNING: No redirect detected (Status: $($response.StatusCode))" -ForegroundColor Yellow
        $results.warnings++
    }
} catch {
    if ($_.Exception.Response.StatusCode -eq 301 -or $_.Exception.Response.StatusCode -eq 302) {
        Write-Host "  SUCCESS: HTTP redirects to HTTPS" -ForegroundColor Green
        $results.passed++
    } else {
        Write-Host "  FAILED: $_" -ForegroundColor Red
        $results.failed++
    }
}

# Test 2: HTTPS Endpoint
Write-Host "Test 2: HTTPS Endpoint Accessibility" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$HttpsUrl/api/v1/health" -SkipCertificateCheck -ErrorAction Stop
    
    if ($response.StatusCode -eq 200) {
        Write-Host "  SUCCESS: HTTPS endpoint accessible" -ForegroundColor Green
        $results.passed++
    } else {
        Write-Host "  FAILED: Unexpected status code: $($response.StatusCode)" -ForegroundColor Red
        $results.failed++
    }
} catch {
    Write-Host "  FAILED: $_" -ForegroundColor Red
    $results.failed++
}

# Test 3: Security Headers
Write-Host "Test 3: Security Headers" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$HttpsUrl/api/v1/health" -SkipCertificateCheck -ErrorAction Stop
    $headers = $response.Headers
    
    $requiredHeaders = @{
        "Strict-Transport-Security" = "HSTS header present"
        "X-Content-Type-Options" = "X-Content-Type-Options present"
        "X-Frame-Options" = "X-Frame-Options present"
        "X-XSS-Protection" = "X-XSS-Protection present"
    }
    
    $headersOk = $true
    foreach ($header in $requiredHeaders.Keys) {
        if ($headers[$header]) {
            Write-Host "    ✓ $header: $($headers[$header])" -ForegroundColor Green
        } else {
            Write-Host "    ✗ $header: MISSING" -ForegroundColor Red
            $headersOk = $false
        }
    }
    
    if ($headersOk) {
        Write-Host "  SUCCESS: All security headers present" -ForegroundColor Green
        $results.passed++
    } else {
        Write-Host "  WARNING: Some security headers missing" -ForegroundColor Yellow
        $results.warnings++
    }
} catch {
    Write-Host "  FAILED: $_" -ForegroundColor Red
    $results.failed++
}

# Test 4: SSL/TLS Configuration
Write-Host "Test 4: SSL/TLS Configuration" -ForegroundColor Yellow
try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, { $true }, $false)
    
    $tcpClient.Connect($HttpsUrl.Replace("https://", "").Split(":")[0], 8443)
    $sslStream.AuthenticateAsClient($HttpsUrl.Replace("https://", "").Split(":")[0])
    
    $protocol = $sslStream.SslProtocol
    Write-Host "  Protocol: $protocol" -ForegroundColor Gray
    
    if ($protocol -match "Tls12|Tls13") {
        Write-Host "  SUCCESS: Secure protocol used" -ForegroundColor Green
        $results.passed++
    } else {
        Write-Host "  WARNING: Older protocol used: $protocol" -ForegroundColor Yellow
        $results.warnings++
    }
    
    $sslStream.Close()
    $tcpClient.Close()
} catch {
    Write-Host "  WARNING: Could not verify SSL/TLS: $_" -ForegroundColor Yellow
    $results.warnings++
}

# Summary
Write-Host ""
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "Passed: $($results.passed)" -ForegroundColor Green
Write-Host "Failed: $($results.failed)" -ForegroundColor $(if ($results.failed -gt 0) { "Red" } else { "Green" })
Write-Host "Warnings: $($results.warnings)" -ForegroundColor Yellow
Write-Host ""

if ($results.failed -gt 0) {
    Write-Host "FAILED: HTTPS enforcement test completed with errors" -ForegroundColor Red
    exit 1
} else {
    Write-Host "SUCCESS: HTTPS enforcement test passed" -ForegroundColor Green
    exit 0
}
