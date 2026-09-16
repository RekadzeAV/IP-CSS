# Р—Р°РїСѓСЃРє MVP Automated Acceptance СЃ РїСЂРѕРІРµСЂРєРѕР№ РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹
# РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё Р·Р°РїСѓСЃРєР°РµС‚ docker-compose РµСЃР»Рё РєРѕРЅС‚РµР№РЅРµСЂС‹ РЅРµ Р·Р°РїСѓС‰РµРЅС‹

param(
    [switch]$SkipVideoGate,
    [switch]$GeneratePhase1Summary,
    [ValidateSet("Strict", "MvpCi")]
    [string]$Phase1SummaryProfile = "MvpCi",
    [switch]$SkipInfrastructureCheck,
    [switch]$ShowHelp
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
$dockerComposeFile = Join-Path $ProjectRoot "docker-compose.yml"

if ($ShowHelp) {
    Write-Host "MVP Automated Acceptance СЃ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРѕР№ РїСЂРѕРІРµСЂРєРѕР№ РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\run-mvp-acceptance-with-infra.ps1"
    Write-Host "  .\scripts\run-mvp-acceptance-with-infra.ps1 -SkipVideoGate"
    Write-Host "  .\scripts\run-mvp-acceptance-with-infra.ps1 -GeneratePhase1Summary"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipVideoGate           РџСЂРѕРїСѓСЃС‚РёС‚СЊ video e2e gate"
    Write-Host "  -GeneratePhase1Summary   РЎРіРµРЅРµСЂРёСЂРѕРІР°С‚СЊ Phase 1 summary"
    Write-Host "  -Phase1SummaryProfile    Strict|MvpCi (default: MvpCi)"
    Write-Host "  -SkipInfrastructureCheck РџСЂРѕРїСѓСЃС‚РёС‚СЊ РїСЂРѕРІРµСЂРєСѓ РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹"
    Write-Host ""
    exit 0
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  MVP Automated Acceptance СЃ РїСЂРѕРІРµСЂРєРѕР№ РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# РџСЂРѕРІРµСЂРєР° РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹
if (-not $SkipInfrastructureCheck) {
    Write-Host "РЁР°Рі 1: РџСЂРѕРІРµСЂРєР° РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹" -ForegroundColor Cyan
    Write-Host ""
    
    # РџСЂРѕРІРµСЂРєР° Docker
    try {
        $dockerVersion = docker --version 2>&1
        Write-Host "  вњ“ Docker СѓСЃС‚Р°РЅРѕРІР»РµРЅ: $dockerVersion" -ForegroundColor Green
    } catch {
        Write-Host "вќЊ Docker РЅРµ СѓСЃС‚Р°РЅРѕРІР»РµРЅ" -ForegroundColor Red
        Write-Host "   РЈСЃС‚Р°РЅРѕРІРёС‚Рµ Docker Desktop Рё РїРѕРїСЂРѕР±СѓР№С‚Рµ СЃРЅРѕРІР°" -ForegroundColor Yellow
        exit 1
    }
    
    # РџСЂРѕРІРµСЂРєР° docker-compose.yml
    if (-not (Test-Path $dockerComposeFile)) {
        Write-Host "вќЊ docker-compose.yml РЅРµ РЅР°Р№РґРµРЅ" -ForegroundColor Red
        exit 1
    }
    
    # РџСЂРѕРІРµСЂРєР° Р·Р°РїСѓС‰РµРЅРЅС‹С… РєРѕРЅС‚РµР№РЅРµСЂРѕРІ
    Write-Host "  РџСЂРѕРІРµСЂРєР° Р·Р°РїСѓС‰РµРЅРЅС‹С… РєРѕРЅС‚РµР№РЅРµСЂРѕРІ..." -ForegroundColor DarkGray
    $runningContainers = docker ps --format "{{.Names}}" 2>&1
    $surveillanceRunning = $runningContainers -contains "ip-camera-surveillance"
    $postgresRunning = $runningContainers -contains "surveillance-postgres"
    $redisRunning = $runningContainers -contains "surveillance-redis"
    
    $allRunning = $surveillanceRunning -and $postgresRunning -and $redisRunning
    
    if ($allRunning) {
        Write-Host "  вњ“ Р’СЃРµ РєРѕРЅС‚РµР№РЅРµСЂС‹ Р·Р°РїСѓС‰РµРЅС‹" -ForegroundColor Green
    } else {
        Write-Host "  вљ  РќРµРєРѕС‚РѕСЂС‹Рµ РєРѕРЅС‚РµР№РЅРµСЂС‹ РЅРµ Р·Р°РїСѓС‰РµРЅС‹" -ForegroundColor Yellow
        
        # РџСЂРѕРІРµСЂРєР° .env С„Р°Р№Р»Р°
        $envFile = Join-Path $ProjectRoot ".env"
        if (-not (Test-Path $envFile)) {
            Write-Host "    вќЊ .env С„Р°Р№Р» РЅРµ РЅР°Р№РґРµРЅ" -ForegroundColor Red
            Write-Host "    РЎРєРѕРїРёСЂСѓР№С‚Рµ .env.example РІ .env Рё РЅР°СЃС‚СЂРѕР№С‚Рµ РїРµСЂРµРјРµРЅРЅС‹Рµ" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "    РџСЂРёРјРµСЂ:" -ForegroundColor DarkGray
            Write-Host "      Copy-Item .env.example .env" -ForegroundColor White
            Write-Host "      # РћС‚СЂРµРґР°РєС‚РёСЂСѓР№С‚Рµ .env Рё РЅР°СЃС‚СЂРѕР№С‚Рµ РїРµСЂРµРјРµРЅРЅС‹Рµ" -ForegroundColor White
            exit 1
        }
        
        # Р—Р°РїСЂРѕСЃ РїРѕРґС‚РІРµСЂР¶РґРµРЅРёСЏ РЅР° Р·Р°РїСѓСЃРє
        Write-Host ""
        Write-Host "  Р—Р°РїСѓСЃС‚РёС‚СЊ РєРѕРЅС‚РµР№РЅРµСЂС‹? (y/n)" -ForegroundColor Cyan
        $confirm = Read-Host
        
        if ($confirm -eq "y" -or $confirm -eq "Y") {
            Write-Host ""
            Write-Host "  Р—Р°РїСѓСЃРє РєРѕРЅС‚РµР№РЅРµСЂРѕРІ С‡РµСЂРµР· docker compose up -d..." -ForegroundColor DarkGray
            
            Push-Location $ProjectRoot
            try {
                docker compose up -d
                if ($LASTEXITCODE -ne 0) {
                    Write-Host "  вќЊ РћС€РёР±РєР° РїСЂРё Р·Р°РїСѓСЃРєРµ РєРѕРЅС‚РµР№РЅРµСЂРѕРІ" -ForegroundColor Red
                    Write-Host "  РџСЂРѕРІРµСЂСЊС‚Рµ Р»РѕРіРё: docker compose logs" -ForegroundColor Yellow
                    exit 1
                }
                
                Write-Host "  вњ“ РљРѕРЅС‚РµР№РЅРµСЂС‹ Р·Р°РїСѓС‰РµРЅС‹" -ForegroundColor Green
                Write-Host "  РћР¶РёРґР°РЅРёРµ Р·Р°РїСѓСЃРєР° СЃРµСЂРІРёСЃРѕРІ (30 СЃРµРєСѓРЅРґ)..." -ForegroundColor DarkGray
                Start-Sleep -Seconds 30
            } finally {
                Pop-Location
            }
        } else {
            Write-Host ""
            Write-Host "вќЊ РљРѕРЅС‚РµР№РЅРµСЂС‹ РЅРµ Р·Р°РїСѓС‰РµРЅС‹. Р—Р°РїСѓСЃРє:" -ForegroundColor Red
            Write-Host "   cd <project-root>" -ForegroundColor White
            Write-Host "   docker compose up -d" -ForegroundColor White
            exit 1
        }
    }
    
    Write-Host ""
    Write-Host "РЁР°Рі 2: РџСЂРѕРІРµСЂРєР° РґРѕСЃС‚СѓРїРЅРѕСЃС‚Рё СЃРµСЂРІРёСЃРѕРІ" -ForegroundColor Cyan
    Write-Host ""
    
    # РџСЂРѕРІРµСЂРєР° PostgreSQL
    Write-Host "  РџСЂРѕРІРµСЂРєР° PostgreSQL..." -ForegroundColor DarkGray
    try {
        $pgResult = docker exec surveillance-postgres pg_isready -U surveillance 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "    вњ“ PostgreSQL РіРѕС‚РѕРІ" -ForegroundColor Green
        } else {
            Write-Host "    вљ  PostgreSQL РЅРµ РѕС‚РІРµС‡Р°РµС‚" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "    вљ  РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕРІРµСЂРёС‚СЊ PostgreSQL" -ForegroundColor Yellow
    }
    
    # РџСЂРѕРІРµСЂРєР° Redis
    Write-Host "  РџСЂРѕРІРµСЂРєР° Redis..." -ForegroundColor DarkGray
    try {
        $redisResult = docker exec surveillance-redis redis-cli ping 2>&1
        if ($redisResult -eq "PONG") {
            Write-Host "    вњ“ Redis РіРѕС‚РѕРІ" -ForegroundColor Green
        } else {
            Write-Host "    вљ  Redis РЅРµ РѕС‚РІРµС‡Р°РµС‚" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "    вљ  РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕРІРµСЂРёС‚СЊ Redis" -ForegroundColor Yellow
    }
    
    # РџСЂРѕРІРµСЂРєР° РѕСЃРЅРѕРІРЅРѕРіРѕ СЃРµСЂРІРёСЃР°
    Write-Host "  РџСЂРѕРІРµСЂРєР° surveillance service..." -ForegroundColor DarkGray
    try {
        $healthUrl = "http://localhost:8080/api/v1/health"
        $response = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 10 -UseBasicParsing 2>&1
        if ($response.StatusCode -eq 200) {
            Write-Host "    вњ“ Surveillance service РіРѕС‚РѕРІ" -ForegroundColor Green
        } else {
            Write-Host "    вљ  Surveillance service РІРµСЂРЅСѓР» СЃС‚Р°С‚СѓСЃ: $($response.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "    вљ  РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕРІРµСЂРёС‚СЊ surveillance service" -ForegroundColor Yellow
        Write-Host "    РЎРµСЂРІРёСЃ РјРѕР¶РµС‚ Р±С‹С‚СЊ РµС‰С‘ РІ РїСЂРѕС†РµСЃСЃРµ Р·Р°РїСѓСЃРєР°" -ForegroundColor DarkGray
    }
    
    Write-Host ""
} else {
    Write-Host "РЁР°Рі 1: РџСЂРѕРїСѓС‰РµРЅР° РїСЂРѕРІРµСЂРєР° РёРЅС„СЂР°СЃС‚СЂСѓРєС‚СѓСЂС‹ (РїРѕ Р·Р°РїСЂРѕСЃСѓ)" -ForegroundColor Cyan
    Write-Host ""
}

# Р—Р°РїСѓСЃРє РѕСЃРЅРѕРІРЅРѕРіРѕ С‚РµСЃС‚Р°
Write-Host "РЁР°Рі 3: Р—Р°РїСѓСЃРє MVP Automated Acceptance" -ForegroundColor Cyan
Write-Host ""

# РРјРїРѕСЂС‚РёСЂСѓРµРј РѕСЃРЅРѕРІРЅРѕР№ СЃРєСЂРёРїС‚
$mvpScript = Join-Path $ScriptDir "ci\mvp-automated-acceptance.ps1"

if (-not (Test-Path $mvpScript)) {
    Write-Host "вќЊ РЎРєСЂРёРїС‚ mvp-automated-acceptance.ps1 РЅРµ РЅР°Р№РґРµРЅ: $mvpScript" -ForegroundColor Red
    exit 1
}

# Р—Р°РїСѓСЃРє СЃ РїР°СЂР°РјРµС‚СЂР°РјРё
$arguments = @()
if ($SkipVideoGate) { $arguments += "-SkipVideoGate" }
if ($GeneratePhase1Summary) { 
    $arguments += "-GeneratePhase1Summary"
    $arguments += "-Phase1SummaryProfile"
    $arguments += $Phase1SummaryProfile
}

Write-Host "Р—Р°РїСѓСЃРє: mvp-automated-acceptance.ps1 $($arguments -join ' ')" -ForegroundColor DarkGray
Write-Host ""

& $mvpScript @arguments

$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Р РµР·СѓР»СЊС‚Р°С‚С‹" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if ($exitCode -eq 0) {
    Write-Host "вњ… MVP Automated Acceptance СѓСЃРїРµС€РЅРѕ Р·Р°РІРµСЂС€РµРЅ!" -ForegroundColor Green
} else {
    Write-Host "вќЊ MVP Automated Acceptance Р·Р°РІРµСЂС€РёР»СЃСЏ СЃ РѕС€РёР±РєР°РјРё (exit code: $exitCode)" -ForegroundColor Red
    
    if ($exitCode -eq 2) {
        Write-Host "  Status: NO-GO (РєСЂРёС‚РёС‡РЅС‹Рµ РїСЂРѕР±Р»РµРјС‹)" -ForegroundColor Red
    } elseif ($exitCode -eq 3) {
        Write-Host "  Status: CONDITIONAL GO (РµСЃС‚СЊ Р·Р°РјРµС‡Р°РЅРёСЏ)" -ForegroundColor Yellow
    }
}

Write-Host ""

# РџСЂРµРґР»РѕР¶РµРЅРёРµ РїРѕСЃРјРѕС‚СЂРµС‚СЊ Р»РѕРіРё
if ($exitCode -ne 0) {
    Write-Host "РЎРѕРІРµС‚С‹ РїРѕ СѓСЃС‚СЂР°РЅРµРЅРёСЋ РїСЂРѕР±Р»РµРј:" -ForegroundColor Cyan
    Write-Host "  1. РџСЂРѕРІРµСЂСЊС‚Рµ Р»РѕРіРё РєРѕРЅС‚РµР№РЅРµСЂРѕРІ: docker compose logs" -ForegroundColor DarkGray
    Write-Host "  2. РџСЂРѕРІРµСЂСЊС‚Рµ Р»РѕРіРё С‚РµСЃС‚Р°: docs/reports/ mvp-automated-acceptance-*.md" -ForegroundColor DarkGray
    Write-Host "  3. РЈР±РµРґРёС‚РµСЃСЊ С‡С‚Рѕ РІСЃРµ Р·Р°РІРёСЃРёРјРѕСЃС‚Рё СѓСЃС‚Р°РЅРѕРІР»РµРЅС‹" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  РљРѕРЅРµС†" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

exit $exitCode
