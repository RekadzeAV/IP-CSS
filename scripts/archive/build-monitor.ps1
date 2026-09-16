# РњРѕРЅРёС‚РѕСЂРёРЅРі СЃР±РѕСЂРєРё РїСЂРѕРµРєС‚Р°
# РђРІС‚РѕРјР°С‚РёС‡РµСЃРєР°СЏ РїСЂРѕРІРµСЂРєР° Рё РѕР±СЂР°Р±РѕС‚РєР° СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ

param(
    [int]$CheckIntervalSeconds = 60,
    [int]$MaxChecks = 30,
    [string]$LogFile = "docs/reports/build_monitor.log"
)

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $entry = "[$timestamp] [$Level] $Message"
    $entry | Out-File -FilePath $LogFile -Append -Encoding UTF8
    Write-Host $entry
}

Write-Log "========== Р—РђРџРЈРЎРљ РњРћРќРРўРћР РРќР“Рђ =========="
Write-Log "РРЅС‚РµСЂРІР°Р» РїСЂРѕРІРµСЂРєРё: $CheckIntervalSeconds СЃРµРє"
Write-Log "РњР°РєСЃРёРјСѓРј РїСЂРѕРІРµСЂРѕРє: $MaxChecks"

$checkCount = 0
$buildCompleted = $false

while ($checkCount -lt $MaxChecks -and -not $buildCompleted) {
    $checkCount++
    Write-Log "РџСЂРѕРІРµСЂРєР° #$checkCount/$MaxChecks"
    
    $javaProcesses = Get-Process java -ErrorAction SilentlyContinue
    $buildExists = Test-Path "build"
    
    if ($javaProcesses.Count -eq 0) {
        Write-Log "Java РїСЂРѕС†РµСЃСЃС‹ Р·Р°РІРµСЂС€РµРЅС‹" "SUCCESS"
        
        if ($buildExists) {
            Write-Log "вњ… РЎР±РѕСЂРєР° СѓСЃРїРµС€РЅРѕ Р·Р°РІРµСЂС€РµРЅР°!" "SUCCESS"
            
            # РџСЂРѕРІРµСЂРєР° СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ
            $buildDirs = Get-ChildItem "build" -Directory -ErrorAction SilentlyContinue
            Write-Log "РЎРѕР·РґР°РЅРЅС‹Рµ РґРёСЂРµРєС‚РѕСЂРёРё:"
            $buildDirs | ForEach-Object { Write-Log "  - $($_.Name)" }
            
            # РџСЂРѕРІРµСЂРєР° РЅР° РѕС€РёР±РєРё
            $logFiles = Get-ChildItem "build" -Recurse -Filter "*.log" -ErrorAction SilentlyContinue | Select-Object -First 5
            if ($logFiles.Count -gt 0) {
                Write-Log "РќР°Р№РґРµРЅС‹ Р»РѕРі-С„Р°Р№Р»С‹:"
                $logFiles | ForEach-Object { Write-Log "  - $($_.FullName)" }
            }
            
            $buildCompleted = $true
            break
        } else {
            Write-Log "вќЊ РЎР±РѕСЂРєР° РЅРµ СЃРѕР·РґР°Р»Р° РїР°РїРєСѓ build" "ERROR"
            Write-Log "РџСЂРѕРІРµСЂРєР° exit code..."
            exit $LASTEXITCODE
        }
    } else {
        $memTotal = ($javaProcesses | Measure-Object -Property WorkingSet -Sum).Sum
        $cpuTotal = ($javaProcesses | Measure-Object -Property CPU -Sum).Sum
        
        Write-Log "РЎР±РѕСЂРєР° Р·Р°РїСѓС‰РµРЅР°: $($javaProcesses.Count) РїСЂРѕС†РµСЃСЃРѕРІ Java" "INFO"
        Write-Log "  РџР°РјСЏС‚СЊ: $([math]::Round($memTotal/1MB, 2)) MB"
        Write-Log "  CPU: $([math]::Round($cpuTotal, 2)) СЃРµРє"
        
        if ($checkCount % 5 -eq 0) {
            Write-Log "Р’СЂРµРјСЏ РІС‹РїРѕР»РЅРµРЅРёСЏ: $($checkCount * $CheckIntervalSeconds) СЃРµРє" "WARNING"
        }
    }
    
    if ($checkCount -lt $MaxChecks) {
        Write-Log "РћР¶РёРґР°РЅРёРµ $CheckIntervalSeconds СЃРµРєСѓРЅРґ..." "INFO"
        Start-Sleep -Seconds $CheckIntervalSeconds
    }
}

if (-not $buildCompleted) {
    Write-Log "вќЊ РџСЂРµРІС‹С€РµРЅРѕ РјР°РєСЃРёРјР°Р»СЊРЅРѕРµ РІСЂРµРјСЏ РѕР¶РёРґР°РЅРёСЏ" "ERROR"
    Write-Log "РџСЂРµСЂС‹РІР°РЅРёРµ РјРѕРЅРёС‚РѕСЂРёРЅРіР°"
    exit 1
}

Write-Log "========== РњРћРќРРўРћР РРќР“ Р—РђР’Р•Р РЁР•Рќ ==========" "SUCCESS"
exit 0
