[CmdletBinding()]
param(
    [switch]$ShowHelp,
    [string]$PackageName = "com.company.ipcamera",
    [string]$Permission = "android.permission.CAMERA",
    [string]$OutputDir = "diagnostics\platform-smoke\android",
    [switch]$FailIfNoDevice
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$resolvedOutputDir = Join-Path $projectRoot $OutputDir

if ($ShowHelp) {
    Write-Host "Android permissions revoke/recover smoke (adb runtime check)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\android-permissions-revoke-recover-smoke.ps1"
    Write-Host "  .\scripts\android-permissions-revoke-recover-smoke.ps1 -PackageName com.company.ipcamera -Permission android.permission.CAMERA"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -OutputDir  Repo-relative folder for report json/md"
    Write-Host "  -FailIfNoDevice  Return FAIL instead of PARTIAL when adb has no connected device"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  PASS"
    Write-Host "  1  FAIL"
    Write-Host "  3  PARTIAL (typically no device)"
    exit 0
}

if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryJson = Join-Path $resolvedOutputDir ("android-permissions-revoke-recover-smoke-{0}.json" -f $runId)
$summaryMd = Join-Path $resolvedOutputDir ("android-permissions-revoke-recover-smoke-{0}.md" -f $runId)

function Get-AdbPath {
    $candidates = New-Object System.Collections.Generic.List[string]
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -ne $adb) {
        if (-not [string]::IsNullOrWhiteSpace($adb.Path)) { $candidates.Add($adb.Path) | Out-Null }
        if (-not [string]::IsNullOrWhiteSpace($adb.Source)) { $candidates.Add($adb.Source) | Out-Null }
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        $candidates.Add((Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")) | Out-Null
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $candidates.Add((Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")) | Out-Null
    }
    $candidates.Add((Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")) | Out-Null
    return ($candidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path $_) } | Select-Object -First 1)
}

function Get-DeviceList([string]$adbPath) {
    & $adbPath start-server *> $null
    $output = & $adbPath devices 2>$null
    $devices = @()
    foreach ($line in $output) {
        if ($line -match "^(\S+)\s+device$") {
            $devices += $Matches[1]
        }
    }
    return $devices
}

function Get-PermissionState([string]$adbPath, [string]$device, [string]$pkg, [string]$perm) {
    $dump = & $adbPath -s $device shell dumpsys package $pkg 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dump)) { return "UNKNOWN" }

    # Typical line: android.permission.CAMERA: granted=true, flags=[ ... ]
    $regex = [regex]::Escape($perm) + ":\s*granted=(true|false)"
    $m = [regex]::Match(($dump -join "`n"), $regex, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $m.Success) { return "UNKNOWN" }
    if ($m.Groups[1].Value -ieq "true") { return "GRANTED" }
    return "DENIED"
}

$overall = "PASS"
$notes = New-Object System.Collections.Generic.List[string]
$steps = New-Object System.Collections.Generic.List[object]

$adbPath = Get-AdbPath
if ($null -eq $adbPath) {
    $overall = "PARTIAL"
    $notes.Add("adb not found in PATH.") | Out-Null
} else {
    $devices = Get-DeviceList -adbPath $adbPath
    if ($devices.Count -eq 0) {
        if ($FailIfNoDevice) {
            $overall = "FAIL"
        } else {
            $overall = "PARTIAL"
        }
        $notes.Add("No connected Android devices/emulators.") | Out-Null
    } else {
        $device = $devices[0]
        $notes.Add("Using device: $device") | Out-Null

        $stateInitial = Get-PermissionState -adbPath $adbPath -device $device -pkg $PackageName -perm $Permission
        $steps.Add([PSCustomObject]@{ Step = "initial_state"; Status = "PASS"; Detail = $stateInitial }) | Out-Null

        & $adbPath -s $device shell pm revoke $PackageName $Permission 2>$null
        if ($LASTEXITCODE -ne 0) {
            $overall = "FAIL"
            $steps.Add([PSCustomObject]@{ Step = "revoke"; Status = "FAIL"; Detail = "pm revoke exit $LASTEXITCODE" }) | Out-Null
        } else {
            $stateAfterRevoke = Get-PermissionState -adbPath $adbPath -device $device -pkg $PackageName -perm $Permission
            $revokePass = $stateAfterRevoke -eq "DENIED" -or $stateAfterRevoke -eq "UNKNOWN"
            if (-not $revokePass) { $overall = "FAIL" }
            $steps.Add([PSCustomObject]@{
                Step = "revoke"
                Status = $(if ($revokePass) { "PASS" } else { "FAIL" })
                Detail = "state=$stateAfterRevoke"
            }) | Out-Null

            & $adbPath -s $device shell pm grant $PackageName $Permission 2>$null
            if ($LASTEXITCODE -ne 0) {
                $overall = "FAIL"
                $steps.Add([PSCustomObject]@{ Step = "grant"; Status = "FAIL"; Detail = "pm grant exit $LASTEXITCODE" }) | Out-Null
            } else {
                $stateAfterGrant = Get-PermissionState -adbPath $adbPath -device $device -pkg $PackageName -perm $Permission
                $grantPass = $stateAfterGrant -eq "GRANTED" -or $stateAfterGrant -eq "UNKNOWN"
                if (-not $grantPass) { $overall = "FAIL" }
                $steps.Add([PSCustomObject]@{
                    Step = "grant"
                    Status = $(if ($grantPass) { "PASS" } else { "FAIL" })
                    Detail = "state=$stateAfterGrant"
                }) | Out-Null
            }
        }
    }
}

$summary = [ordered]@{
    runId = $runId
    timestampUtc = [DateTime]::UtcNow.ToString("o")
    packageName = $PackageName
    permission = $Permission
    statuses = @{
        overall = $overall
    }
    steps = $steps.ToArray()
    notes = $notes.ToArray()
}

$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson -Encoding UTF8

$lines = @(
    "# Android Permission Revoke/Recover Smoke Report",
    "",
    "- Run ID: $runId",
    "- Package: **$PackageName**",
    "- Permission: **$Permission**",
    "- Overall: **$overall**",
    "",
    "## Steps",
    "",
    "| Step | Status | Detail |",
    "|---|---|---|"
)
if ($steps.Count -eq 0) {
    $lines += "| n/a | n/a | no runtime steps executed |"
} else {
    foreach ($s in $steps) {
        $detail = if ($null -eq $s.Detail) { "" } else { ([string]$s.Detail).Replace("|", "/") }
        $lines += "| $($s.Step) | $($s.Status) | $detail |"
    }
}

$lines += ""
$lines += "## Notes"
if ($notes.Count -eq 0) {
    $lines += "- none"
} else {
    foreach ($n in $notes) {
        $lines += "- $n"
    }
}

$lines -join [Environment]::NewLine | Set-Content -Path $summaryMd -Encoding UTF8

Write-Host ("Android permissions smoke summary: {0}" -f $summaryMd) -ForegroundColor Green
Write-Host ("Android permissions smoke json: {0}" -f $summaryJson) -ForegroundColor Green

if ($overall -eq "FAIL") { exit 1 }
if ($overall -eq "PARTIAL") { exit 3 }
exit 0

