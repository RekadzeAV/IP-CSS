param(
    [string]$OutputDir = "docs/reports",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run Phase 3 analytics auto-execution and publish status report."
    Write-Host "Usage: .\scripts\phase3-analytics-auto-execution.ps1"
    exit 0
}

function Invoke-TestStep {
    param(
        [string]$Name,
        [string]$Command,
        [switch]$AllowFailure
    )
    try {
        Invoke-Expression $Command | Out-Host
        if ($LASTEXITCODE -eq 0) {
            return [PSCustomObject]@{ Name = $Name; Status = "PASS"; Note = "-" }
        }
        if ($AllowFailure) {
            return [PSCustomObject]@{ Name = $Name; Status = "PARTIAL"; Note = "Exit code $LASTEXITCODE (non-blocking)" }
        }
        return [PSCustomObject]@{ Name = $Name; Status = "FAIL"; Note = "Exit code $LASTEXITCODE" }
    } catch {
        if ($AllowFailure) {
            return [PSCustomObject]@{ Name = $Name; Status = "PARTIAL"; Note = $_.Exception.Message }
        }
        return [PSCustomObject]@{ Name = $Name; Status = "FAIL"; Note = $_.Exception.Message }
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
$gradlew = Join-Path $projectRoot "gradlew.bat"

if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found: $gradlew"
}

$results = @()
$results += Invoke-TestStep -Name "Face repository integration test" -Command ".\gradlew.bat :shared:desktopTest --tests ""*FaceRepositoryImplSqlDelightIntegrationTest"" --no-daemon"
$results += Invoke-TestStep -Name "Server API compile baseline" -Command ".\gradlew.bat :server:api:compileKotlin --no-daemon"
$results += Invoke-TestStep -Name "Face gallery route test" -Command ".\gradlew.bat :server:api:test --tests ""*FaceGalleryRoutesTest"" --no-daemon" -AllowFailure
$results += Invoke-TestStep -Name "Native analytics contract test" -Command ".\gradlew.bat :core:network:desktopTest --tests ""*NativeAnalyticsTest"" --no-daemon"

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$partialCount = ($results | Where-Object { $_.Status -eq "PARTIAL" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$overall = if ($failCount -gt 0) { "FAIL" } elseif ($partialCount -gt 0) { "PARTIAL" } else { "PASS" }
$decision = if ($overall -eq "PASS") { "CONDITIONAL GO (Analytics baseline)" } else { "NO-GO" }

$dateStamp = Get-Date -Format "yyyy-MM-dd"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$outFile = Join-Path $projectRoot "$OutputDir\PHASE3_ANALYTICS_AUTO_EXECUTION_STATUS_$dateStamp.md"

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Phase 3 Analytics Auto-Execution Status ($dateStamp)")
$lines.Add("")
$lines.Add("## Execution Snapshot")
$lines.Add("")
$lines.Add("- Timestamp: $timestamp")
$lines.Add("- Results: PASS $passCount / PARTIAL $partialCount / FAIL $failCount")
$lines.Add("- Overall: $overall")
$lines.Add("- Decision: $decision")
$lines.Add("")
$lines.Add("## Step Results")
$lines.Add("")
$lines.Add("| Step | Status | Note |")
$lines.Add("|---|---|---|")
foreach ($r in $results) {
    $note = if ([string]::IsNullOrWhiteSpace($r.Note)) { "-" } else { $r.Note.Replace('|','/') }
    $lines.Add("| $($r.Name) | $($r.Status) | $note |")
}
$lines.Add("")
$lines.Add("## Story 3.3 Coverage")
$lines.Add("")
$lines.Add("- 3.3.1 ANPR hardening: PARTIAL (requires dataset quality gates).")
$lines.Add("- 3.3.2 Face recognition pipeline: baseline tests executed.")
$lines.Add("- 3.3.3 Reports (CSV/PDF): code path exists, dedicated report correctness dataset still pending.")
$lines.Add("")
$lines.Add("## Evidence")
$lines.Add("")
$lines.Add("- server/api/src/main/kotlin/com/company/ipcamera/server/service/ReportServiceImpl.kt")
$lines.Add("- shared/src/commonMain/kotlin/com/company/ipcamera/shared/domain/repository/FaceRepository.kt")
$lines.Add("- docs/planning/PHASE_3_DETAILED_BACKLOG.md")

Set-Content -Path $outFile -Value $lines -Encoding UTF8
Write-Host "Analytics Phase 3 auto report generated: $outFile" -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
