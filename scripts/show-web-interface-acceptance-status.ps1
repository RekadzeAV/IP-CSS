param(
  [switch]$AsJson,
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Show current web interface 1.6 acceptance status."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1 -AsJson"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1 -ShowHelp"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"

if (-not (Test-Path -Path $checklistPath)) {
  throw "Checklist file not found: $checklistPath"
}

$content = Get-Content -Path $checklistPath -Raw

function Get-ItemStatus {
  param(
    [string]$Text,
    [string]$Id
  )
  $pattern = "(?m)^- .*" + [regex]::Escape($Id) + ".*\*\*(Done|Partial|Open)\*\*"
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) {
    return $match.Groups[1].Value
  }
  return "UNKNOWN"
}

function Get-OverallStatus {
  param([string]$Text)
  $match = [regex]::Match(
    $Text,
    '`(CONDITIONAL GO|NO-GO|GO|OPEN|PARTIAL|DONE)`',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
  )
  if ($match.Success) {
    return $match.Groups[1].Value
  }
  return "UNKNOWN"
}

$f2 = Get-ItemStatus -Text $content -Id "F2"
$c4 = Get-ItemStatus -Text $content -Id "C4"
$overall = Get-OverallStatus -Text $content

$result = [ordered]@{
  checklist = $checklistPath
  overall = $overall
  F2 = $f2
  C4 = $c4
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 3
  exit 0
}

Write-Host "Web Interface 1.6 status:"
Write-Host "  overall: $overall"
Write-Host "  F2: $f2"
Write-Host "  C4: $c4"
Write-Host "  checklist: $checklistPath"
param(
  [switch]$AsJson,
  [switch]$ShowHelp
)

if ($ShowHelp) { Write-Host "Show current web interface 1.6 acceptance status."; exit 0 }

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"
if (-not (Test-Path -Path $checklistPath)) { throw "Checklist file not found: $checklistPath" }
$content = Get-Content -Path $checklistPath -Raw

function Get-ItemStatus {
  param([string]$Text, [string]$Id)
  $m = [regex]::Match($Text, "(?m)^- .*" + [regex]::Escape($Id) + ".*\*\*(Done|Partial|Open)\*\*")
  if ($m.Success) { return $m.Groups[1].Value }
  return "UNKNOWN"
}

function Get-OverallStatus {
  param([string]$Text)
  $m = [regex]::Match($Text, '`(CONDITIONAL GO|NO-GO|GO|OPEN|PARTIAL|DONE)`', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
  if ($m.Success) { return $m.Groups[1].Value }
  return "UNKNOWN"
}

$result = [ordered]@{ checklist=$checklistPath; overall=(Get-OverallStatus -Text $content); F2=(Get-ItemStatus -Text $content -Id "F2"); C4=(Get-ItemStatus -Text $content -Id "C4") }
if ($AsJson) { $result | ConvertTo-Json -Depth 3; exit 0 }
Write-Host "Web Interface 1.6 status:"
Write-Host "  overall: $($result.overall)"
Write-Host "  F2: $($result.F2)"
Write-Host "  C4: $($result.C4)"
Write-Host "  checklist: $checklistPath"
param(
  [switch]$AsJson,
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Show current web interface 1.6 acceptance status."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1 -AsJson"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"
if (-not (Test-Path -Path $checklistPath)) {
  throw "Checklist file not found: $checklistPath"
}

$content = Get-Content -Path $checklistPath -Raw

function Get-ItemStatus {
  param([string]$Text, [string]$Id)
  $pattern = "(?m)^- .*" + [regex]::Escape($Id) + ".*\*\*(Done|Partial|Open)\*\*"
  $m = [regex]::Match($Text, $pattern)
  if ($m.Success) { return $m.Groups[1].Value }
  return "UNKNOWN"
}

function Get-OverallStatus {
  param([string]$Text)
  $m = [regex]::Match($Text, '`(CONDITIONAL GO|NO-GO|GO|OPEN|PARTIAL|DONE)`', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
  if ($m.Success) { return $m.Groups[1].Value }
  return "UNKNOWN"
}

$result = [ordered]@{
  checklist = $checklistPath
  overall = (Get-OverallStatus -Text $content)
  F2 = (Get-ItemStatus -Text $content -Id "F2")
  C4 = (Get-ItemStatus -Text $content -Id "C4")
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 3
  exit 0
}

Write-Host "Web Interface 1.6 status:"
Write-Host "  overall: $($result.overall)"
Write-Host "  F2: $($result.F2)"
Write-Host "  C4: $($result.C4)"
Write-Host "  checklist: $checklistPath"
param(
  [switch]$AsJson,
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Show current web interface 1.6 acceptance status."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1 -AsJson"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"
if (-not (Test-Path -Path $checklistPath)) {
  throw "Checklist file not found: $checklistPath"
}

$content = Get-Content -Path $checklistPath -Raw

function Get-ItemStatus([string]$Text, [string]$Id) {
  $pattern = "(?m)^- .*" + [regex]::Escape($Id) + ".*\*\*(Done|Partial|Open)\*\*"
  $m = [regex]::Match($Text, $pattern)
  if ($m.Success) { return $m.Groups[1].Value }
  return "UNKNOWN"
}

function Get-OverallStatus([string]$Text) {
  $m = [regex]::Match($Text, '`(CONDITIONAL GO|NO-GO|GO|OPEN|PARTIAL|DONE)`', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
  if ($m.Success) { return $m.Groups[1].Value }
  return "UNKNOWN"
}

$result = [ordered]@{
  checklist = $checklistPath
  overall = (Get-OverallStatus -Text $content)
  F2 = (Get-ItemStatus -Text $content -Id "F2")
  C4 = (Get-ItemStatus -Text $content -Id "C4")
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 3
  exit 0
}

Write-Host "Web Interface 1.6 status:"
Write-Host "  overall: $($result.overall)"
Write-Host "  F2: $($result.F2)"
Write-Host "  C4: $($result.C4)"
Write-Host "  checklist: $checklistPath"
param(
  [switch]$AsJson,
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Show current web interface 1.6 acceptance status."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1 -AsJson"
  Write-Host "  .\scripts\show-web-interface-acceptance-status.ps1 -ShowHelp"
  exit 0
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$checklistPath = Join-Path $repoRoot "docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"

if (-not (Test-Path -Path $checklistPath)) {
  throw "Checklist file not found: $checklistPath"
}

$content = Get-Content -Path $checklistPath -Raw

function Get-ItemStatus {
  param(
    [string]$Text,
    [string]$Id
  )
  $pattern = "(?m)^- .*" + [regex]::Escape($Id) + ".*\*\*(Done|Partial|Open)\*\*"
  $match = [regex]::Match($Text, $pattern)
  if ($match.Success) {
    return $match.Groups[1].Value
  }
  return "UNKNOWN"
}

function Get-OverallStatus {
  param([string]$Text)
  $match = [regex]::Match(
    $Text,
    '`(CONDITIONAL GO|NO-GO|GO|OPEN|PARTIAL|DONE)`',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
  )
  if ($match.Success) {
    return $match.Groups[1].Value
  }
  return "UNKNOWN"
}

$f2 = Get-ItemStatus -Text $content -Id "F2"
$c4 = Get-ItemStatus -Text $content -Id "C4"
$overall = Get-OverallStatus -Text $content

$result = [ordered]@{
  checklist = $checklistPath
  overall = $overall
  F2 = $f2
  C4 = $c4
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 3
  exit 0
}

Write-Host "Web Interface 1.6 status:"
Write-Host "  overall: $overall"
Write-Host "  F2: $f2"
Write-Host "  C4: $c4"
Write-Host "  checklist: $checklistPath"

