#!/usr/bin/env pwsh
# W1-1.2: Smoke С‚РµСЃС‚С‹ РїРѕСЃР»Рµ РјРёРіСЂР°С†РёРё Р‘Р”
# РќР°Р·РЅР°С‡РµРЅРёРµ: РїСЂРѕРІРµСЂРєР° Р±Р°Р·РѕРІС‹С… РѕРїРµСЂР°С†РёР№ РїРѕСЃР»Рµ РјРёРіСЂР°С†РёРё SQLDelight/PostgreSQL

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiVersion = "v1",
    [switch]$FullSmoke
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "W1-1.2: Smoke С‚РµСЃС‚С‹ РїРѕСЃР»Рµ РјРёРіСЂР°С†РёРё Р‘Р”" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Р‘Р°Р·РѕРІС‹Р№ URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "РџРѕР»РЅС‹Р№ СЂРµР¶РёРј: $FullSmoke" -ForegroundColor Yellow
Write-Host ""

$ErrorActionPreference = "Stop"
$passedTests = 0
$failedTests = 0
$testResults = @()

# Р¤СѓРЅРєС†РёСЏ РґР»СЏ РІС‹РїРѕР»РЅРµРЅРёСЏ HTTP Р·Р°РїСЂРѕСЃР°
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    $uri = "$BaseUrl/api/$ApiVersion$Path"
    
    try {
        $headers["Content-Type"] = "application/json"
        $params = @{
            Method = $Method
            Uri = $uri
            Headers = $Headers
            UseBasicParsing = $true
        }
        
        if ($Body) {
            $params["Body"] = $Body | ConvertTo-Json -Depth 10
        }
        
        $response = Invoke-RestMethod @params
        return @{
            Success = $true
            Data = $response
            StatusCode = 200
        }
    } catch {
        return @{
            Success = $false
            Error = $_.Exception.Message
            StatusCode = $_.Exception.Response.StatusCode.value__
        }
    }
}

# РўРµСЃС‚ 1: Health check
Write-Host "`n[TEST 1] Health Check (Live)" -ForegroundColor White
$result = Invoke-ApiRequest -Method "Get" -Path "/health/live"
if ($result.Success) {
    Write-Host "  вњ“ Health check passed" -ForegroundColor Green
    $passedTests++
    $testResults += @{Test="Health Check";Status="PASS";Details="OK"}
} else {
    Write-Host "  вњ— Health check failed: $($result.Error)" -ForegroundColor Red
    $failedTests++
    $testResults += @{Test="Health Check";Status="FAIL";Details=$result.Error}
}

# РўРµСЃС‚ 2: Health ready
Write-Host "`n[TEST 2] Health Check (Ready)" -ForegroundColor White
$result = Invoke-ApiRequest -Method "Get" -Path "/health/ready"
if ($result.Success) {
    Write-Host "  вњ“ Database ready: $($result.Data.database)" -ForegroundColor Green
    $passedTests++
    $testResults += @{Test="Health Ready";Status="PASS";Details="Database OK"}
} else {
    Write-Host "  вњ— Health ready failed: $($result.Error)" -ForegroundColor Red
    $failedTests++
    $testResults += @{Test="Health Ready";Status="FAIL";Details=$result.Error}
}

# РўРµСЃС‚ 3: РџРѕР»СѓС‡РёС‚СЊ СЃРїРёСЃРѕРє РєР°РјРµСЂ (РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ РїСѓСЃС‚С‹Рј)
Write-Host "`n[TEST 3] Get Cameras List" -ForegroundColor White
$result = Invoke-ApiRequest -Method "Get" -Path "/cameras"
if ($result.Success) {
    Write-Host "  вњ“ Cameras list retrieved: $($result.Data.Count) cameras" -ForegroundColor Green
    $passedTests++
    $testResults += @{Test="Get Cameras";Status="PASS";Details="$($result.Data.Count) cameras"}
} else {
    Write-Host "  вњ— Get cameras failed: $($result.Error)" -ForegroundColor Red
    $failedTests++
    $testResults += @{Test="Get Cameras";Status="FAIL";Details=$result.Error}
}

# РўРµСЃС‚ 4: РЎРѕР·РґР°С‚СЊ С‚РµСЃС‚РѕРІСѓСЋ РєР°РјРµСЂСѓ
Write-Host "`n[TEST 4] Create Test Camera" -ForegroundColor White
$testCamera = @{
    name = "SmokeTest_Camera_$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    url = "rtsp://admin:password@192.168.1.100:554/stream1"
    protocol = "RTSP"
    enabled = $true
}
$result = Invoke-ApiRequest -Method "Post" -Path "/cameras" -Body $testCamera
if ($result.Success) {
    $cameraId = $result.Data.id
    Write-Host "  вњ“ Camera created: ID=$cameraId" -ForegroundColor Green
    $passedTests++
    $testResults += @{Test="Create Camera";Status="PASS";Details="ID=$cameraId"}
} else {
    Write-Host "  вњ— Create camera failed: $($result.Error)" -ForegroundColor Red
    $failedTests++
    $testResults += @{Test="Create Camera";Status="FAIL";Details=$result.Error}
    $cameraId = $null
}

# РўРµСЃС‚ 5: РџРѕР»СѓС‡РёС‚СЊ СЃРѕР·РґР°РЅРЅСѓСЋ РєР°РјРµСЂСѓ
if ($cameraId) {
    Write-Host "`n[TEST 5] Get Created Camera" -ForegroundColor White
    $result = Invoke-ApiRequest -Method "Get" -Path "/cameras/$cameraId"
    if ($result.Success) {
        Write-Host "  вњ“ Camera retrieved: $($result.Data.name)" -ForegroundColor Green
        $passedTests++
        $testResults += @{Test="Get Camera";Status="PASS";Details=$result.Data.name}
    } else {
        Write-Host "  вњ— Get camera failed: $($result.Error)" -ForegroundColor Red
        $failedTests++
        $testResults += @{Test="Get Camera";Status="FAIL";Details=$result.Error}
    }
}

# РўРµСЃС‚ 6: РћР±РЅРѕРІРёС‚СЊ РєР°РјРµСЂСѓ
if ($cameraId) {
    Write-Host "`n[TEST 6] Update Camera" -ForegroundColor White
    $updateData = @{
        name = "SmokeTest_Camera_Updated"
        enabled = $false
    }
    $result = Invoke-ApiRequest -Method "Put" -Path "/cameras/$cameraId" -Body $updateData
    if ($result.Success) {
        Write-Host "  вњ“ Camera updated" -ForegroundColor Green
        $passedTests++
        $testResults += @{Test="Update Camera";Status="PASS";Details="OK"}
    } else {
        Write-Host "  вњ— Update camera failed: $($result.Error)" -ForegroundColor Red
        $failedTests++
        $testResults += @{Test="Update Camera";Status="FAIL";Details=$result.Error}
    }
}

# РўРµСЃС‚ 7: РЈРґР°Р»РёС‚СЊ РєР°РјРµСЂСѓ
if ($cameraId) {
    Write-Host "`n[TEST 7] Delete Camera" -ForegroundColor White
    $result = Invoke-ApiRequest -Method "Delete" -Path "/cameras/$cameraId"
    if ($result.Success) {
        Write-Host "  вњ“ Camera deleted" -ForegroundColor Green
        $passedTests++
        $testResults += @{Test="Delete Camera";Status="PASS";Details="OK"}
    } else {
        Write-Host "  вњ— Delete camera failed: $($result.Error)" -ForegroundColor Red
        $failedTests++
        $testResults += @{Test="Delete Camera";Status="FAIL";Details=$result.Error}
    }
}

# РџРѕР»РЅС‹Р№ smoke (РѕРїС†РёРѕРЅР°Р»СЊРЅРѕ)
if ($FullSmoke) {
    Write-Host "`n=== РџРћР›РќР«Р™ SMOKE ===" -ForegroundColor Cyan
    
    # РўРµСЃС‚ 8: Auth
    Write-Host "`n[TEST 8] Authentication" -ForegroundColor White
    $authBody = @{
        username = "admin"
        password = "$env:ADMIN_PASSWORD"
    }
    $result = Invoke-ApiRequest -Method "Post" -Path "/auth/login" -Body $authBody
    if ($result.Success) {
        Write-Host "  вњ“ Auth successful" -ForegroundColor Green
        $passedTests++
        $testResults += @{Test="Auth";Status="PASS";Details="OK"}
    } else {
        Write-Host "  вњ— Auth failed: $($result.Error)" -ForegroundColor Red
        $failedTests++
        $testResults += @{Test="Auth";Status="FAIL";Details=$result.Error}
    }
    
    # РўРµСЃС‚ 9: Events
    Write-Host "`n[TEST 9] Get Events" -ForegroundColor White
    $result = Invoke-ApiRequest -Method "Get" -Path "/events"
    if ($result.Success) {
        Write-Host "  вњ“ Events retrieved: $($result.Data.Count) events" -ForegroundColor Green
        $passedTests++
        $testResults += @{Test="Get Events";Status="PASS";Details="$($result.Data.Count) events"}
    } else {
        Write-Host "  вњ— Get events failed: $($result.Error)" -ForegroundColor Red
        $failedTests++
        $testResults += @{Test="Get Events";Status="FAIL";Details=$result.Error}
    }
}

# РС‚РѕРіРѕРІС‹Р№ РѕС‚С‡С‘С‚
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "РРўРћР“Р SMOKE РўР•РЎРўРћР’" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "РџСЂРѕР№РґРµРЅРѕ: $passedTests" -ForegroundColor Green
Write-Host "РџСЂРѕРІР°Р»РµРЅРѕ: $failedTests" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Red" })

# РЎРѕС…СЂР°РЅРµРЅРёРµ СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ
$reportPath = "build/reports/migration_smoke_$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
New-Item -ItemType Directory -Force -Path (Split-Path $reportPath) | Out-Null
$report = @{
    timestamp = (Get-Date -Format "o")
    baseUrl = $BaseUrl
    fullSmoke = $FullSmoke
    summary = @{
        passed = $passedTests
        failed = $failedTests
        total = ($passedTests + $failedTests)
    }
    tests = $testResults
}
$report | ConvertTo-Json -Depth 10 | Out-File $reportPath -Encoding utf8

Write-Host "`nРћС‚С‡С‘С‚ СЃРѕС…СЂР°РЅС‘РЅ: $reportPath" -ForegroundColor Yellow

# Р’С‹С…РѕРґРЅРѕР№ РєРѕРґ
if ($failedTests -gt 0) {
    exit 1
} else {
    exit 0
}
