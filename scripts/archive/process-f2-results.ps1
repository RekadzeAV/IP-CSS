param(
  [string]$InputPath = "",
  [switch]$ShowHelp
)

if ($ShowHelp) {
  Write-Host "Parse F2_RESULTS web perf lines from a text file and update docs/status WEB_INTERFACE_* markdown."
  Write-Host ""
  Write-Host "Usage:"
  Write-Host "  .\scripts\process-f2-results.ps1 -ShowHelp"
  Write-Host "  .\scripts\process-f2-results.ps1 -InputPath <path-to-f2-results.txt>"
  Write-Host ""
  Write-Host "Input format:"
  Write-Host "  Lines like: /dashboard: perf=..., lcp=..., inp_or_tbt=..., cls=..., longTasks=..."
  Write-Host ""
  Write-Host "Exit codes:"
  Write-Host "  0  Docs updated"
  Write-Host "  1  Missing input, bad parse, or missing page rows"
  exit 0
}

if ([string]::IsNullOrWhiteSpace($InputPath)) {
  Write-Host "InputPath is required. Use: .\scripts\process-f2-results.ps1 -ShowHelp" -ForegroundColor Red
  exit 1
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $InputPath)) {
  throw "Input file not found: $InputPath"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$perfReportPath = "$repoRoot/docs/status/WEB_INTERFACE_PERF_REPORT.md"
$checklistPath = "$repoRoot/docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md"

$raw = Get-Content -Path $InputPath -Raw
$lines = $raw -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 }

$pageOrder = @("dashboard", "cameras", "events", "recordings")
$results = @{}

function ConvertTo-NumberOrNull {
  param([string]$Value)
  $normalized = $Value.Trim().Replace(",", ".")
  $out = 0.0
  if ([double]::TryParse($normalized, [System.Globalization.NumberStyles]::Float, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$out)) {
    return $out
  }
  return $null
}

foreach ($line in $lines) {
  if ($line -eq "F2_RESULTS") { continue }
  $pattern = '^/(?<page>dashboard|cameras|events|recordings):\s*perf=(?<perf>[^,]+),\s*lcp=(?<lcp>[^,]+),\s*inp_or_tbt=(?<inp>[^,]+),\s*cls=(?<cls>[^,]+),\s*longTasks=(?<long>.+)$'
  $match = [regex]::Match($line.Trim(), $pattern)
  if (-not $match.Success) { continue }
  $page = $match.Groups["page"].Value
  $results[$page] = @{
    perf = $match.Groups["perf"].Value.Trim()
    lcp = $match.Groups["lcp"].Value.Trim()
    inp = $match.Groups["inp"].Value.Trim()
    cls = $match.Groups["cls"].Value.Trim()
    long = $match.Groups["long"].Value.Trim()
  }
}

foreach ($page in $pageOrder) {
  if (-not $results.ContainsKey($page)) {
    throw "Missing page result: /$page"
  }
}

$perfNumbers = @()
$allLcpGood = $true
$hasBlockingSignals = $false

foreach ($page in $pageOrder) {
  $item = $results[$page]
  $perfVal = ConvertTo-NumberOrNull -Value $item.perf
  if ($null -eq $perfVal) {
    throw "Invalid perf for /${page}: '$($item.perf)'"
  }
  $perfNumbers += $perfVal

  $lcpVal = ConvertTo-NumberOrNull -Value $item.lcp
  if ($null -eq $lcpVal -or $lcpVal -gt 3.0) {
    $allLcpGood = $false
  }

  if ($item.long.ToLower().Contains("block") -or $item.long.ToLower().Contains("freeze")) {
    $hasBlockingSignals = $true
  }
}

$avgPerf = [math]::Round((($perfNumbers | Measure-Object -Average).Average), 1)

$status = "Open"
if ($avgPerf -ge 80 -and $allLcpGood -and -not $hasBlockingSignals) {
  $status = "Done"
} elseif ($avgPerf -ge 70 -and -not $hasBlockingSignals) {
  $status = "Partial"
}

$report = Get-Content -Path $perfReportPath -Raw
foreach ($page in $pageOrder) {
  $item = $results[$page]
  $rowResult = if ((ConvertTo-NumberOrNull -Value $item.perf) -ge 70) { "PASS" } else { "FAIL" }
  $newRow = "| /$page | ``$($item.perf)`` | ``$($item.lcp)`` | ``$($item.inp)`` | ``$($item.cls)`` | ``$($item.long)`` | ``$rowResult`` |"
  $report = [regex]::Replace(
    $report,
    "(?m)^\| /$page \|.*$",
    [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $newRow }
  )
}

$avgLine = "**Средний Performance:** ``$avgPerf``"
$report = [regex]::Replace($report, "(?m)^\*\*Средний Performance:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $avgLine })
$factLine = "**Фактическая оценка:** ``$status``"
$report = [regex]::Replace($report, "(?m)^\*\*Фактическая оценка:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $factLine })
$decisionLine = "**Решение:** ``F2 -> $status``  "
$report = [regex]::Replace($report, "(?m)^\*\*Решение:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $decisionLine })
$reasonLine = if ($status -eq "Done") {
  "**Обоснование:** ``Порог Done достигнут: средний score >= 80, LCP <= 3.0s, блокирующие long tasks не зафиксированы.``"
} elseif ($status -eq "Partial") {
  "**Обоснование:** ``Порог Partial достигнут: средний score >= 70, блокирующие UI-freeze не зафиксированы.``"
} else {
  "**Обоснование:** ``Порог Partial не достигнут или зафиксированы блокирующие риски.``"
}
$report = [regex]::Replace($report, "(?m)^\*\*Обоснование:\*\* .*$", [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $reasonLine })

Set-Content -Path $perfReportPath -Value $report -Encoding UTF8

$checklist = Get-Content -Path $checklistPath -Raw
$f2Line = switch ($status) {
  "Done" { "- ``F2 Производительность`` — **Done** (валидный performance pass выполнен, порог Done подтверждён)" }
  "Partial" { "- ``F2 Производительность`` — **Partial** (валидный performance pass выполнен, порог Partial подтверждён)" }
  default { "- ``F2 Производительность`` — **Open** (валидный performance pass выполнен, но порог Partial не достигнут)" }
}
$checklist = [regex]::Replace(
  $checklist,
  "(?m)^- `F2 Производительность` — .*$",
  [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $f2Line }
)
Set-Content -Path $checklistPath -Value $checklist -Encoding UTF8

Write-Output "Processed F2 results successfully."
Write-Output "Average performance: $avgPerf"
Write-Output "F2 status: $status"
