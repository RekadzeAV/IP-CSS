# РџСЂРѕРІРµСЂРєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рє NAS РїРµСЂРµРґ Field Validation
# Usage: .\scripts\test-nas-connection.ps1

Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'РџСЂРѕРІРµСЂРєР° РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рє NAS' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan

$NASHost = '192.168.10.38'
$Username = 'Andrey'
$Password = '$env:NAS_PASSWORD'
$PlinkPath = 'C:\Program Files\PuTTY\plink.exe'

Write-Host "NAS Host: $NASHost" -ForegroundColor White
Write-Host "Username: $Username" -ForegroundColor White

# РџСЂРѕРІРµСЂРєР° СЃРµС‚РµРІРѕРіРѕ РїРѕРґРєР»СЋС‡РµРЅРёСЏ
Write-Host "`n[1/3] РџСЂРѕРІРµСЂРєР° СЃРµС‚РµРІРѕРіРѕ РїРѕРґРєР»СЋС‡РµРЅРёСЏ..." -ForegroundColor Yellow
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $async = $tcp.BeginConnect($NASHost, 22, $null, $null)
    $wait = $async.AsyncWaitHandle.WaitOne(3000)
    
    if ($wait) {
        $tcp.Close()
        Write-Host "  OK SSH РїРѕСЂС‚ 22 РґРѕСЃС‚СѓРїРµРЅ РЅР° $NASHost" -ForegroundColor Green
    }
    else {
        $tcp.Close()
        Write-Host '  ERROR SSH РїРѕСЂС‚ 22 РЅРµРґРѕСЃС‚СѓРїРµРЅ' -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "  ERROR $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# РџСЂРѕРІРµСЂРєР° SSH РїРѕРґРєР»СЋС‡РµРЅРёСЏ
Write-Host "`n[2/3] РџСЂРѕРІРµСЂРєР° SSH РїРѕРґРєР»СЋС‡РµРЅРёСЏ..." -ForegroundColor Yellow

if (-not (Test-Path $PlinkPath)) {
    Write-Host '  ERROR plink.exe РЅРµ РЅР°Р№РґРµРЅ' -ForegroundColor Red
    Write-Host '  РЈСЃС‚Р°РЅРѕРІРёС‚Рµ PuTTY' -ForegroundColor Yellow
    exit 1
}

$testCommand = 'echo SSH connection test successful'
$plinkArgs = @('-ssh', '-batch', '-pw', $Password, "${Username}@${NASHost}", $testCommand)

$plinkProcess = Start-Process -FilePath $PlinkPath -ArgumentList $plinkArgs -Wait -PassThru -RedirectStandardOutput 'temp-ssh-output.txt'

if ($plinkProcess.ExitCode -eq 0) {
    Write-Host '  OK SSH РїРѕРґРєР»СЋС‡РµРЅРёРµ СѓСЃРїРµС€РЅРѕ!' -ForegroundColor Green
}
else {
    Write-Host '  ERROR SSH РїРѕРґРєР»СЋС‡РµРЅРёРµ РЅРµ СѓРґР°Р»РѕСЃСЊ' -ForegroundColor Red
    exit 1
}

Remove-Item 'temp-ssh-output.txt' -ErrorAction SilentlyContinue

# РџСЂРѕРІРµСЂРєР° Git СЂРµРїРѕР·РёС‚РѕСЂРёСЏ
Write-Host "`n[3/3] РџСЂРѕРІРµСЂРєР° Git СЂРµРїРѕР·РёС‚РѕСЂРёСЏ..." -ForegroundColor Yellow

$gitCommand = 'ls -la /volume1/Git/'
$plinkArgs = @('-ssh', '-batch', '-pw', $Password, "${Username}@${NASHost}", $gitCommand)

$plinkProcess = Start-Process -FilePath $PlinkPath -ArgumentList $plinkArgs -Wait -PassThru -RedirectStandardOutput 'temp-git-output.txt'

if ($plinkProcess.ExitCode -eq 0) {
    $output = Get-Content 'temp-git-output.txt' -Raw
    if ($output -match 'IP-CSS-open.git') {
        Write-Host '  OK Git СЂРµРїРѕР·РёС‚РѕСЂРёР№ РЅР°Р№РґРµРЅ!' -ForegroundColor Green
    }
    else {
        Write-Host '  WARNING Git СЂРµРїРѕР·РёС‚РѕСЂРёР№ РЅРµ РЅР°Р№РґРµРЅ' -ForegroundColor Yellow
    }
}
else {
    Write-Host '  ERROR РќРµ СѓРґР°Р»РѕСЃСЊ РїРѕР»СѓС‡РёС‚СЊ РґРѕСЃС‚СѓРї Рє Git' -ForegroundColor Red
}

Remove-Item 'temp-git-output.txt' -ErrorAction SilentlyContinue

# РС‚РѕРі
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host 'РС‚РѕРі: РџРѕРґРєР»СЋС‡РµРЅРёРµ Рє NAS СЂР°Р±РѕС‚Р°РµС‚!' -ForegroundColor Green
Write-Host '========================================' -ForegroundColor Cyan
Write-Host "`nРЎР»РµРґСѓСЋС‰РёР№ С€Р°Рі:" -ForegroundColor Yellow
Write-Host "  .\scripts\run-nas-field-validation.ps1 -Platforms Synology -LongRunDuration 24" -ForegroundColor White
