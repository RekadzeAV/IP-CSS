# Quick network-layer smoke against cameras from JSON (RTSP/HTTP, ONVIF GetCapabilities, Media/Events, optional PullPoint).
#
# Usage:
#   .\scripts\network-layer-smoke-test.ps1
#   .\scripts\network-layer-smoke-test.ps1 -ConfigPath "config\test-cameras.local.json"

param(
    [string]$ConfigPath = "config\test-cameras.local.json",
    [int]$TimeoutSec = 8,
    [bool]$TestPullPoint = $true,
    [bool]$DiagnosticMode = $false,
    [string]$ReportDir = "diagnostics\network-smoke",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host "Network-layer smoke: RTSP/HTTP reachability, ONVIF GetCapabilities (Basic auth), Media/Events in caps, optional PullPoint."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\network-layer-smoke-test.ps1 -ShowHelp"
    Write-Host '  .\scripts\network-layer-smoke-test.ps1 [-ConfigPath <json>] [-TimeoutSec N] [-TestPullPoint:$false] [-DiagnosticMode] [-ReportDir <dir>]'
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ConfigPath   Camera list (default config\test-cameras.local.json)"
    Write-Host "  -TimeoutSec   Per-request timeout"
    Write-Host "  -TestPullPoint  Exercise PullPoint create/pull when true (default)"
    Write-Host "  -DiagnosticMode  Write summary.json under timestamped report dir"
    Write-Host "  -ReportDir    Root for per-run folders (default diagnostics\network-smoke)"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed (review console table; no aggregate fail exit)"
    Write-Host "  1  Config missing/empty"
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
    Write-Host "Create it from config/test-cameras.example.json" -ForegroundColor Yellow
    exit 1
}

$config = Get-Content $ResolvedConfigPath -Raw | ConvertFrom-Json
$cameras = @($config.cameras)
if ($cameras.Count -eq 0) {
    Write-Host "No cameras in config: $ResolvedConfigPath" -ForegroundColor Red
    exit 1
}

$defaultUser = $config.defaults.username
$defaultPass = $config.defaults.password

$soap = '<?xml version="1.0" encoding="UTF-8"?><s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"><s:Body><tds:GetCapabilities xmlns:tds="http://www.onvif.org/ver10/device/wsdl"><tds:Category>All</tds:Category></tds:GetCapabilities></s:Body></s:Envelope>'
$pullPointCreateSoap = '<?xml version="1.0" encoding="UTF-8"?><soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:tev="http://www.onvif.org/ver10/events/wsdl" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"><soap:Body><tev:CreatePullPointSubscription><wsnt:InitialTerminationTime>PT60S</wsnt:InitialTerminationTime></tev:CreatePullPointSubscription></soap:Body></soap:Envelope>'
$pullMessagesSoap = '<?xml version="1.0" encoding="UTF-8"?><soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:tev="http://www.onvif.org/ver10/events/wsdl" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"><soap:Body><tev:PullMessages><wsnt:Timeout>PT1S</wsnt:Timeout><wsnt:MessageLimit>10</wsnt:MessageLimit></tev:PullMessages></soap:Body></soap:Envelope>'

$results = @()
$diagnostics = @()

function Get-EventServiceUrlsFromCapabilities {
    param(
        [string]$CapabilitiesXml,
        [string]$HostName,
        [int]$HttpPort
    )

    $urls = New-Object System.Collections.Generic.List[string]
    if ($CapabilitiesXml) {
        $eventMatches = [regex]::Matches(
            $CapabilitiesXml,
            '<(?:\w+:)?Events[^>]*>.*?<(?:\w+:)?XAddr[^>]*>([^<]+)</(?:\w+:)?XAddr>',
            [System.Text.RegularExpressions.RegexOptions]::IgnoreCase -bor [System.Text.RegularExpressions.RegexOptions]::Singleline
        )
        foreach ($m in $eventMatches) {
            $u = $m.Groups[1].Value.Trim()
            if ($u -and -not $urls.Contains($u)) { $urls.Add($u) }
        }
    }

    $fallback = @(
        "http://${HostName}:$HttpPort/onvif/events_service",
        "http://${HostName}:$HttpPort/onvif/event_service",
        "http://${HostName}:$HttpPort/onvif/events",
        "http://${HostName}:$HttpPort/onvif/EventService",
        "http://${HostName}:$HttpPort/onvif/event"
    )
    foreach ($u in $fallback) {
        if (-not $urls.Contains($u)) { $urls.Add($u) }
    }

    return @($urls)
}

if ($DiagnosticMode) {
    New-Item -ItemType Directory -Path $ResolvedRunReportDir -Force | Out-Null
}

function Get-Md5Hex([string]$text) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $hash = [System.Security.Cryptography.MD5]::Create().ComputeHash($bytes)
    return -join ($hash | ForEach-Object { $_.ToString("x2") })
}

function New-DigestAuthorizationHeader(
    [string]$WwwAuthenticate,
    [string]$Method,
    [string]$UriPath,
    [string]$Username,
    [string]$Secret
) {
    if (-not $WwwAuthenticate -or -not ($WwwAuthenticate -match "Digest")) { return $null }

    $realm = ([regex]::Match($WwwAuthenticate, 'realm="([^"]+)"', 'IgnoreCase')).Groups[1].Value
    $nonce = ([regex]::Match($WwwAuthenticate, 'nonce="([^"]+)"', 'IgnoreCase')).Groups[1].Value
    $qop = ([regex]::Match($WwwAuthenticate, 'qop="([^"]+)"', 'IgnoreCase')).Groups[1].Value
    $opaque = ([regex]::Match($WwwAuthenticate, 'opaque="([^"]+)"', 'IgnoreCase')).Groups[1].Value
    $algorithm = ([regex]::Match($WwwAuthenticate, 'algorithm=([^,\s]+)', 'IgnoreCase')).Groups[1].Value

    if (-not $realm -or -not $nonce) { return $null }
    if (-not $algorithm) { $algorithm = "MD5" }

    $nc = "00000001"
    $cnonce = [Guid]::NewGuid().ToString("N").Substring(0, 16)
    $ha1 = Get-Md5Hex("$Username`:$realm`:$Secret")
    $ha2 = Get-Md5Hex("$Method`:$UriPath")

    $response = ""
    $useQop = $false
    if ($qop) {
        $qopFirst = $qop.Split(",")[0].Trim()
        if ($qopFirst) {
            $response = Get-Md5Hex("$ha1`:$nonce`:$nc`:$cnonce`:$qopFirst`:$ha2")
            $qop = $qopFirst
            $useQop = $true
        }
    }
    if (-not $useQop) {
        $response = Get-Md5Hex("$ha1`:$nonce`:$ha2")
    }

    $parts = @(
        "Digest username=""$Username""",
        "realm=""$realm""",
        "nonce=""$nonce""",
        "uri=""$UriPath""",
        "response=""$response""",
        "algorithm=$algorithm"
    )
    if ($opaque) { $parts += "opaque=""$opaque""" }
    if ($useQop) {
        $parts += "qop=$qop"
        $parts += "nc=$nc"
        $parts += "cnonce=""$cnonce"""
    }

    return ($parts -join ", ")
}

function Save-DiagnosticArtifact {
    param(
        [string]$HostName,
        [string]$Stage,
        [string]$Url,
        [string]$AuthMode,
        [string]$SoapRequest,
        [string]$SoapResponse,
        [string]$StatusCode,
        [string]$ErrorMessage
    )

    if (-not $DiagnosticMode) { return }
    $safeHost = $HostName -replace "[^0-9A-Za-z\.\-_]", "_"
    $safeStage = $Stage -replace "[^0-9A-Za-z\.\-_]", "_"
    $safeAuth = if ($AuthMode) { $AuthMode } else { "none" }
    $baseName = "$safeHost-$safeStage-$safeAuth"

    $requestPath = Join-Path $ResolvedRunReportDir "$baseName-request.xml"
    $responsePath = Join-Path $ResolvedRunReportDir "$baseName-response.xml"
    $metaPath = Join-Path $ResolvedRunReportDir "$baseName-meta.txt"

    if ($SoapRequest) { Set-Content -Path $requestPath -Value $SoapRequest -Encoding UTF8 }
    if ($SoapResponse) { Set-Content -Path $responsePath -Value $SoapResponse -Encoding UTF8 }

    $meta = @(
        "host=$HostName"
        "stage=$Stage"
        "url=$Url"
        "authMode=$safeAuth"
        "statusCode=$StatusCode"
        "error=$ErrorMessage"
        "timestamp=$(Get-Date -Format s)"
    ) -join [Environment]::NewLine
    Set-Content -Path $metaPath -Value $meta -Encoding UTF8
}

function New-WsseSoapEnvelope {
    param(
        [string]$SoapBody,
        [string]$Username,
        [string]$Secret
    )

    $nonce = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes(([Guid]::NewGuid().ToString("N"))))
    $created = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $securityHeader = @"
<soap:Header xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
    <wsse:Security soap:mustUnderstand="1">
        <wsse:UsernameToken>
            <wsse:Username>$Username</wsse:Username>
            <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">$Secret</wsse:Password>
            <wsse:Nonce>$nonce</wsse:Nonce>
            <wsu:Created>$created</wsu:Created>
        </wsse:UsernameToken>
    </wsse:Security>
</soap:Header>
"@

    return $SoapBody -replace "<soap:Body>", "$securityHeader<soap:Body>"
}

function Invoke-OnvifSoapRequest {
    param(
        [string]$HostName,
        [string]$Stage,
        [string]$Url,
        [string]$SoapBody,
        [string]$Username,
        [string]$Secret,
        [int]$Timeout
    )

    $pair = "$Username`:$Secret"
    $b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
    $headers = @{ Authorization = "Basic $b64"; SOAPAction = '""' }

    try {
        $resp = Invoke-WebRequest `
            -Uri $Url `
            -Method Post `
            -Headers $headers `
            -ContentType 'text/xml; charset=utf-8' `
            -Body $SoapBody `
            -TimeoutSec $Timeout `
            -UseBasicParsing
        Save-DiagnosticArtifact -HostName $HostName -Stage $Stage -Url $Url -AuthMode "basic" -SoapRequest $SoapBody -SoapResponse $resp.Content -StatusCode ([string][int]$resp.StatusCode) -ErrorMessage ""
        return [PSCustomObject]@{
            Success = $true
            Response = $resp
            StatusCode = [int]$resp.StatusCode
            Content = $resp.Content
            AuthMode = "basic"
            Url = $Url
            ErrorMessage = ""
            WwwAuthenticate = ""
        }
    } catch {
        $rawErr = ""
        $statusCode = 0
        $wwwAuth = ""
        try {
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode.value__
                $wwwAuth = $_.Exception.Response.Headers["WWW-Authenticate"]
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $rawErr = $reader.ReadToEnd()
                }
            }
        } catch { }

        Save-DiagnosticArtifact -HostName $HostName -Stage $Stage -Url $Url -AuthMode "basic" -SoapRequest $SoapBody -SoapResponse $rawErr -StatusCode ([string]$statusCode) -ErrorMessage $_.Exception.Message

        try {
            if ($statusCode -eq 401) {
                if ($wwwAuth) {
                    $uriObj = [Uri]$Url
                    $uriPath = if ([string]::IsNullOrWhiteSpace($uriObj.PathAndQuery)) { "/" } else { $uriObj.PathAndQuery }
                    $digestAuth = New-DigestAuthorizationHeader `
                        -WwwAuthenticate $wwwAuth `
                        -Method "POST" `
                        -UriPath $uriPath `
                        -Username $Username `
                        -Secret $Secret
                    if ($digestAuth) {
                        $digestHeaders = @{ Authorization = $digestAuth; SOAPAction = '""' }
                        $digestResp = Invoke-WebRequest `
                            -Uri $Url `
                            -Method Post `
                            -Headers $digestHeaders `
                            -ContentType 'text/xml; charset=utf-8' `
                            -Body $SoapBody `
                            -TimeoutSec $Timeout `
                            -UseBasicParsing
                        Save-DiagnosticArtifact -HostName $HostName -Stage $Stage -Url $Url -AuthMode "digest" -SoapRequest $SoapBody -SoapResponse $digestResp.Content -StatusCode ([string][int]$digestResp.StatusCode) -ErrorMessage ""
                        return [PSCustomObject]@{
                            Success = $true
                            Response = $digestResp
                            StatusCode = [int]$digestResp.StatusCode
                            Content = $digestResp.Content
                            AuthMode = "digest"
                            Url = $Url
                            ErrorMessage = ""
                            WwwAuthenticate = $wwwAuth
                        }
                    }
                }
            }
        } catch {
            $digestErr = ""
            $digestStatus = 0
            try {
                if ($_.Exception.Response) {
                    $digestStatus = $_.Exception.Response.StatusCode.value__
                    $stream2 = $_.Exception.Response.GetResponseStream()
                    if ($stream2) {
                        $reader2 = New-Object System.IO.StreamReader($stream2)
                        $digestErr = $reader2.ReadToEnd()
                    }
                }
            } catch { }
            Save-DiagnosticArtifact -HostName $HostName -Stage $Stage -Url $Url -AuthMode "digest" -SoapRequest $SoapBody -SoapResponse $digestErr -StatusCode ([string]$digestStatus) -ErrorMessage $_.Exception.Message
            return [PSCustomObject]@{
                Success = $false
                Response = $null
                StatusCode = $digestStatus
                Content = $digestErr
                AuthMode = "digest"
                Url = $Url
                ErrorMessage = $_.Exception.Message
                WwwAuthenticate = $wwwAuth
            }
        }
        return [PSCustomObject]@{
            Success = $false
            Response = $null
            StatusCode = $statusCode
            Content = $rawErr
            AuthMode = "basic"
            Url = $Url
            ErrorMessage = $_.Exception.Message
            WwwAuthenticate = $wwwAuth
        }
    }
}

function Invoke-OnvifSoapWsseRequest {
    param(
        [string]$HostName,
        [string]$Stage,
        [string]$Url,
        [string]$SoapBody,
        [string]$Username,
        [string]$Secret,
        [int]$Timeout
    )

    $wsseSoap = New-WsseSoapEnvelope -SoapBody $SoapBody -Username $Username -Secret $Secret
    try {
        $resp = Invoke-WebRequest `
            -Uri $Url `
            -Method Post `
            -Headers @{ SOAPAction = '""' } `
            -ContentType 'application/soap+xml; charset=utf-8' `
            -Body $wsseSoap `
            -TimeoutSec $Timeout `
            -UseBasicParsing

        Save-DiagnosticArtifact -HostName $HostName -Stage $Stage -Url $Url -AuthMode "wsse" -SoapRequest $wsseSoap -SoapResponse $resp.Content -StatusCode ([string][int]$resp.StatusCode) -ErrorMessage ""
        return [PSCustomObject]@{
            Success = $true
            StatusCode = [int]$resp.StatusCode
            Content = $resp.Content
            AuthMode = "wsse"
            Url = $Url
            ErrorMessage = ""
        }
    } catch {
        $statusCode = 0
        $rawErr = ""
        try {
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode.value__
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $rawErr = $reader.ReadToEnd()
                }
            }
        } catch { }
        Save-DiagnosticArtifact -HostName $HostName -Stage $Stage -Url $Url -AuthMode "wsse" -SoapRequest $wsseSoap -SoapResponse $rawErr -StatusCode ([string]$statusCode) -ErrorMessage $_.Exception.Message
        return [PSCustomObject]@{
            Success = $false
            StatusCode = $statusCode
            Content = $rawErr
            AuthMode = "wsse"
            Url = $Url
            ErrorMessage = $_.Exception.Message
        }
    }
}

foreach ($cam in $cameras) {
    $hostName = $cam.host
    $rtspPort = if ($cam.rtspPort) { [int]$cam.rtspPort } else { 554 }
    $httpPort = 80
    if ($config.defaults.httpPorts -and $config.defaults.httpPorts.Count -gt 0) {
        $httpPort = [int]$config.defaults.httpPorts[0]
    }

    $username = if ($cam.username) { $cam.username } else { $defaultUser }
    $password = if ($cam.password) { $cam.password } else { $defaultPass }

    $rtspOk = $false
    $httpOk = $false
    $onvifOk = $false
    $mediaOk = $false
    $eventsOk = $false
    $pullPointOk = $false
    $onvifStatus = "ERROR"
    $pullPointStatus = "SKIP"

    try {
        $rtspOk = (Test-NetConnection -ComputerName $hostName -Port $rtspPort -WarningAction SilentlyContinue).TcpTestSucceeded
    } catch { }

    try {
        $httpOk = (Test-NetConnection -ComputerName $hostName -Port $httpPort -WarningAction SilentlyContinue).TcpTestSucceeded
    } catch { }

    if ($httpOk -and $username -and $password) {
        try {
            $url = "http://${hostName}:$httpPort/onvif/device_service"
            $cap = Invoke-OnvifSoapRequest -HostName $hostName -Stage "get-capabilities" -Url $url -SoapBody $soap -Username $username -Secret $password -Timeout $TimeoutSec
            $onvifStatus = if ($cap.StatusCode -gt 0) { [string]$cap.StatusCode } else { "ERROR" }
            if ($cap.Success -and $cap.StatusCode -ge 200 -and $cap.StatusCode -lt 300) {
                $onvifOk = $true
                $content = $cap.Content
                $mediaOk = ($content -match "Media" -or $content -match "media")
                $eventsOk = ($content -match "Events" -or $content -match "event")

                if ($TestPullPoint -and $eventsOk) {
                    try {
                        $eventUrls = Get-EventServiceUrlsFromCapabilities -CapabilitiesXml $content -HostName $hostName -HttpPort $httpPort

                        $createResp = $null
                        $createTrace = @()
                        foreach ($eventUrl in $eventUrls) {
                            $attempts = @()
                            $attempts += Invoke-OnvifSoapRequest -HostName $hostName -Stage "create-pullpoint" -Url $eventUrl -SoapBody $pullPointCreateSoap -Username $username -Secret $password -Timeout $TimeoutSec
                            $attempts += Invoke-OnvifSoapWsseRequest -HostName $hostName -Stage "create-pullpoint" -Url $eventUrl -SoapBody $pullPointCreateSoap -Username $username -Secret $password -Timeout $TimeoutSec

                            foreach ($attempt in $attempts) {
                                $createTrace += "$eventUrl [$($attempt.AuthMode)] => $($attempt.StatusCode)"
                                if ($attempt.Success -and $attempt.StatusCode -ge 200 -and $attempt.StatusCode -lt 300) {
                                    $createResp = $attempt
                                    break
                                }
                            }
                            if ($createResp) {
                                break
                            }
                        }

                        if ($createResp -and $createResp.Success -and $createResp.StatusCode -ge 200 -and $createResp.StatusCode -lt 300) {
                            $createBody = $createResp.Content
                            $subUrlMatch = [regex]::Match(
                                $createBody,
                                '<(?:\w+:)?Address[^>]*>([^<]+)</(?:\w+:)?Address>',
                                [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
                            )

                            if ($subUrlMatch.Success) {
                                $subscriptionUrl = $subUrlMatch.Groups[1].Value.Trim()
                                $pullResp = Invoke-OnvifSoapRequest -HostName $hostName -Stage "pull-messages" -Url $subscriptionUrl -SoapBody $pullMessagesSoap -Username $username -Secret $password -Timeout $TimeoutSec
                                if ($pullResp.Success -and $pullResp.StatusCode -ge 200 -and $pullResp.StatusCode -lt 300) {
                                    $pullPointOk = $true
                                    $pullPointStatus = [string]$pullResp.StatusCode
                                } else {
                                    $pullPointStatus = if ($pullResp.StatusCode -gt 0) { "HTTP $($pullResp.StatusCode)" } else { "ERROR" }
                                }
                            } else {
                                $pullPointStatus = "NoSubscriptionUrl"
                            }
                        } else {
                            if ($createTrace.Count -gt 0) {
                                $pullPointStatus = ($createTrace -join " | ")
                            } else {
                                $pullPointStatus = "CreatePullPointFailed"
                            }
                        }
                    } catch {
                        try {
                            $pps = $_.Exception.Response.StatusCode.value__
                            if ($pps) {
                                $pullPointStatus = "HTTP $pps"
                            } else {
                                $pullPointStatus = "ERROR"
                            }
                        } catch {
                            $pullPointStatus = "ERROR"
                        }
                    }
                }
            }
        } catch {
            try {
                $statusCode = $_.Exception.Response.StatusCode.value__
                if ($statusCode) {
                    $onvifStatus = [string]$statusCode
                }
            } catch { }
        }
    }

    $results += [PSCustomObject]@{
        Host = $hostName
        Rtsp = $rtspOk
        Http = $httpOk
        OnvifAuth = $onvifOk
        OnvifStatus = $onvifStatus
        Media = $mediaOk
        Events = $eventsOk
        PullPoint = $pullPointOk
        PullPointStatus = $pullPointStatus
    }

    if ($DiagnosticMode) {
        $diagnostics += [PSCustomObject]@{
            Host = $hostName
            OnvifStatus = $onvifStatus
            Events = $eventsOk
            PullPointStatus = $pullPointStatus
        }
    }
}

$total = $results.Count
$rtspCnt = @($results | Where-Object { $_.Rtsp }).Count
$httpCnt = @($results | Where-Object { $_.Http }).Count
$onvifCnt = @($results | Where-Object { $_.OnvifAuth }).Count
$mediaEventsCnt = @($results | Where-Object { $_.Media -and $_.Events }).Count
$pullPointCnt = @($results | Where-Object { $_.PullPoint }).Count

$rtspPct = [math]::Round(($rtspCnt * 100.0) / $total, 1)
$httpPct = [math]::Round(($httpCnt * 100.0) / $total, 1)
$onvifPct = [math]::Round(($onvifCnt * 100.0) / $total, 1)
$mediaEventsPct = [math]::Round(($mediaEventsCnt * 100.0) / $total, 1)
$pullPointPct = [math]::Round(($pullPointCnt * 100.0) / $total, 1)
$networkLayerPct = [math]::Round((($rtspPct + $httpPct + $onvifPct + $mediaEventsPct + $pullPointPct) / 5.0), 1)

Write-Host ""
Write-Host "=== NETWORK LAYER SMOKE TEST ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize
Write-Host ""
Write-Host "RTSP availability:      $rtspCnt/$total ($rtspPct%)" -ForegroundColor Green
Write-Host "HTTP availability:      $httpCnt/$total ($httpPct%)" -ForegroundColor Green
Write-Host "ONVIF auth (HTTP 2xx):  $onvifCnt/$total ($onvifPct%)" -ForegroundColor Green
Write-Host "Media+Events support:   $mediaEventsCnt/$total ($mediaEventsPct%)" -ForegroundColor Yellow
Write-Host "PullPoint (create+pull): $pullPointCnt/$total ($pullPointPct%)" -ForegroundColor Yellow
Write-Host ""
Write-Host "Estimated 1.4 readiness: $networkLayerPct%" -ForegroundColor Cyan

if ($DiagnosticMode) {
    $summaryPath = Join-Path $ResolvedRunReportDir "summary.json"
    $summary = [PSCustomObject]@{
        generatedAt = (Get-Date).ToString("s")
        configPath = $ResolvedConfigPath
        totals = [PSCustomObject]@{
            total = $total
            rtspPct = $rtspPct
            httpPct = $httpPct
            onvifPct = $onvifPct
            mediaEventsPct = $mediaEventsPct
            pullPointPct = $pullPointPct
            networkLayerPct = $networkLayerPct
        }
        rows = $results
        diagnostics = $diagnostics
    }
    $summary | ConvertTo-Json -Depth 8 | Set-Content -Path $summaryPath -Encoding UTF8
    Write-Host "Diagnostic report saved: $ResolvedRunReportDir" -ForegroundColor Cyan
}

