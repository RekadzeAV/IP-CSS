param(
    [switch]$ShowHelp,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OutputFile = "diagnostics\postgres-finalization\preflight-report.md",
    [switch]$Strict
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "PostgreSQL finalization preflight (env/docker/API health checks)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\postgres-finalization-preflight.ps1"
    Write-Host "  .\scripts\postgres-finalization-preflight.ps1 -BaseUrl http://localhost:8080 -Strict"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputFile  Repo-relative markdown report path"
    Write-Host "  -Strict  Stricter checks when applicable"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  All critical checks passed"
    Write-Host "  1  Critical preflight checks failed"
    exit 0
}

function Add-CheckResult {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Details
    )
    [PSCustomObject]@{ Name = $Name; Passed = $Passed; Details = $Details }
}

function Test-HttpEndpoint {
    param([string]$Url)
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
        return [PSCustomObject]@{ Reachable = $true; StatusCode = [int]$resp.StatusCode; Error = "" }
    } catch {
        $code = 0
        try { if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode.value__ } } catch { }
        return [PSCustomObject]@{ Reachable = $false; StatusCode = $code; Error = $_.Exception.Message }
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutput = if ([System.IO.Path]::IsPathRooted($OutputFile)) { $OutputFile } else { Join-Path $projectRoot $OutputFile }
$outDir = Split-Path -Parent $resolvedOutput
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$results = @()
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
$results += Add-CheckResult -Name "Docker CLI available" -Passed ($null -ne $dockerCmd) -Details ($(if ($dockerCmd) { $dockerCmd.Source } else { "docker not found in PATH" }))
$composeFile = Join-Path $projectRoot "docker-compose.yml"
$results += Add-CheckResult -Name "docker-compose.yml exists" -Passed (Test-Path $composeFile) -Details $composeFile

$health = Test-HttpEndpoint -Url ($BaseUrl.TrimEnd("/") + "/api/v1/health")
$healthOk = $health.Reachable -and ($health.StatusCode -eq 200 -or $health.StatusCode -eq 503)
$results += Add-CheckResult -Name "API /api/v1/health reachable" -Passed $healthOk -Details ($(if ($healthOk) { "HTTP $($health.StatusCode)" } else { "status=$($health.StatusCode), error=$($health.Error)" }))

$requiredEnv = @("ENVIRONMENT","NODE_ENV","DB_MODE","DATABASE_URL","DATABASE_USER","DATABASE_PASSWORD","ENABLE_FLYWAY")
foreach ($envName in $requiredEnv) {
    $value = [Environment]::GetEnvironmentVariable($envName)
    $present = -not [string]::IsNullOrWhiteSpace($value)
    $masked = if ($present) { if ($envName -like "*PASSWORD*" -or $envName -like "*SECRET*") { "***set***" } else { $value } } else { "<missing>" }
    $results += Add-CheckResult -Name "Env $envName present" -Passed $present -Details $masked
}

$criticalFailures = @($results | Where-Object { -not $_.Passed -and $_.Name -match "^Env " }).Count
if ($Strict) {
    $criticalFailures += @($results | Where-Object { -not $_.Passed -and $_.Name -in @("Docker CLI available","docker-compose.yml exists") }).Count
}

$passedCount = @($results | Where-Object { $_.Passed }).Count
$totalCount = $results.Count
$overallPass = $criticalFailures -eq 0

$lines = @()
$lines += "# PostgreSQL Finalization Preflight Report"
$lines += ""
$lines += "- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += "- Base URL: $BaseUrl"
$lines += "- Strict mode: $Strict"
$lines += "- Passed checks: **$passedCount / $totalCount**"
$lines += "- Critical status: " + ($(if ($overallPass) { "PASS" } else { "FAIL" }))
$lines += ""
$lines += "| Check | Result | Details |"
$lines += "|---|---|---|"
foreach ($r in $results) {
    $status = if ($r.Passed) { "OK" } else { "FAIL" }
    $details = ($r.Details -replace "\|", "/")
    $lines += "| $($r.Name) | $status | $details |"
}
$lines += ""
$lines += "## Recommended next command"
$lines += ""
$lines += '```powershell'
$lines += ".\\scripts\\run-postgres-finalization-suite.ps1 -AutoStartRuntime -ValidateReport -BaseUrl http://localhost:8080 -Username admin -Password $env:ADMIN_PASSWORD -Environment staging -BuildCommit commit-sha -Owner owner -Reviewer reviewer"
$lines += '```'

Set-Content -Path $resolvedOutput -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "Preflight report saved: $resolvedOutput" -ForegroundColor Cyan
if (-not $overallPass) {
    Write-Host "Critical preflight checks failed." -ForegroundColor Red
    exit 1
}
Write-Host "Preflight checks passed." -ForegroundColor Green
exit 0
