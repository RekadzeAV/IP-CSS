param(
    [switch]$ShowHelp,
    [string]$ReportPath = "release-build/test/security-mvp-readiness-report.md",
    [string]$EnvFilePath = ""
)

if ($ShowHelp) {
    Write-Host "Security MVP readiness check (env-derived controls + markdown report)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\security-mvp-readiness-check.ps1"
    Write-Host "  .\scripts\security-mvp-readiness-check.ps1 -ReportPath release-build\test\security-mvp-readiness-report.md -EnvFilePath .env.example"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ReportPath  Where to write the markdown report (default under release-build/test)."
    Write-Host "  -EnvFilePath  Optional .env file for fallback values (merged with process environment)."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Decision GO (all controls PASS)"
    Write-Host "  2  Decision NO-GO (at least one FAIL)"
    Write-Host "  3  Decision CONDITIONAL (no FAIL, some CONDITIONAL)"
    exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-EnvValue {
    param(
        [string]$Name,
        [hashtable]$FallbackMap
    )
    $raw = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($raw) -and $null -ne $FallbackMap -and $FallbackMap.ContainsKey($Name)) {
        $raw = [string]$FallbackMap[$Name]
    }
    return $raw
}

function Get-EnvBool {
    param(
        [string]$Name,
        [hashtable]$FallbackMap,
        [bool]$Default = $false
    )
    $raw = Get-EnvValue -Name $Name -FallbackMap $FallbackMap
    if ([string]::IsNullOrWhiteSpace($raw)) { return $Default }
    return $raw.Trim().ToLowerInvariant() -eq "true"
}

function Read-EnvFileMap {
    param([string]$Path)
    $map = @{}
    if ([string]::IsNullOrWhiteSpace($Path)) { return $map }
    if (-not (Test-Path -LiteralPath $Path)) { return $map }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) { continue }
        $idx = $trimmed.IndexOf("=")
        if ($idx -lt 1) { continue }
        $key = $trimmed.Substring(0, $idx).Trim()
        if ([string]::IsNullOrWhiteSpace($key)) { continue }
        $value = $trimmed.Substring($idx + 1).Trim()
        $map[$key] = $value
    }
    return $map
}

function Add-CheckResult {
    param(
        [System.Collections.Generic.List[object]]$List,
        [string]$Id,
        [string]$Control,
        [string]$State,
        [string]$Evidence
    )
    $List.Add([PSCustomObject]@{
        Id = $Id
        Control = $Control
        State = $State
        Evidence = $Evidence
    }) | Out-Null
}

$results = New-Object System.Collections.Generic.List[object]

$resolvedEnvFile = $EnvFilePath
if (-not [string]::IsNullOrWhiteSpace($resolvedEnvFile) -and -not [System.IO.Path]::IsPathRooted($resolvedEnvFile)) {
    $resolvedEnvFile = Join-Path (Get-Location) $resolvedEnvFile
}
$envFileMap = Read-EnvFileMap -Path $resolvedEnvFile

$environment = Get-EnvValue -Name "ENVIRONMENT" -FallbackMap $envFileMap
$nodeEnv = Get-EnvValue -Name "NODE_ENV" -FallbackMap $envFileMap
$isProduction = ($environment -eq "production" -or $nodeEnv -eq "production")

$useHttps = Get-EnvBool -Name "USE_HTTPS" -FallbackMap $envFileMap -Default:$false
$forceHttps = Get-EnvBool -Name "FORCE_HTTPS" -FallbackMap $envFileMap -Default:$isProduction
$allowExternalTls = Get-EnvBool -Name "ALLOW_EXTERNAL_TLS_TERMINATION" -FallbackMap $envFileMap -Default:$false
$auditPersistEnabled = Get-EnvBool -Name "AUDIT_PERSIST_ENABLED" -FallbackMap $envFileMap -Default:$false

$sslKeyStorePath = Get-EnvValue -Name "SSL_KEYSTORE_PATH" -FallbackMap $envFileMap
$sslKeyStorePassword = Get-EnvValue -Name "SSL_KEYSTORE_PASSWORD" -FallbackMap $envFileMap

# 1.9.3 Certificate pinning path is implemented in app code.
# Operational check here validates deployment-side trust preconditions.
if ($forceHttps -or $useHttps -or $allowExternalTls) {
    Add-CheckResult -List $results -Id "1.9.3" -Control "TLS trust boundary configured (required for pinning strategy)" -State "PASS" -Evidence "FORCE_HTTPS/USE_HTTPS/ALLOW_EXTERNAL_TLS_TERMINATION is enabled."
} else {
    Add-CheckResult -List $results -Id "1.9.3" -Control "TLS trust boundary configured (required for pinning strategy)" -State "CONDITIONAL" -Evidence "TLS-related switches are not enabled."
}

# 1.9.4 Enforced HTTPS
if ($forceHttps -and ($useHttps -or $allowExternalTls)) {
    Add-CheckResult -List $results -Id "1.9.4" -Control "HTTPS enforcement topology" -State "PASS" -Evidence "FORCE_HTTPS=true and TLS termination path is configured."
} else {
    Add-CheckResult -List $results -Id "1.9.4" -Control "HTTPS enforcement topology" -State "CONDITIONAL" -Evidence "Expected FORCE_HTTPS=true and one of USE_HTTPS=true / ALLOW_EXTERNAL_TLS_TERMINATION=true."
}

# Direct TLS consistency checks (ServerConfig startup guards mirror this).
if ($useHttps) {
    if ([string]::IsNullOrWhiteSpace($sslKeyStorePath) -or [string]::IsNullOrWhiteSpace($sslKeyStorePassword)) {
        Add-CheckResult -List $results -Id "1.9.4a" -Control "Direct TLS keystore variables present" -State "FAIL" -Evidence "USE_HTTPS=true but SSL_KEYSTORE_PATH/SSL_KEYSTORE_PASSWORD are not fully set."
    } elseif (-not (Test-Path -LiteralPath $sslKeyStorePath)) {
        Add-CheckResult -List $results -Id "1.9.4a" -Control "Direct TLS keystore variables present" -State "FAIL" -Evidence "Keystore file not found: $sslKeyStorePath"
    } else {
        Add-CheckResult -List $results -Id "1.9.4a" -Control "Direct TLS keystore variables present" -State "PASS" -Evidence "Keystore variables and file are present."
    }
}

# 1.9.5 Credentials encryption: deployment check for required mode/env.
$dbMode = Get-EnvValue -Name "DB_MODE" -FallbackMap $envFileMap
if ([string]::IsNullOrWhiteSpace($dbMode)) { $dbMode = if ($isProduction) { "postgres" } else { "embedded" } }
if ($dbMode -in @("postgres", "embedded")) {
    Add-CheckResult -List $results -Id "1.9.5" -Control "Credential-at-rest encryption runtime mode" -State "PASS" -Evidence "DB_MODE=$dbMode (supported). Application enforces fail-closed mapper/migration in code."
} else {
    Add-CheckResult -List $results -Id "1.9.5" -Control "Credential-at-rest encryption runtime mode" -State "FAIL" -Evidence "Unsupported DB_MODE=$dbMode"
}

# 1.9.6 Audit persistence
if ($auditPersistEnabled) {
    Add-CheckResult -List $results -Id "1.9.6" -Control "Durable audit logging enabled" -State "PASS" -Evidence "AUDIT_PERSIST_ENABLED=true"
} else {
    Add-CheckResult -List $results -Id "1.9.6" -Control "Durable audit logging enabled" -State "CONDITIONAL" -Evidence "AUDIT_PERSIST_ENABLED is not true."
}

$failCount = @($results | Where-Object { $_.State -eq "FAIL" }).Count
$conditionalCount = @($results | Where-Object { $_.State -eq "CONDITIONAL" }).Count
$goDecision = if ($failCount -gt 0) {
    "NO-GO"
} elseif ($conditionalCount -gt 0) {
    "CONDITIONAL"
} else {
    "GO"
}

$reportLines = @(
    "# Security MVP Readiness Report",
    "",
    "Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz")",
    "Env source: $(if ([string]::IsNullOrWhiteSpace($resolvedEnvFile)) { "process environment" } else { $resolvedEnvFile })",
    "Decision: **$goDecision**",
    "",
    "| ID | Control | State | Evidence |",
    "|---|---|---|---|"
)

foreach ($r in $results) {
    $reportLines += "| $($r.Id) | $($r.Control) | $($r.State) | $($r.Evidence) |"
}

$reportDir = Split-Path -Parent $ReportPath
if (-not [string]::IsNullOrWhiteSpace($reportDir) -and -not (Test-Path -LiteralPath $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}

$reportLines -join [Environment]::NewLine | Set-Content -LiteralPath $ReportPath -Encoding UTF8

Write-Host "Security MVP readiness decision: $goDecision"
Write-Host "Report written to: $ReportPath"

if ($goDecision -eq "NO-GO") {
    Write-Host "At least one control is FAIL. Review report for remediation."
    exit 2
}
if ($goDecision -eq "CONDITIONAL") {
    Write-Host "Some controls are CONDITIONAL. Release may proceed with documented limitations."
    exit 3
}
exit 0
