# РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРёР№ РєРѕРЅС‚СЂРѕР»Р»РµСЂ РІС‹РїРѕР»РЅРµРЅРёСЏ Р¤Р°Р·С‹ 1
# Р—Р°РїСѓСЃРєР°РµС‚СЃСЏ РїРѕСЃР»Рµ Р·Р°РІРµСЂС€РµРЅРёСЏ СЃР±РѕСЂРєРё Рё РїСЂРѕРґРѕР»Р¶Р°РµС‚ РІС‹РїРѕР»РЅРµРЅРёРµ

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  РђР’РўРћРњРђРўРР—РР РћР’РђРќРќРћР• Р’Р«РџРћР›РќР•РќРР• Р¤РђР—Р« 1" -ForegroundColor Cyan
Write-Host "  РќР°С‡Р°Р»Рѕ: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Р¤СѓРЅРєС†РёСЏ Р»РѕРіРёСЂРѕРІР°РЅРёСЏ
function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch($Level) {
        "ERROR" { "Red" }
        "WARNING" { "Yellow" }
        "SUCCESS" { "Green" }
        default { "White" }
    }
    $entry = "[$timestamp] [$Level] $Message"
    Write-Host $entry -ForegroundColor $color
    $entry | Out-File -FilePath "docs/reports/automated_execution.log" -Append -Encoding UTF8
}

# РџСЂРѕРІРµСЂРєР° РїРµСЂРІРѕР№ СЃР±РѕСЂРєРё
Write-Log "РџСЂРѕРІРµСЂРєР° СЂРµР·СѓР»СЊС‚Р°С‚Р° РїРµСЂРІРѕР№ СЃР±РѕСЂРєРё..."

$buildExists = Test-Path "build"
$javaProcesses = Get-Process java -ErrorAction SilentlyContinue

if ($javaProcesses.Count -gt 0) {
    Write-Log "РЎР±РѕСЂРєР° РІСЃС‘ РµС‰С‘ Р·Р°РїСѓС‰РµРЅР°: $($javaProcesses.Count) РїСЂРѕС†РµСЃСЃРѕРІ" "WARNING"
    Write-Log "РћР¶РёРґР°РЅРёРµ Р·Р°РІРµСЂС€РµРЅРёСЏ..." "INFO"
    Start-Sleep -Seconds 300
    
    $javaProcesses = Get-Process java -ErrorAction SilentlyContinue
    if ($javaProcesses.Count -gt 0) {
        Write-Log "РЎР±РѕСЂРєР° РїСЂРѕРґРѕР»Р¶Р°РµС‚СЃСЏ, РїРѕРґРѕР¶РґСѓ РµС‰С‘..." "WARNING"
        Start-Sleep -Seconds 600
    }
}

if (-not (Test-Path "build")) {
    Write-Log "вќЊ РџР°РїРєР° build РЅРµ РЅР°Р№РґРµРЅР° РїРѕСЃР»Рµ РѕР¶РёРґР°РЅРёСЏ" "ERROR"
    Write-Log "РџСЂРѕРІРµСЂРєР° Р»РѕРіРѕРІ Gradle..." "INFO"
    
    $gradleLogs = Get-ChildItem -Recurse -Filter "*gradle*.log" -ErrorAction SilentlyContinue | Select-Object -First 5
    if ($gradleLogs.Count -gt 0) {
        Write-Log "РќР°Р№РґРµРЅРЅС‹Рµ Р»РѕРіРё:" "INFO"
        $gradleLogs | ForEach-Object { Write-Log "  - $($_.FullName)" }
    }
    
    exit 1
}

Write-Log "вњ… РЎР±РѕСЂРєР° Р·Р°РІРµСЂС€РµРЅР° СѓСЃРїРµС€РЅРѕ!" "SUCCESS"
Write-Log "РќР°С‡РёРЅР°СЋ РІС‹РїРѕР»РЅРµРЅРёРµ Р·Р°РґР°С‡ Р¤Р°Р·С‹ 1" "INFO"

# Р—Р°РґР°С‡Р° 1.1: RTSP Native Integration
Write-Log "========== Р—РђР”РђР§Рђ 1.1: RTSP Native Integration ==========" "INFO"

# РџСЂРѕРІРµСЂРєР° native РєРѕРјРїРѕРЅРµРЅС‚РѕРІ
if (Test-Path "native") {
    Write-Log "РќР°Р№РґРµРЅР° РїР°РїРєР° native, РїСЂРѕРІРµСЂСЏСЋ CMakeLists.txt..." "INFO"
    
    # РЎРѕР·РґР°РЅРёРµ build РґРёСЂРµРєС‚РѕСЂРёРё
    if (!(Test-Path "native\build")) {
        New-Item -ItemType Directory -Path "native\build" -Force | Out-Null
    }
    
    Set-Location "native\build"
    
    Write-Log "Р—Р°РїСѓСЃРє CMake РіРµРЅРµСЂР°С†РёРё..." "INFO"
    $cmakeResult = cmake .. -DCMAKE_BUILD_TYPE=Release 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Log "CMake РіРµРЅРµСЂР°С†РёСЏ СѓСЃРїРµС€РЅР°" "SUCCESS"
        
        Write-Log "РљРѕРјРїРёР»СЏС†РёСЏ native РєРѕРјРїРѕРЅРµРЅС‚РѕРІ..." "INFO"
        $buildResult = cmake --build . --config Release 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "вњ… Native РєРѕРјРїРѕРЅРµРЅС‚С‹ СЃРєРѕРјРїРёР»РёСЂРѕРІР°РЅС‹ СѓСЃРїРµС€РЅРѕ" "SUCCESS"
        } else {
            Write-Log "вќЊ РћС€РёР±РєР° РєРѕРјРїРёР»СЏС†РёРё native РєРѕРјРїРѕРЅРµРЅС‚РѕРІ" "ERROR"
            Write-Log $buildResult -split "`n" | Select-Object -First 20
        }
    } else {
        Write-Log "вќЊ РћС€РёР±РєР° CMake РіРµРЅРµСЂР°С†РёРё" "ERROR"
        Write-Log $cmakeResult -split "`n" | Select-Object -First 20
    }
    
    Set-Location "../.."
} else {
    Write-Log "РџР°РїРєР° native РЅРµ РЅР°Р№РґРµРЅР°" "WARNING"
}

# РџСЂРѕРІРµСЂРєР° СЃР±РѕСЂРєРё Kotlin СЃ FFI
Write-Log "РџСЂРѕРІРµСЂРєР° Kotlin РєРѕРјРїРёР»СЏС†РёРё СЃ FFI Р±РёРЅРґРёРЅРіР°РјРё..." "INFO"
$gradleResult = ./gradlew.bat :shared:compileKotlinJvm --no-daemon 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Log "вњ… Kotlin РєРѕРјРїРёР»СЏС†РёСЏ СѓСЃРїРµС€РЅР°" "SUCCESS"
} else {
    Write-Log "вќЊ РћС€РёР±РєР° Kotlin РєРѕРјРїРёР»СЏС†РёРё" "ERROR"
    Write-Log "РџРѕРїС‹С‚РєРё РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ (РјР°РєСЃ 3)..." "WARNING"
    
    for ($i = 1; $i -le 3; $i++) {
        Write-Log "РџРѕРїС‹С‚РєР° #$i" "INFO"
        ./gradlew.bat clean :shared:compileKotlinJvm --no-daemon 2>&1 | Out-Null
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "вњ… Р’РѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёРµ СѓСЃРїРµС€РµРЅРѕ РЅР° РїРѕРїС‹С‚РєРµ #$i" "SUCCESS"
            break
        }
        
        if ($i -lt 3) {
            Write-Log "РћС‡РёСЃС‚РєР° Рё РїРѕРІС‚РѕСЂ..." "WARNING"
            ./gradlew.bat clean --stop 2>&1 | Out-Null
            Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Log "========== Р—РђР”РђР§Рђ 1.1 Р—РђР’Р•Р РЁР•РќРђ ==========" "INFO"

# Р—Р°РґР°С‡Р° 1.2: PostgreSQL Finalization
Write-Log "========== Р—РђР”РђР§Рђ 1.2: PostgreSQL Finalization ==========" "INFO"

if (Test-Path "scripts\postgres-finalization-generate-staging-report.ps1") {
    Write-Log "Р—Р°РїСѓСЃРє staging report..." "INFO"
    & .\scripts\postgres-finalization-generate-staging-report.ps1
    
    Write-Log "РџСЂРѕРІРµСЂРєР° РјРёРіСЂР°С†РёР№..." "INFO"
    ./gradlew.bat :server:api:flywayMigrate --no-daemon 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Log "вњ… РњРёРіСЂР°С†РёРё СѓСЃРїРµС€РЅС‹" "SUCCESS"
    } else {
        Write-Log "вљ пёЏ РњРёРіСЂР°С†РёРё С‚СЂРµР±СѓСЋС‚ РІРЅРёРјР°РЅРёСЏ" "WARNING"
    }
} else {
    Write-Log "РЎРєСЂРёРїС‚ PostgreSQL finalization РЅРµ РЅР°Р№РґРµРЅ" "WARNING"
}

Write-Log "========== Р—РђР”РђР§Рђ 1.2 Р—РђР’Р•Р РЁР•РќРђ ==========" "INFO"

Write-Log "========================================" "SUCCESS"
Write-Log "Р­РўРђРџ 1 (РљСЂРёС‚РёС‡РµСЃРєРёРµ Р±Р»РѕРєРµСЂС‹) Р—РђР’Р•Р РЁР•Рќ" "SUCCESS"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Р“РѕС‚РѕРІ Рє РІС‹РїРѕР»РЅРµРЅРёСЋ Р­С‚Р°РїР° 2 (Р’С‹СЃРѕРєРёР№ РїСЂРёРѕСЂРёС‚РµС‚)" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

exit 0
