param(
    [string]$Version = "Alfa-0.1.1",
    [string]$Packages = "synology,qnap,asustor,truenas",
    [string]$Architectures = "x86_64,arm64",
    [switch]$RunBuild,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Run automated Phase 3 precheck flow and publish a status report."
    Write-Host "Usage: .\scripts\phase3-auto-execution.ps1 [-Version Alfa-0.1.1] [-Packages `"synology,qnap,asustor,truenas`"] [-Architectures `"x86_64,arm64`"] [-RunBuild]"
    exit 0
}

function New-Result {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Note
    )
    [PSCustomObject]@{
        Name = $Name
        Status = $Status
        Note = $Note
    }
}

function Invoke-Step {
    param(
        [string]$Name,
        [ScriptBlock]$Action
    )
    try {
        & $Action
        return New-Result -Name $Name -Status "PASS" -Note ""
    } catch {
        return New-Result -Name $Name -Status "FAIL" -Note $_.Exception.Message
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$packagesList = $Packages.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
$archList = $Architectures.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }

$results = New-Object System.Collections.Generic.List[object]

$results.Add((Invoke-Step -Name "Validate NAS contracts" -Action {
    & "$projectRoot\scripts\ci\validate-nas-contracts.ps1" -Version $Version -Packages $Packages -Architectures $Architectures
}))

if ($RunBuild) {
    foreach ($pkg in $packagesList) {
        if ($pkg -eq "truenas") {
            $results.Add((Invoke-Step -Name "Build package: $pkg/$($archList[0])" -Action {
                & "$projectRoot\scripts\build-nas-package.ps1" -PackageType $pkg -Arch $archList[0] -Version $Version
            }))
            continue
        }

        foreach ($arch in $archList) {
            $results.Add((Invoke-Step -Name "Build package: $pkg/$arch" -Action {
                & "$projectRoot\scripts\build-nas-package.ps1" -PackageType $pkg -Arch $arch -Version $Version
            }))
        }
    }
}

foreach ($pkg in $packagesList) {
    if ($pkg -eq "truenas") {
        $results.Add((Invoke-Step -Name "Smoke precheck: $pkg/$($archList[0])" -Action {
            if (-not (Test-Path "build/truenas-$Version")) {
                throw "TrueNAS bundle not found: build/truenas-$Version"
            }
        }))
        continue
    }

    foreach ($arch in $archList) {
        $results.Add((Invoke-Step -Name "Smoke precheck: $pkg/$arch" -Action {
            & "$projectRoot\scripts\test-nas-build.ps1" -PackageType $pkg -Arch $arch
        }))
    }
}

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$dateStamp = Get-Date -Format "yyyy-MM-dd"
$reportPath = "docs/reports/PHASE3_AUTO_EXECUTION_STATUS_$dateStamp.md"
$nowIso = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$overall = if ($failCount -eq 0) { "PASS (PRECHECK)" } else { "FAIL (PRECHECK)" }
$programGate = "NO-GO (until S2-S6 field validation evidence is complete)"

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Phase 3 Auto-Execution Status ($dateStamp)")
$lines.Add("")
$lines.Add("## Execution Snapshot")
$lines.Add("")
$lines.Add("- Timestamp: $nowIso")
$lines.Add("- Version: $Version")
$lines.Add("- Packages: $Packages")
$lines.Add("- Architectures: $Architectures")
$lines.Add("- Build mode: " + ($(if ($RunBuild) { "enabled" } else { "disabled (precheck only)" })))
$lines.Add("- Results: PASS $passCount / FAIL $failCount")
$lines.Add("- Precheck status: $overall")
$lines.Add("- Program gate: $programGate")
$lines.Add("")
$lines.Add("## Step Results")
$lines.Add("")
$lines.Add("| Step | Status | Note |")
$lines.Add("|---|---|---|")
foreach ($result in $results) {
    $note = if ([string]::IsNullOrWhiteSpace($result.Note)) { "-" } else { $result.Note.Replace("|", "/") }
    $lines.Add("| $($result.Name) | $($result.Status) | $note |")
}
$lines.Add("")
$lines.Add("## Field Validation Status (S2-S6)")
$lines.Add("")
$lines.Add("- Synology: PENDING (field)")
$lines.Add("- QNAP: PENDING (field)")
$lines.Add("- Asustor: PENDING (field)")
$lines.Add("- TrueNAS CORE/SCALE: PENDING (field)")
$lines.Add("")
$lines.Add("## Decision")
$lines.Add("")
$lines.Add("- Packaging/checksum precheck decision: " + ($(if ($failCount -eq 0) { "CONDITIONAL GO (PRECHECK)" } else { "NO-GO (PRECHECK FAIL)" })))
$lines.Add("- Program release decision: $programGate")
$lines.Add("")
$lines.Add("## Evidence Links")
$lines.Add("")
$lines.Add("- docs/reports/NAS_RELEASE_MASTER_INDEX_2026-04-26.md")
$lines.Add("- docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md")
$lines.Add("- docs/reports/NAS_PLATFORM_SMOKE_RUNBOOK_2026-04-26.md")
$lines.Add("- docs/status/NAS_PLATFORM_STATUS_TABLE.md")

Set-Content -Path $reportPath -Value $lines -Encoding UTF8
Write-Host "Phase 3 auto-execution report generated: $reportPath" -ForegroundColor Green

if ($failCount -gt 0) {
    exit 1
}
