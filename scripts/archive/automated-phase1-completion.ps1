# РђРІС‚РѕРјР°С‚РёР·РёСЂРѕРІР°РЅРЅРѕРµ РІС‹РїРѕР»РЅРµРЅРёРµ Р¤Р°Р·С‹ 1 MVP
# IP-CSS Project
# Р’РµСЂСЃРёСЏ: 1.0
# Р”Р°С‚Р°: 2026-04-28

param(
    [string]$TaskId = "all",
    [int]$MaxBuildAttempts = 3,
    [switch]$SkipTests,
    [switch]$Verbose
)

$ErrorActionPreference = "Continue"
$buildAttempts = 0
$lastBuildOutput = ""
$unchangedCount = 0

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
    Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor $color
}

# Р¤СѓРЅРєС†РёСЏ РїСЂРѕРІРµСЂРєРё СЃР±РѕСЂРєРё
function Test-Build {
    param([string]$BuildCommand)
    
    $buildAttempts++
    Write-Log "РџРѕРїС‹С‚РєР° СЃР±РѕСЂРєРё #$buildAttempts" "INFO"
    
    $startTime = Get-Date
    $result = Invoke-Expression "$BuildCommand 2>&1"
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    $lastBuildOutput = $result -join "`n"
    
    $exitCode = $LASTEXITCODE
    
    Write-Log "Р’СЂРµРјСЏ СЃР±РѕСЂРєРё: $($duration.TotalMinutes.ToString('0.0')) РјРёРЅСѓС‚" "INFO"
    
    if ($exitCode -eq 0) {
        Write-Log "РЎР±РѕСЂРєР° СѓСЃРїРµС€РЅР°!" "SUCCESS"
        $unchangedCount = 0
        return $true
    } else {
        Write-Log "РЎР±РѕСЂРєР° РЅРµ СѓРґР°Р»Р°СЃСЊ. Р’С‹РІРѕРґ:" "ERROR"
        Write-Log $lastBuildOutput -split "`n" | Select-Object -First 20
        return $false
    }
}

# Р¤СѓРЅРєС†РёСЏ РїСЂРѕРІРµСЂРєРё РёР·РјРµРЅРµРЅРёР№
function Test-ChangesDetected {
    param([string]$PreviousOutput, [string]$CurrentOutput)
    
    if ($PreviousOutput -eq $CurrentOutput) {
        $unchangedCount++
        Write-Log "РЎР±РѕСЂРєР° РЅРµ РёР·РјРµРЅРёР»Р°СЃСЊ ($unchangedCount/3)" "WARNING"
        return $false
    } else {
        $unchangedCount = 0
        return $true
    }
}

# Р¤СѓРЅРєС†РёСЏ РѕР±РЅР°СЂСѓР¶РµРЅРёСЏ РїСЂРёС‡РёРЅ РЅРµСѓРґР°С‡Рё
function Find-BuildIssue {
    param([string]$BuildOutput)
    
    Write-Log "РђРЅР°Р»РёР· РїСЂРёС‡РёРЅ РЅРµСѓРґР°С‡Рё СЃР±РѕСЂРєРё..." "INFO"
    
    $issues = @()
    
    # РџСЂРѕРІРµСЂРєР° РЅР° РѕС€РёР±РєРё РєРѕРјРїРёР»СЏС†РёРё Kotlin
    if ($BuildOutput -match "e:\s*(.*)") {
        $issues += "РћС€РёР±РєР° РєРѕРјРїРёР»СЏС†РёРё Kotlin: $($matches[1])"
    }
    
    # РџСЂРѕРІРµСЂРєР° РЅР° РѕС€РёР±РєРё C++ РєРѕРјРїРёР»СЏС†РёРё
    if ($BuildOutput -match "error C\d+:\s*(.*)") {
        $issues += "РћС€РёР±РєР° РєРѕРјРїРёР»СЏС†РёРё C++: $($matches[1])"
        Write-Log "РўСЂРµР±СѓРµС‚СЃСЏ Visual Studio Build Tools" "WARNING"
    }
    
    # РџСЂРѕРІРµСЂРєР° РЅР° РѕС€РёР±РєРё FFmpeg
    if ($BuildOutput -match "ffmpeg|FFmpeg|libav") {
        if ($BuildOutput -match "not found|cannot find|missing") {
            $issues += "FFmpeg РЅРµ РЅР°Р№РґРµРЅ РёР»Рё РЅРµ РЅР°СЃС‚СЂРѕРµРЅ"
        }
    }
    
    # РџСЂРѕРІРµСЂРєР° РЅР° РѕС€РёР±РєРё Gradle
    if ($BuildOutput -match "FAILURE|Exception|Could not") {
        $issues += "РћС€РёР±РєР° Gradle: РѕР±РЅР°СЂСѓР¶РµРЅР° РёСЃРєР»СЋС‡РёС‚РµР»СЊРЅР°СЏ СЃРёС‚СѓР°С†РёСЏ"
    }
    
    # РџСЂРѕРІРµСЂРєР° РЅР° РѕС‚СЃСѓС‚СЃС‚РІРёРµ Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№
    if ($BuildOutput -match "dependency|dependency resolution|Could not resolve") {
        $issues += "РџСЂРѕР±Р»РµРјР° СЃ Р·Р°РІРёСЃРёРјРѕСЃС‚СЏРјРё"
    }
    
    # РџСЂРѕРІРµСЂРєР° РЅР° РѕС€РёР±РєРё РЅР°С‚РёРІРЅРѕР№ РєРѕРјРїРёР»СЏС†РёРё
    if ($BuildOutput -match "CMake|ninja|make") {
        if ($BuildOutput -match "error|failed|not found") {
            $issues += "РћС€РёР±РєР° СЃР±РѕСЂРєРё РЅР°С‚РёРІРЅС‹С… РєРѕРјРїРѕРЅРµРЅС‚РѕРІ"
        }
    }
    
    return $issues
}

# Р¤СѓРЅРєС†РёСЏ СѓСЃС‚СЂР°РЅРµРЅРёСЏ РїСЂРѕР±Р»РµРј
function Resolve-BuildIssue {
    param([array]$Issues)
    
    Write-Log "РЈСЃС‚СЂР°РЅРµРЅРёРµ РїСЂРѕР±Р»РµРј СЃР±РѕСЂРєРё..." "INFO"
    
    foreach ($issue in $Issues) {
        Write-Log "РџСЂРѕР±Р»РµРјР°: $issue" "WARNING"
        
        if ($issue -match "FFmpeg") {
            Write-Log "РџСЂРѕРІРµСЂРєР° СѓСЃС‚Р°РЅРѕРІРєРё FFmpeg..." "INFO"
            if (!(Get-Command "ffmpeg" -ErrorAction SilentlyContinue)) {
                Write-Log "FFmpeg РЅРµ РЅР°Р№РґРµРЅ. РЈСЃС‚Р°РЅРѕРІРєР° Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№..." "WARNING"
                & .\scripts\install-build-dependencies.ps1
            }
        }
        
        if ($issue -match "Visual Studio|C\+\+") {
            Write-Log "РџСЂРѕРІРµСЂРєР° Visual Studio Build Tools..." "INFO"
            $vswhere = "C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe"
            if (Test-Path $vswhere) {
                $vsInstall = & $vswhere -latest -property installationPath
                if ($vsInstall) {
                    Write-Log "Visual Studio РЅР°Р№РґРµРЅР°: $vsInstall" "SUCCESS"
                }
            }
        }
        
        if ($issue -match "dependencies|Gradle") {
            Write-Log "РћС‡РёСЃС‚РєР° РєСЌС€Р° Gradle..." "INFO"
            & .\gradlew.bat clean --stop
            Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
        }
        
        if ($issue -match "CMake|native") {
            Write-Log "РџРµСЂРµСЃР±РѕСЂРєР° РЅР°С‚РёРІРЅС‹С… РєРѕРјРїРѕРЅРµРЅС‚РѕРІ..." "INFO"
            if (Test-Path "native") {
                Set-Location "native"
                if (Test-Path "build") { Remove-Item "build" -Recurse -Force }
                & cmake -B build -S .
                & cmake --build build --config Release
                Set-Location ..
            }
        }
    }
}

# Р—Р°РґР°С‡Р° 1: RTSP Native Integration
function Invoke-Task-RTSPIntegration {
    Write-Log "=== Р—Р°РґР°С‡Р° 1: RTSP Native Integration ===" "INFO"
    
    # РЁР°Рі 1: РџСЂРѕРІРµСЂРєР° Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№
    Write-Log "РџСЂРѕРІРµСЂРєР° FFmpeg Рё CMake..." "INFO"
    if (!(Get-Command "cmake" -ErrorAction SilentlyContinue)) {
        Write-Log "CMake РЅРµ РЅР°Р№РґРµРЅ. РЈСЃС‚Р°РЅРѕРІРєР°..." "WARNING"
        & .\scripts\install-build-dependencies.ps1
    }
    
    # РЁР°Рі 2: РџСЂРѕРІРµСЂРєР° Visual Studio РґР»СЏ РЅР°С‚РёРІРЅРѕР№ РєРѕРјРїРёР»СЏС†РёРё
    $vswhere = "C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe"
    if (Test-Path $vswhere) {
        $vsInfo = & $vswhere -latest -format json | ConvertFrom-Json
        Write-Log "Visual Studio РЅР°Р№РґРµРЅР°: $($vsInfo.displayName)" "SUCCESS"
        Write-Log "РџСѓС‚СЊ: $($vsInfo.installationPath)" "INFO"
    } else {
        Write-Log "Visual Studio РЅРµ РЅР°Р№РґРµРЅР°! РўСЂРµР±СѓРµС‚СЃСЏ РґР»СЏ РЅР°С‚РёРІРЅРѕР№ РєРѕРјРїРёР»СЏС†РёРё." "ERROR"
    }
    
    # РЁР°Рі 3: РЎР±РѕСЂРєР° РЅР°С‚РёРІРЅС‹С… РєРѕРјРїРѕРЅРµРЅС‚РѕРІ
    Write-Log "РЎР±РѕСЂРєР° native РєРѕРјРїРѕРЅРµРЅС‚РѕРІ..." "INFO"
    if (Test-Path "native") {
        Set-Location "native"
        if (!(Test-Path "build")) {
            & cmake -B build -S . -DCMAKE_BUILD_TYPE=Release
        }
        & cmake --build build --config Release
        Set-Location ..
    }
    
    # РЁР°Рі 4: РџСЂРѕРІРµСЂРєР° FFI Р±РёРЅРґРёРЅРіРѕРІ
    Write-Log "РџСЂРѕРІРµСЂРєР° Kotlin cinterop Р±РёРЅРґРёРЅРіРѕРІ..." "INFO"
    
    # РЁР°Рі 5: РЎР±РѕСЂРєР° РїСЂРѕРµРєС‚Р°
    $buildSuccess = Test-Build "./gradlew.bat :shared:compileKotlinJvm"
    
    return $buildSuccess
}

# Р—Р°РґР°С‡Р° 2: PostgreSQL Finalization
function Invoke-Task-PostgreSQLFinalization {
    Write-Log "=== Р—Р°РґР°С‡Р° 2: PostgreSQL Finalization ===" "INFO"
    
    # Р—Р°РїСѓСЃРє СЃРєСЂРёРїС‚Р° С„РёРЅР°Р»РёР·Р°С†РёРё
    if (Test-Path "scripts\postgres-finalization-generate-staging-report.ps1") {
        & .\scripts\postgres-finalization-generate-staging-report.ps1
    }
    
    # РџСЂРѕРІРµСЂРєР° РјРёРіСЂР°С†РёР№
    Write-Log "РџСЂРѕРІРµСЂРєР° РјРёРіСЂР°С†РёР№ Р±Р°Р·С‹ РґР°РЅРЅС‹С…..." "INFO"
    $buildSuccess = Test-Build "./gradlew.bat :server:api:flywayMigrate"
    
    return $buildSuccess
}

# Р—Р°РґР°С‡Р° 3: HLS Runtime Stability
function Invoke-Task-HLSStability {
    Write-Log "=== Р—Р°РґР°С‡Р° 3: HLS Runtime Stability ===" "INFO"
    
    # Р—Р°РїСѓСЃРє long-run С‚РµСЃС‚РѕРІ
    if (Test-Path "scripts\long-run-test.ps1") {
        & .\scripts\long-run-test.ps1 -DurationSeconds 1800
    }
    
    return $true
}

# Р—Р°РґР°С‡Р° 4: Security MVP
function Invoke-Task-SecurityMVP {
    Write-Log "=== Р—Р°РґР°С‡Р° 4: Security MVP Evidence ===" "INFO"
    
    # РЎР±РѕСЂРєР° РїСЂРѕРµРєС‚Р° СЃ РїСЂРѕРІРµСЂРєРѕР№ Р±РµР·РѕРїР°СЃРЅРѕСЃС‚Рё
    $buildSuccess = Test-Build "./gradlew.bat build -x test"
    
    return $buildSuccess
}

# Р—Р°РґР°С‡Р° 5: Android Video Integration
function Invoke-Task-AndroidVideo {
    Write-Log "=== Р—Р°РґР°С‡Р° 5: Android Video Integration ===" "INFO"
    
    # РЎР±РѕСЂРєР° Android РјРѕРґСѓР»СЏ
    $buildSuccess = Test-Build "./gradlew.bat :platforms:client-android:assembleDebug"
    
    return $buildSuccess
}

# Р—Р°РґР°С‡Р° 6: Desktop UI Completion
function Invoke-Task-DesktopUI {
    Write-Log "=== Р—Р°РґР°С‡Р° 6: Desktop UI Completion ===" "INFO"
    
    # РЎР±РѕСЂРєР° Desktop РјРѕРґСѓР»СЏ
    $buildSuccess = Test-Build "./gradlew.bat :platforms:client-desktop:assemble"
    
    return $buildSuccess
}

# Р—Р°РґР°С‡Р° 7: Testing
function Invoke-Task-Testing {
    Write-Log "=== Р—Р°РґР°С‡Р° 7: РўРµСЃС‚РёСЂРѕРІР°РЅРёРµ ===" "INFO"
    
    # Р—Р°РїСѓСЃРє unit С‚РµСЃС‚РѕРІ
    $buildSuccess = Test-Build "./gradlew.bat test"
    
    return $buildSuccess
}

# РћСЃРЅРѕРІРЅР°СЏ С„СѓРЅРєС†РёСЏ РІС‹РїРѕР»РЅРµРЅРёСЏ
function Invoke-Phase1Completion {
    Write-Log "==========================================" "INFO"
    Write-Log "РќРђР§РђР›Рћ РђР’РўРћРњРђРўРР—РР РћР’РђРќРќРћР“Рћ Р’Р«РџРћР›РќР•РќРРЇ Р¤РђР—Р« 1" "SUCCESS"
    Write-Log "==========================================" "INFO"
    
    $tasks = @{
        "rtsp" = "Invoke-Task-RTSPIntegration"
        "postgresql" = "Invoke-Task-PostgreSQLFinalization"
        "hls" = "Invoke-Task-HLSStability"
        "security" = "Invoke-Task-SecurityMVP"
        "android" = "Invoke-Task-AndroidVideo"
        "desktop" = "Invoke-Task-DesktopUI"
        "testing" = "Invoke-Task-Testing"
        "all" = @("Invoke-Task-RTSPIntegration", "Invoke-Task-PostgreSQLFinalization", "Invoke-Task-HLSStability", "Invoke-Task-SecurityMVP", "Invoke-Task-AndroidVideo", "Invoke-Task-DesktopUI", "Invoke-Task-Testing")
    }
    
    $selectedTasks = if ($TaskId -eq "all") { $tasks["all"] } else { @($tasks[$TaskId]) }
    
    if ($selectedTasks.Count -eq 0) {
        Write-Log "РќРµ РЅР°Р№РґРµРЅРѕ Р·Р°РґР°С‡ РґР»СЏ ID: $TaskId" "ERROR"
        exit 1
    }
    
    foreach ($task in $selectedTasks) {
        Write-Log "Р’С‹РїРѕР»РЅРµРЅРёРµ Р·Р°РґР°С‡Рё: $task" "INFO"
        
        $success = Invoke-Command -ScriptBlock ([scriptblock]::Create($task))
        
        if (-not $success) {
            Write-Log "Р—Р°РґР°С‡Р° $task РЅРµ РІС‹РїРѕР»РЅРµРЅР°. РђРЅР°Р»РёР· РїСЂРѕР±Р»РµРј..." "ERROR"
            
            # РџРѕРїС‹С‚РєРё РїРµСЂРµСЃР±РѕСЂРєРё
            for ($i = 1; $i -le $MaxBuildAttempts; $i++) {
                Write-Log "РџРѕРїС‹С‚РєР° РїРµСЂРµСЃР±РѕСЂРєРё #$i/$MaxBuildAttempts" "WARNING"
                
                $buildSuccess = Test-Build "./gradlew.bat clean build --no-daemon"
                
                if ($buildSuccess) {
                    Write-Log "РЎР±РѕСЂРєР° СѓСЃРїРµС€РЅР° РїРѕСЃР»Рµ $i РїРѕРїС‹С‚РѕРє!" "SUCCESS"
                    break
                }
                
                if ($i -lt $MaxBuildAttempts) {
                    # РђРЅР°Р»РёР· РїСЂРѕР±Р»РµРј
                    $issues = Find-BuildIssue $lastBuildOutput
                    if ($issues.Count -gt 0) {
                        Resolve-BuildIssue $issues
                    }
                    
                    # РџСЂРѕРІРµСЂРєР° РёР·РјРµРЅРµРЅРёР№
                    if (-not (Test-ChangesDetected $lastBuildOutput $lastBuildOutput)) {
                        if ($unchangedCount -ge 3) {
                            Write-Log "РћР±РЅР°СЂСѓР¶РµРЅРѕ 3 РѕРґРёРЅР°РєРѕРІС‹Рµ СЃР±РѕСЂРєРё РїРѕРґСЂСЏРґ. РћСЃС‚Р°РЅРѕРІРєР°." "ERROR"
                            Write-Log "РџРѕСЃР»РµРґРЅРёР№ РІС‹РІРѕРґ СЃР±РѕСЂРєРё:" "ERROR"
                            Write-Log $lastBuildOutput -split "`n" | Select-Object -First 50
                            exit 1
                        }
                    }
                }
            }
            
            if (-not $buildSuccess) {
                Write-Log "РџСЂРµРІС‹С€РµРЅРѕ РјР°РєСЃРёРјР°Р»СЊРЅРѕРµ РєРѕР»РёС‡РµСЃС‚РІРѕ РїРѕРїС‹С‚РѕРє СЃР±РѕСЂРєРё РґР»СЏ Р·Р°РґР°С‡Рё $task" "ERROR"
                exit 1
            }
        }
    }
    
    Write-Log "==========================================" "SUCCESS"
    Write-Log "Р¤РђР—Рђ 1 Р—РђР’Р•Р РЁР•РќРђ РЈРЎРџР•РЁРќРћ!" "SUCCESS"
    Write-Log "==========================================" "SUCCESS"
}

# Р—Р°РїСѓСЃРє
Invoke-Phase1Completion
