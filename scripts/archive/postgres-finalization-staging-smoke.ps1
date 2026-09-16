# PostgreSQL finalization staging smoke script (1.5.6)
# РџСЂРѕРІРµСЂСЏРµС‚ РєР»СЋС‡РµРІС‹Рµ API СЃС†РµРЅР°СЂРёРё РїРѕСЃР»Рµ cutover:
# - health / readiness
# - login / refresh / logout
# - cameras / events / recordings (authorized GET)
# - database admin endpoints (health / migrations / pool stats)
#
# Usage:
#   .\scripts\postgres-finalization-staging-smoke.ps1 `
#      -BaseUrl "http://localhost:8080" `
#      -Username "admin" `
#      -Password "$env:ADMIN_PASSWORD" `
#      -OutputFile "diagnostics\postgres-finalization\staging-smoke-report.md"

[Diagnostics.CodeAnalysis.SuppressMessageAttribute('PSAvoidUsingPlainTextForPassword', '')]
param(
    [switch]$ShowHelp,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "$env:ADMIN_PASSWORD",
    [string]$OutputFile = "diagnostics\postgres-finalization\staging-smoke-report.md",
    [int]$TimeoutSec = 20,
    [switch]$ExitZeroOnReportFail
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "PostgreSQL finalization staging smoke (authorized API + DB admin endpoints)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\postgres-finalization-staging-smoke.ps1 -BaseUrl http://localhost:8080 -Username admin -Password <pwd>"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputFile  Markdown report path (repo-relative ok)"
    Write-Host "  -TimeoutSec  HTTP timeout per request"
    Write-Host "  -ExitZeroOnReportFail  Exit 0 even when Overall FAIL or API unreachable (legacy; not for CI)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  All checks passed, or -ExitZeroOnReportFail with report written"
    Write-Host "  1  API unreachable before checks, or Overall FAIL (default)"
    Write-Host "  Non-zero  Unhandled runtime error"
    exit 0
}

function New-ResultRow {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Expected,
        [string]$Actual
    )
    return [PSCustomObject]@{
        Name = $Name
        Passed = $Passed
        Expected = $Expected
        Actual = $Actual
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $params = @{
        Method = $Method
        Uri = $Url
        WebSession = $Session
        TimeoutSec = $TimeoutSec
        ErrorAction = "Stop"
    }

    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
    }
    if ($Headers.Count -gt 0) {
        $params["Headers"] = $Headers
    }

    try {
        $resp = Invoke-WebRequest @params
        $json = $null
        try {
            $json = $resp.Content | ConvertFrom-Json
        } catch {
            $json = $null
        }

        return [PSCustomObject]@{
            Ok = $true
            StatusCode = [int]$resp.StatusCode
            Content = $resp.Content
            Json = $json
            ErrorMessage = ""
        }
    } catch {
        $statusCode = 0
        $content = ""
        try {
            if ($_.Exception.Response) {
                $statusCode = [int]$_.Exception.Response.StatusCode.value__
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $content = $reader.ReadToEnd()
                }
            }
        } catch { }

        return [PSCustomObject]@{
            Ok = $false
            StatusCode = $statusCode
            Content = $content
            Json = $null
            ErrorMessage = $_.Exception.Message
        }
    }
}

function Format-Actual {
    param(
        [int]$StatusCode,
        [string]$ErrorMessage
    )
    if ($StatusCode -gt 0) {
        return "HTTP $StatusCode"
    }
    if ([string]::IsNullOrWhiteSpace($ErrorMessage)) {
        return "HTTP 0"
    }
    return ("HTTP 0 (" + $ErrorMessage.Replace("|", "/") + ")")
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutput = if ([System.IO.Path]::IsPathRooted($OutputFile)) { $OutputFile } else { Join-Path $projectRoot $OutputFile }
$outputDir = Split-Path -Parent $resolvedOutput
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$base = $BaseUrl.TrimEnd("/")
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$results = @()

Write-Host "Running PostgreSQL finalization staging smoke..." -ForegroundColor Cyan
Write-Host "Base URL: $base" -ForegroundColor Gray

# Preflight: API base reachability
$preflight = Invoke-Api -Method "GET" -Url "$base/api/v1/health" -Session $session
if ($preflight.StatusCode -eq 0) {
    $lines = @()
    $lines += "# PostgreSQL Finalization Staging Smoke Report"
    $lines += ""
    $lines += "- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    $lines += "- Base URL: $BaseUrl"
    $lines += "- Username: $Username"
    $lines += "- Passed: **0 / 11**"
    $lines += "- Overall: вќЊ FAIL"
    $lines += ""
    $lines += "## Preflight Failure"
    $lines += ""
    $lines += "- API is unreachable at $base/api/v1/health."
    $lines += "- Error: $($preflight.ErrorMessage)"
    $lines += ""
    $lines += "## Suggested Actions"
    $lines += ""
    $lines += "- Ensure server is started and listening on port 8080."
    $lines += "- Verify DB env vars (`DB_MODE`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`)."
    $lines += "- Re-run this script after service is reachable."
    Set-Content -Path $resolvedOutput -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
    Write-Host "Preflight failed: API unreachable." -ForegroundColor Red
    Write-Host "Report saved: $resolvedOutput" -ForegroundColor Cyan
    if ($ExitZeroOnReportFail) { exit 0 }
    exit 1
}

# 1) health
$health = Invoke-Api -Method "GET" -Url "$base/api/v1/health" -Session $session
$healthPass = $health.StatusCode -eq 200 -or $health.StatusCode -eq 503
$results += New-ResultRow `
    -Name "GET /api/v1/health" `
    -Passed $healthPass `
    -Expected "HTTP 200 or 503 with health payload" `
    -Actual (Format-Actual -StatusCode $health.StatusCode -ErrorMessage $health.ErrorMessage)

# 2) readiness
$ready = Invoke-Api -Method "GET" -Url "$base/api/v1/health/ready" -Session $session
$readyPass = $ready.StatusCode -eq 200
$results += New-ResultRow `
    -Name "GET /api/v1/health/ready" `
    -Passed $readyPass `
    -Expected "HTTP 200 (READY)" `
    -Actual (Format-Actual -StatusCode $ready.StatusCode -ErrorMessage $ready.ErrorMessage)

# 3) login
$loginBody = @{
    username = $Username
    password = $Password
}
$login = Invoke-Api -Method "POST" -Url "$base/api/v1/auth/login" -Session $session -Body $loginBody
$hasAccessCookie = $false
$hasRefreshCookie = $false
$accessToken = ""
try {
    $cookieUri = [Uri]$base
    $cookies = $session.Cookies.GetCookies($cookieUri)
    foreach ($c in $cookies) {
        if ($c.Name -eq "access_token") {
            $hasAccessCookie = $true
            $accessToken = $c.Value
        }
        if ($c.Name -eq "refresh_token") { $hasRefreshCookie = $true }
    }
} catch { }
$loginPass = $login.StatusCode -eq 200 -and $hasAccessCookie -and $hasRefreshCookie
$results += New-ResultRow `
    -Name "POST /api/v1/auth/login" `
    -Passed $loginPass `
    -Expected "HTTP 200 and access_token/refresh_token cookies set" `
    -Actual ("{0}, access_cookie={1}, refresh_cookie={2}" -f (Format-Actual -StatusCode $login.StatusCode -ErrorMessage $login.ErrorMessage), $hasAccessCookie, $hasRefreshCookie)

# Р”Р»СЏ Р·Р°С‰РёС‰РµРЅРЅС‹С… endpoint СЏРІРЅРѕ РѕС‚РїСЂР°РІР»СЏРµРј Bearer, С‡С‚РѕР±С‹ РЅРµ Р·Р°РІРёСЃРµС‚СЊ РѕС‚ cookie middleware.
$authHeaders = @{}
if (-not [string]::IsNullOrWhiteSpace($accessToken)) {
    $authHeaders["Authorization"] = "Bearer $accessToken"
}

# 4) authenticated GET cameras
$cameras = Invoke-Api -Method "GET" -Url "$base/api/v1/cameras" -Session $session -Headers $authHeaders
$results += New-ResultRow `
    -Name "GET /api/v1/cameras (auth)" `
    -Passed ($cameras.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $cameras.StatusCode -ErrorMessage $cameras.ErrorMessage)

# 5) authenticated GET events
$events = Invoke-Api -Method "GET" -Url "$base/api/v1/events" -Session $session -Headers $authHeaders
$results += New-ResultRow `
    -Name "GET /api/v1/events (auth)" `
    -Passed ($events.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $events.StatusCode -ErrorMessage $events.ErrorMessage)

# 6) authenticated GET recordings
$recordings = Invoke-Api -Method "GET" -Url "$base/api/v1/recordings" -Session $session -Headers $authHeaders
$results += New-ResultRow `
    -Name "GET /api/v1/recordings (auth)" `
    -Passed ($recordings.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $recordings.StatusCode -ErrorMessage $recordings.ErrorMessage)

# 7) admin DB health
$dbHealth = Invoke-Api -Method "GET" -Url "$base/api/v1/database/health" -Session $session -Headers $authHeaders
$results += New-ResultRow `
    -Name "GET /api/v1/database/health (admin)" `
    -Passed ($dbHealth.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $dbHealth.StatusCode -ErrorMessage $dbHealth.ErrorMessage)

# 8) admin DB migrations
$dbMigrations = Invoke-Api -Method "GET" -Url "$base/api/v1/database/migrations" -Session $session -Headers $authHeaders
$results += New-ResultRow `
    -Name "GET /api/v1/database/migrations (admin)" `
    -Passed ($dbMigrations.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $dbMigrations.StatusCode -ErrorMessage $dbMigrations.ErrorMessage)

# 9) admin DB pool stats
$dbPool = Invoke-Api -Method "GET" -Url "$base/api/v1/database/pool/stats" -Session $session -Headers $authHeaders
$results += New-ResultRow `
    -Name "GET /api/v1/database/pool/stats (admin)" `
    -Passed ($dbPool.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $dbPool.StatusCode -ErrorMessage $dbPool.ErrorMessage)

# 10) refresh
$refresh = Invoke-Api -Method "POST" -Url "$base/api/v1/auth/refresh" -Session $session -Body @{}
$results += New-ResultRow `
    -Name "POST /api/v1/auth/refresh" `
    -Passed ($refresh.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $refresh.StatusCode -ErrorMessage $refresh.ErrorMessage)

# 11) logout
$logout = Invoke-Api -Method "POST" -Url "$base/api/v1/auth/logout" -Session $session -Body @{} -Headers $authHeaders
$results += New-ResultRow `
    -Name "POST /api/v1/auth/logout" `
    -Passed ($logout.StatusCode -eq 200) `
    -Expected "HTTP 200" `
    -Actual (Format-Actual -StatusCode $logout.StatusCode -ErrorMessage $logout.ErrorMessage)

$passedCount = @($results | Where-Object { $_.Passed }).Count
$totalCount = $results.Count
$overallPassed = $passedCount -eq $totalCount

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$lines = @()
$lines += "# PostgreSQL Finalization Staging Smoke Report"
$lines += ""
$lines += "- Generated at: $timestamp"
$lines += "- Base URL: $BaseUrl"
$lines += "- Username: $Username"
$lines += "- Passed: **$passedCount / $totalCount**"
$lines += "- Overall: " + ($(if ($overallPassed) { "вњ… PASS" } else { "вќЊ FAIL" }))
$lines += ""
$lines += "| Check | Expected | Actual | Status |"
$lines += "|---|---|---|---|"
foreach ($r in $results) {
    $status = if ($r.Passed) { "вњ…" } else { "вќЊ" }
    $lines += "| $($r.Name) | $($r.Expected) | $($r.Actual) | $status |"
}
$lines += ""
$lines += "## Raw Notes"
$lines += ""
$lines += "- Health status code: $($health.StatusCode)"
$lines += "- Ready status code: $($ready.StatusCode)"
$lines += "- Login status code: $($login.StatusCode)"
$lines += "- DB health status code: $($dbHealth.StatusCode)"
$lines += "- DB migrations status code: $($dbMigrations.StatusCode)"
$lines += "- DB pool stats status code: $($dbPool.StatusCode)"
$lines += "- Refresh status code: $($refresh.StatusCode)"
$lines += "- Logout status code: $($logout.StatusCode)"

Set-Content -Path $resolvedOutput -Value ($lines -join [Environment]::NewLine) -Encoding UTF8

if ($overallPassed) {
    Write-Host "Smoke completed successfully: $passedCount/$totalCount checks passed." -ForegroundColor Green
} else {
    Write-Host "Smoke completed with failures: $passedCount/$totalCount checks passed." -ForegroundColor Yellow
}
Write-Host "Report saved: $resolvedOutput" -ForegroundColor Cyan

if ($overallPassed -or $ExitZeroOnReportFail) {
    exit 0
}
exit 1
