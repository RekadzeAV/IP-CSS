param(
  [string]$InputPath = "",
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Parse C4_RESULTS network degradation cycles and update docs/status markdown."
  Write-Host "Usage: .\scripts\process-c4-results.ps1 -InputPath <path>"
  exit 0
}

if ([string]::IsNullOrWhiteSpace($InputPath)) {
  Write-Host "InputPath is required." -ForegroundColor Red
  exit 1
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (-not (Test-Path -Path $InputPath)) { throw "Input file not found: $InputPath" }

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeLogPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_RUNTIME_VALIDATION_LOG.md"
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"

function Write-TextRobust {
  param([string]$Path, [string]$Text)
  try { [System.IO.File]::WriteAllText($Path, $Text, [System.Text.Encoding]::UTF8) }
  catch {
    $tmp = "$Path.tmp"
    [System.IO.File]::WriteAllText($tmp, $Text, [System.Text.Encoding]::UTF8)
    Move-Item -Path $tmp -Destination $Path -Force
  }
}

function ConvertTo-BoolValue {
  param([string]$Value)
  $v = $Value.Trim().ToLowerInvariant()
  if ($v -in @("yes", "y", "true", "1")) { return $true }
  if ($v -in @("no", "n", "false", "0")) { return $false }
  throw "Invalid bool value: $Value"
}

$lines = (Get-Content -Path $InputPath -Raw) -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 }
$cycles = @{}
foreach ($line in $lines) {
  if ($line.Trim() -eq "C4_RESULTS") { continue }
  $m = [regex]::Match($line.Trim(), '^cycle(?<idx>[1-3]):\s*duration=(?<duration>[^,]+),\s*reconnect=(?<reconnect>[^,]+),\s*duplicates=(?<duplicates>[^,]+),\s*criticalLoss=(?<loss>[^,]+),\s*uiCrash=(?<crash>[^,]+),\s*note=(?<note>.+)$')
  if (-not $m.Success) { continue }
  $idx = [int]$m.Groups["idx"].Value
  $cycles[$idx] = @{
    duration = $m.Groups["duration"].Value.Trim()
    reconnect = ConvertTo-BoolValue $m.Groups["reconnect"].Value
    duplicates = ConvertTo-BoolValue $m.Groups["duplicates"].Value
    loss = ConvertTo-BoolValue $m.Groups["loss"].Value
    crash = ConvertTo-BoolValue $m.Groups["crash"].Value
    note = $m.Groups["note"].Value.Trim()
  }
}

foreach ($idx in 1..3) { if (-not $cycles.ContainsKey($idx)) { throw "Missing cycle result: cycle$idx" } }

$reconnectCount = 0
$hasCrash = $false
$hasDup = $false
$hasLoss = $false
foreach ($idx in 1..3) {
  $c = $cycles[$idx]
  if ($c.reconnect) { $reconnectCount++ }
  if ($c.crash) { $hasCrash = $true }
  if ($c.duplicates) { $hasDup = $true }
  if ($c.loss) { $hasLoss = $true }
}

$status = "Open"
if ($reconnectCount -eq 3 -and -not $hasCrash -and -not $hasDup -and -not $hasLoss) {
  $status = "Done"
} elseif ($reconnectCount -ge 2 -and -not $hasCrash) {
  $status = "Partial"
}

$runtime = Get-Content -Path $runtimeLogPath -Raw
foreach ($idx in 1..3) {
  $c = $cycles[$idx]
  $reconnectCell = if ($c.reconnect) { "OK" } else { "FAIL" }
  $dupCell = if ($c.duplicates) { "YES" } else { "NO" }
  $lossCell = if ($c.loss) { "YES" } else { "NO" }
  $row = "| $idx | Slow 3G + Offline | ``$($c.duration)`` | ``$reconnectCell`` | ``$dupCell`` | ``$lossCell`` | ``$(if ($c.reconnect -and -not $c.crash) { "PASS" } else { "FAIL" })`` |"
  $runtime = [regex]::Replace($runtime, "(?m)^\| $idx \| Slow 3G \+ Offline \|.*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $row })
  $noteLine = "- Cycle ${idx}: ``$($c.note.Replace('`r', ' ').Replace('`n', ' '))``"
  $runtime = [regex]::Replace($runtime, "(?m)^- Cycle ${idx}: .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $noteLine })
}

$runtime = if ($runtime -match "(?m)^\*\*Status:\*\* .*$") {
  [regex]::Replace($runtime, "(?m)^\*\*Status:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) "**Status:** ``$status``  " })
} else {
  $runtime + [System.Environment]::NewLine + "**Status:** ``$status``  "
}
$runtime = if ($runtime -match "(?m)^\*\*Decision:\*\* .*$") {
  [regex]::Replace($runtime, "(?m)^\*\*Decision:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) "**Decision:** ``C4 -> $status``  " })
} else {
  $runtime + [System.Environment]::NewLine + "**Decision:** ``C4 -> $status``  "
}
$reason = if ($status -eq "Done") {
  "**Reason:** ``3/3 reconnect cycles passed; no UI crash, duplicates, or critical loss.``"
} elseif ($status -eq "Partial") {
  "**Reason:** ``At least 2/3 reconnect cycles passed with no UI crash.``"
} else {
  "**Reason:** ``Partial threshold not met: reconnect < 2/3 or UI crash detected.``"
}
$runtime = if ($runtime -match "(?m)^\*\*Reason:\*\* .*$") {
  [regex]::Replace($runtime, "(?m)^\*\*Reason:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $reason })
} else {
  $runtime + [System.Environment]::NewLine + $reason + [System.Environment]::NewLine
}
Write-TextRobust -Path $runtimeLogPath -Text $runtime

$check = Get-Content -Path $checklistPath -Raw
$c4Line = switch ($status) {
  "Done" { "- ``C4 Network degradation resilience`` - **Done** (3/3 cycles passed; reconnect stable; no critical consistency issues)" }
  "Partial" { "- ``C4 Network degradation resilience`` - **Partial** (>=2/3 cycles passed with no UI crash; complete 3/3 for Done)" }
  default { "- ``C4 Network degradation resilience`` - **Open** (reconnect < 2/3 or UI crash detected)" }
}
$check = [regex]::Replace($check, "(?m)^- .*C4.*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $c4Line })
Write-TextRobust -Path $checklistPath -Text $check

Write-Output "Processed C4 results successfully."
Write-Output "Reconnect successes: $reconnectCount/3"
Write-Output "C4 status: $status"
param(
  [string]$InputPath = "",
  [switch]$ShowHelp
)

if ($ShowHelp) { Write-Host "Parse C4_RESULTS network degradation cycles and update docs/status markdown."; exit 0 }
if ([string]::IsNullOrWhiteSpace($InputPath)) { Write-Host "InputPath is required." -ForegroundColor Red; exit 1 }
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (-not (Test-Path -Path $InputPath)) { throw "Input file not found: $InputPath" }

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeLogPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_RUNTIME_VALIDATION_LOG.md"
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"
function Write-TextRobust([string]$Path,[string]$Text) {
  try { [System.IO.File]::WriteAllText($Path, $Text, [System.Text.Encoding]::UTF8) } catch { $tmp="$Path.tmp"; [System.IO.File]::WriteAllText($tmp,$Text,[System.Text.Encoding]::UTF8); Move-Item -Path $tmp -Destination $Path -Force }
}
function ConvertTo-BoolValue([string]$Value) {
  $v = $Value.Trim().ToLowerInvariant()
  if ($v -in @("yes","y","true","1")) { return $true }
  if ($v -in @("no","n","false","0")) { return $false }
  throw "Invalid bool value: $Value"
}

$lines = (Get-Content -Path $InputPath -Raw) -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 }
$cycles = @{}
foreach ($line in $lines) {
  if ($line.Trim() -eq "C4_RESULTS") { continue }
  $m = [regex]::Match($line.Trim(), '^cycle(?<idx>[1-3]):\s*duration=(?<duration>[^,]+),\s*reconnect=(?<reconnect>[^,]+),\s*duplicates=(?<duplicates>[^,]+),\s*criticalLoss=(?<loss>[^,]+),\s*uiCrash=(?<crash>[^,]+),\s*note=(?<note>.+)$')
  if (-not $m.Success) { continue }
  $idx = [int]$m.Groups["idx"].Value
  $cycles[$idx] = @{ duration=$m.Groups["duration"].Value.Trim(); reconnect=ConvertTo-BoolValue $m.Groups["reconnect"].Value; duplicates=ConvertTo-BoolValue $m.Groups["duplicates"].Value; loss=ConvertTo-BoolValue $m.Groups["loss"].Value; crash=ConvertTo-BoolValue $m.Groups["crash"].Value; note=$m.Groups["note"].Value.Trim() }
}
foreach ($idx in 1..3) { if (-not $cycles.ContainsKey($idx)) { throw "Missing cycle result: cycle$idx" } }
$reconnectCount = 0; $hasCrash = $false; $hasDup = $false; $hasLoss = $false
foreach ($idx in 1..3) { $c=$cycles[$idx]; if ($c.reconnect) { $reconnectCount++ }; if ($c.crash) { $hasCrash=$true }; if ($c.duplicates) { $hasDup=$true }; if ($c.loss) { $hasLoss=$true } }
$status = "Open"; if ($reconnectCount -eq 3 -and -not $hasCrash -and -not $hasDup -and -not $hasLoss) { $status = "Done" } elseif ($reconnectCount -ge 2 -and -not $hasCrash) { $status = "Partial" }

$runtime = Get-Content -Path $runtimeLogPath -Raw
foreach ($idx in 1..3) {
  $c = $cycles[$idx]; $reconnectCell = if ($c.reconnect) { "OK" } else { "FAIL" }; $dupCell = if ($c.duplicates) { "YES" } else { "NO" }; $lossCell = if ($c.loss) { "YES" } else { "NO" }
  $row = "| $idx | Slow 3G + Offline | ``$($c.duration)`` | ``$reconnectCell`` | ``$dupCell`` | ``$lossCell`` | ``$(if ($c.reconnect -and -not $c.crash) { "PASS" } else { "FAIL" })`` |"
  $runtime = [regex]::Replace($runtime, "(?m)^\| $idx \| Slow 3G \+ Offline \|.*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $row })
}
$runtime = if ($runtime -match "(?m)^\*\*Status:\*\* .*$") { [regex]::Replace($runtime, "(?m)^\*\*Status:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) "**Status:** ``$status``  " }) } else { $runtime + [System.Environment]::NewLine + "**Status:** ``$status``  " }
Write-TextRobust -Path $runtimeLogPath -Text $runtime

$check = Get-Content -Path $checklistPath -Raw
$c4Line = switch ($status) { "Done" { "- ``C4 Network degradation resilience`` - **Done** (3/3 cycles passed; reconnect stable; no critical consistency issues)" } "Partial" { "- ``C4 Network degradation resilience`` - **Partial** (>=2/3 cycles passed with no UI crash; complete 3/3 for Done)" } default { "- ``C4 Network degradation resilience`` - **Open** (reconnect < 2/3 or UI crash detected)" } }
$check = [regex]::Replace($check, "(?m)^- .*C4.*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $c4Line })
Write-TextRobust -Path $checklistPath -Text $check
Write-Output "Processed C4 results successfully."
param(
  [string]$InputPath = "",
  [switch]$ShowHelp
)

if ($ShowHelp) { Write-Host "Parse C4_RESULTS network degradation cycles and update docs/status markdown."; exit 0 }
if ([string]::IsNullOrWhiteSpace($InputPath)) { Write-Host "InputPath is required." -ForegroundColor Red; exit 1 }

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if (-not (Test-Path -Path $InputPath)) { throw "Input file not found: $InputPath" }

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeLogPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_RUNTIME_VALIDATION_LOG.md"
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"

function Write-TextRobust {
  param([string]$Path, [string]$Text)
  try { [System.IO.File]::WriteAllText($Path, $Text, [System.Text.Encoding]::UTF8) }
  catch {
    $tmp = "$Path.tmp"
    [System.IO.File]::WriteAllText($tmp, $Text, [System.Text.Encoding]::UTF8)
    Move-Item -Path $tmp -Destination $Path -Force
  }
}

function ConvertTo-BoolValue {
  param([string]$Value)
  $v = $Value.Trim().ToLowerInvariant()
  if ($v -in @("yes", "y", "true", "1")) { return $true }
  if ($v -in @("no", "n", "false", "0")) { return $false }
  throw "Invalid bool value: $Value"
}

$lines = (Get-Content -Path $InputPath -Raw) -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 }
$cycles = @{}
foreach ($line in $lines) {
  if ($line.Trim() -eq "C4_RESULTS") { continue }
  $m = [regex]::Match($line.Trim(), '^cycle(?<idx>[1-3]):\s*duration=(?<duration>[^,]+),\s*reconnect=(?<reconnect>[^,]+),\s*duplicates=(?<duplicates>[^,]+),\s*criticalLoss=(?<loss>[^,]+),\s*uiCrash=(?<crash>[^,]+),\s*note=(?<note>.+)$')
  if (-not $m.Success) { continue }
  $idx = [int]$m.Groups["idx"].Value
  $cycles[$idx] = @{
    duration = $m.Groups["duration"].Value.Trim()
    reconnect = ConvertTo-BoolValue $m.Groups["reconnect"].Value
    duplicates = ConvertTo-BoolValue $m.Groups["duplicates"].Value
    loss = ConvertTo-BoolValue $m.Groups["loss"].Value
    crash = ConvertTo-BoolValue $m.Groups["crash"].Value
    note = $m.Groups["note"].Value.Trim()
  }
}

foreach ($idx in 1..3) { if (-not $cycles.ContainsKey($idx)) { throw "Missing cycle result: cycle$idx" } }

$reconnectCount = 0
$hasCrash = $false
$hasDup = $false
$hasLoss = $false
foreach ($idx in 1..3) {
  $c = $cycles[$idx]
  if ($c.reconnect) { $reconnectCount++ }
  if ($c.crash) { $hasCrash = $true }
  if ($c.duplicates) { $hasDup = $true }
  if ($c.loss) { $hasLoss = $true }
}

$status = "Open"
if ($reconnectCount -eq 3 -and -not $hasCrash -and -not $hasDup -and -not $hasLoss) { $status = "Done" }
elseif ($reconnectCount -ge 2 -and -not $hasCrash) { $status = "Partial" }

$runtime = Get-Content -Path $runtimeLogPath -Raw
foreach ($idx in 1..3) {
  $c = $cycles[$idx]
  $reconnectCell = if ($c.reconnect) { "OK" } else { "FAIL" }
  $dupCell = if ($c.duplicates) { "YES" } else { "NO" }
  $lossCell = if ($c.loss) { "YES" } else { "NO" }
  $row = "| $idx | Slow 3G + Offline | ``$($c.duration)`` | ``$reconnectCell`` | ``$dupCell`` | ``$lossCell`` | ``$(if ($c.reconnect -and -not $c.crash) { "PASS" } else { "FAIL" })`` |"
  $runtime = [regex]::Replace($runtime, "(?m)^\| $idx \| Slow 3G \+ Offline \|.*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $row })
  $noteLine = "- Cycle ${idx}: ``$($c.note.Replace('`r',' ').Replace('`n',' '))``"
  $runtime = [regex]::Replace($runtime, "(?m)^- Cycle ${idx}: .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $noteLine })
}

$runtime = if ($runtime -match "(?m)^\*\*Status:\*\* .*$") { [regex]::Replace($runtime, "(?m)^\*\*Status:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) "**Status:** ``$status``  " }) } else { $runtime + [System.Environment]::NewLine + "**Status:** ``$status``  " }
$runtime = if ($runtime -match "(?m)^\*\*Decision:\*\* .*$") { [regex]::Replace($runtime, "(?m)^\*\*Decision:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) "**Decision:** ``C4 -> $status``  " }) } else { $runtime + [System.Environment]::NewLine + "**Decision:** ``C4 -> $status``  " }
$reason = if ($status -eq "Done") { "**Reason:** ``3/3 reconnect cycles passed; no UI crash, duplicates, or critical loss.``" } elseif ($status -eq "Partial") { "**Reason:** ``At least 2/3 reconnect cycles passed with no UI crash.``" } else { "**Reason:** ``Partial threshold not met: reconnect < 2/3 or UI crash detected.``" }
$runtime = if ($runtime -match "(?m)^\*\*Reason:\*\* .*$") { [regex]::Replace($runtime, "(?m)^\*\*Reason:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $reason }) } else { $runtime + [System.Environment]::NewLine + $reason + [System.Environment]::NewLine }
Write-TextRobust -Path $runtimeLogPath -Text $runtime

$check = Get-Content -Path $checklistPath -Raw
$c4Line = switch ($status) {
  "Done" { "- ``C4 Network degradation resilience`` - **Done** (3/3 cycles passed; reconnect stable; no critical consistency issues)" }
  "Partial" { "- ``C4 Network degradation resilience`` - **Partial** (>=2/3 cycles passed with no UI crash; complete 3/3 for Done)" }
  default { "- ``C4 Network degradation resilience`` - **Open** (reconnect < 2/3 or UI crash detected)" }
}
$check = [regex]::Replace($check, "(?m)^- .*C4.*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $c4Line })
Write-TextRobust -Path $checklistPath -Text $check

Write-Output "Processed C4 results successfully."
Write-Output "Reconnect successes: $reconnectCount/3"
Write-Output "C4 status: $status"
