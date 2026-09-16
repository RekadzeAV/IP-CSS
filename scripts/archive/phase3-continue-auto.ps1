param(
    [string]$Version = "Alfa-0.1.1",
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run remaining Phase 3 automation chain (NAS + Desktop + Analytics)."
    Write-Host "Usage: .\scripts\phase3-continue-auto.ps1 [-Version Alfa-0.1.1]"
    exit 0
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$dateStamp = Get-Date -Format "yyyy-MM-dd"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$finalReport = "docs/reports/PHASE3_CONTINUE_AUTO_EXECUTION_STATUS_$dateStamp.md"

$steps = @(
    @{ Name = "NAS precheck"; Cmd = ".\scripts\phase3-auto-execution.ps1 -Version $Version -Packages ""synology,qnap,asustor,truenas"" -Architectures ""x86_64,arm64""" },
    @{ Name = "Desktop auto execution"; Cmd = ".\scripts\phase3-desktop-auto-execution.ps1" },
    @{ Name = "Analytics auto execution"; Cmd = ".\scripts\phase3-analytics-auto-execution.ps1" }
)

$results = @()
foreach ($step in $steps) {
    try {
        Invoke-Expression $step.Cmd
        $code = if ($null -ne $LASTEXITCODE) { [int]$LASTEXITCODE } elseif ($?) { 0 } else { 1 }
        if ($code -eq 0) {
            $results += [PSCustomObject]@{ Step = $step.Name; Status = "PASS"; ExitCode = $code; Note = "-" }
        } elseif ($code -eq 3) {
            $results += [PSCustomObject]@{ Step = $step.Name; Status = "PARTIAL"; ExitCode = $code; Note = "Partial completion" }
        } else {
            $results += [PSCustomObject]@{ Step = $step.Name; Status = "FAIL"; ExitCode = $code; Note = "Non-zero exit code" }
        }
    } catch {
        $results += [PSCustomObject]@{ Step = $step.Name; Status = "FAIL"; ExitCode = 1; Note = $_.Exception.Message }
    }
}

$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$partialCount = ($results | Where-Object { $_.Status -eq "PARTIAL" }).Count
$overall = if ($failCount -gt 0) { "FAIL" } elseif ($partialCount -gt 0) { "PARTIAL" } else { "PASS" }

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Phase 3 Continue Auto-Execution Status ($dateStamp)")
$lines.Add("")
$lines.Add("## Execution Snapshot")
$lines.Add("")
$lines.Add("- Timestamp: $timestamp")
$lines.Add("- Version: $Version")
$lines.Add("- Overall: $overall")
$lines.Add("")
$lines.Add("## Step Results")
$lines.Add("")
$lines.Add("| Step | Status | Exit Code | Note |")
$lines.Add("|---|---|---:|---|")
foreach ($r in $results) {
    $exitCodeText = if ($null -eq $r.ExitCode) { "-" } else { [string]$r.ExitCode }
    $note = if ([string]::IsNullOrWhiteSpace($r.Note)) { "-" } else { $r.Note.Replace('|','/') }
    $lines.Add("| $($r.Step) | $($r.Status) | $exitCodeText | $note |")
}
$lines.Add("")
$lines.Add("## Generated Reports")
$lines.Add("")
$lines.Add("- docs/reports/PHASE3_AUTO_EXECUTION_STATUS_$dateStamp.md")
$lines.Add("- docs/reports/PHASE3_DESKTOP_AUTO_EXECUTION_STATUS_$dateStamp.md")
$lines.Add("- docs/reports/PHASE3_ANALYTICS_AUTO_EXECUTION_STATUS_$dateStamp.md")

Set-Content -Path $finalReport -Value $lines -Encoding UTF8
Write-Host "Phase 3 chained auto report generated: $finalReport" -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0
