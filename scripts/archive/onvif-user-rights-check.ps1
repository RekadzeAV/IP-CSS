# ONVIF user rights probe: GetServices, GetEventProperties, CreatePullPointSubscription (cameras from JSON).
# File is UTF-8 with BOM so Windows PowerShell 5.1 parses Russian recommendation strings in the body.
#
# Usage:
#   .\scripts\onvif-user-rights-check.ps1

param(
    [string]$ConfigPath = "config\test-cameras.local.json",
    [int]$TimeoutSec = 8,
    [string]$ReportDir = "diagnostics\onvif-rights",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "ONVIF user rights check against cameras in test-cameras JSON (GetServices, GetEventProperties, CreatePullPoint)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\onvif-user-rights-check.ps1 -ShowHelp"
    Write-Host "  .\scripts\onvif-user-rights-check.ps1 [-ConfigPath <json>] [-TimeoutSec N] [-ReportDir <dir>]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ConfigPath  Camera list (default config\test-cameras.local.json)"
    Write-Host "  -TimeoutSec  SOAP timeout"
    Write-Host "  -ReportDir   Root for per-run folders (default diagnostics\onvif-rights)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed; writes summary.json under a timestamped subfolder"
    Write-Host "  1  Config missing or no cameras"
    exit 0
}

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ResolvedConfigPath = if ([System.IO.Path]::IsPathRooted($ConfigPath)) { $ConfigPath } else { Join-Path $ProjectRoot $ConfigPath }
$runTimestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ResolvedReportRoot = if ([System.IO.Path]::IsPathRooted($ReportDir)) { $ReportDir } else { Join-Path $ProjectRoot $ReportDir }
$ResolvedRunReportDir = Join-Path $ResolvedReportRoot $runTimestamp

if (-not (Test-Path $ResolvedConfigPath)) {
    Write-Host "Config not found: $ResolvedConfigPath" -ForegroundColor Red
    exit 1
}

$config = Get-Content $ResolvedConfigPath -Raw | ConvertFrom-Json
$cameras = @($config.cameras)
if ($cameras.Count -eq 0) {
    Write-Host "No cameras in config" -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Path $ResolvedRunReportDir -Force | Out-Null

$defaultUser = $config.defaults.username
$defaultPass = $config.defaults.password
$httpPort = if ($config.defaults.httpPorts -and $config.defaults.httpPorts.Count -gt 0) { [int]$config.defaults.httpPorts[0] } else { 80 }

$soapGetServices = '<?xml version="1.0" encoding="UTF-8"?><s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"><s:Body><tds:GetServices xmlns:tds="http://www.onvif.org/ver10/device/wsdl"><tds:IncludeCapability>true</tds:IncludeCapability></tds:GetServices></s:Body></s:Envelope>'
$soapGetEventProperties = '<?xml version="1.0" encoding="UTF-8"?><soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:tev="http://www.onvif.org/ver10/events/wsdl"><soap:Body><tev:GetEventProperties/></soap:Body></soap:Envelope>'
$soapCreatePullPoint = '<?xml version="1.0" encoding="UTF-8"?><soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:tev="http://www.onvif.org/ver10/events/wsdl" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"><soap:Body><tev:CreatePullPointSubscription><wsnt:InitialTerminationTime>PT60S</wsnt:InitialTerminationTime></tev:CreatePullPointSubscription></soap:Body></soap:Envelope>'

function Invoke-BasicSoap {
    param(
        [string]$Url,
        [string]$Body,
        [string]$Username,
        [string]$Secret,
        [int]$Timeout
    )

    try {
        $pair = "$Username`:$Secret"
        $b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
        $resp = Invoke-WebRequest `
            -Uri $Url `
            -Method Post `
            -Headers @{ Authorization = "Basic $b64"; SOAPAction = '""' } `
            -ContentType "text/xml; charset=utf-8" `
            -Body $Body `
            -TimeoutSec $Timeout `
            -UseBasicParsing
        return [PSCustomObject]@{
            Status = [int]$resp.StatusCode
            Body = $resp.Content
            Success = ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300)
        }
    } catch {
        $statusCode = 0
        try {
            if ($_.Exception.Response) { $statusCode = $_.Exception.Response.StatusCode.value__ }
        } catch { }
        return [PSCustomObject]@{
            Status = $statusCode
            Body = ""
            Success = $false
        }
    }
}

$rows = @()

foreach ($cam in $cameras) {
    $hostName = $cam.host
    $user = if ($cam.username) { $cam.username } else { $defaultUser }
    $pass = if ($cam.password) { $cam.password } else { $defaultPass }
    $deviceUrl = "http://${hostName}:$httpPort/onvif/device_service"

    $getServices = Invoke-BasicSoap -Url $deviceUrl -Body $soapGetServices -Username $user -Secret $pass -Timeout $TimeoutSec
    $eventUrl = ""
    if ($getServices.Success) {
        $m = [regex]::Match(
            $getServices.Body,
            '<(?:\w+:)?Namespace[^>]*>\s*http://www\.onvif\.org/ver10/events/wsdl\s*</(?:\w+:)?Namespace>.*?<(?:\w+:)?XAddr[^>]*>([^<]+)</(?:\w+:)?XAddr>',
            [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Singleline
        )
        if ($m.Success) { $eventUrl = $m.Groups[1].Value.Trim() }
    }
    if (-not $eventUrl) { $eventUrl = "http://${hostName}:$httpPort/onvif/events_service" }

    $eventProps = Invoke-BasicSoap -Url $eventUrl -Body $soapGetEventProperties -Username $user -Secret $pass -Timeout $TimeoutSec
    $createPull = Invoke-BasicSoap -Url $eventUrl -Body $soapCreatePullPoint -Username $user -Secret $pass -Timeout $TimeoutSec

    $recommendation = ""
    if ($getServices.Status -eq 0) {
        $recommendation = "Камера не отвечает по ONVIF: проверьте сеть, ONVIF enable и порт."
    } elseif ($getServices.Status -ge 200 -and $getServices.Status -lt 300 -and $eventProps.Status -eq 401) {
        $recommendation = "Дать пользователю ONVIF-права на Events (GetEventProperties/PullPoint) или использовать другой ONVIF-аккаунт."
    } elseif ($eventProps.Status -ge 200 -and $eventProps.Status -lt 300 -and $createPull.Status -eq 401) {
        $recommendation = "Права чтения событий есть, но нет прав подписки: разрешить PullPointSubscription."
    } elseif ($createPull.Status -ge 200 -and $createPull.Status -lt 300) {
        $recommendation = "OK: доступ к ONVIF Events и PullPoint подтвержден."
    } else {
        $recommendation = "Проверить ONVIF Event endpoint, security policy (digest/wsse), и профиль пользователя."
    }

    $rows += [PSCustomObject]@{
        Host = $hostName
        User = $user
        EventUrl = $eventUrl
        GetServices = $getServices.Status
        GetEventProperties = $eventProps.Status
        CreatePullPoint = $createPull.Status
        Recommendation = $recommendation
    }
}

$total = $rows.Count
$okServices = @($rows | Where-Object { $_.GetServices -ge 200 -and $_.GetServices -lt 300 }).Count
$okEventProps = @($rows | Where-Object { $_.GetEventProperties -ge 200 -and $_.GetEventProperties -lt 300 }).Count
$okCreatePull = @($rows | Where-Object { $_.CreatePullPoint -ge 200 -and $_.CreatePullPoint -lt 300 }).Count

Write-Host ""
Write-Host "=== ONVIF USER RIGHTS CHECK ===" -ForegroundColor Cyan
$rows | Format-Table -AutoSize
Write-Host ""
Write-Host "GetServices:        $okServices/$total ($([math]::Round(($okServices*100.0)/$total,1))%)" -ForegroundColor Green
Write-Host "GetEventProperties: $okEventProps/$total ($([math]::Round(($okEventProps*100.0)/$total,1))%)" -ForegroundColor Yellow
Write-Host "CreatePullPoint:    $okCreatePull/$total ($([math]::Round(($okCreatePull*100.0)/$total,1))%)" -ForegroundColor Yellow

$summary = [PSCustomObject]@{
    generatedAt = (Get-Date).ToString("s")
    configPath = $ResolvedConfigPath
    totals = [PSCustomObject]@{
        total = $total
        getServicesPct = [math]::Round(($okServices*100.0)/$total,1)
        getEventPropertiesPct = [math]::Round(($okEventProps*100.0)/$total,1)
        createPullPointPct = [math]::Round(($okCreatePull*100.0)/$total,1)
    }
    rows = $rows
}

$summaryPath = Join-Path $ResolvedRunReportDir "summary.json"
$summary | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryPath -Encoding UTF8
Write-Host ""
Write-Host "Report saved: $summaryPath" -ForegroundColor Cyan
