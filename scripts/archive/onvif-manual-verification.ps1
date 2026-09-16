# РЎРѕРІРјРµСЃС‚РёРјРѕСЃС‚СЊ: РїСЂРµР¶РЅРµРµ РёРјСЏ СЃРєСЂРёРїС‚Р°. Р РµР°Р»РёР·Р°С†РёСЏ вЂ” onvif-events-api-verification.ps1
# No param(): forward @args as-is (do not splat a rebuilt array вЂ” switches like -Help would become positional).
$inner = Join-Path $PSScriptRoot "onvif-events-api-verification.ps1"

$hasShowHelp = $false
foreach ($a in $args) {
    if ($null -ne $a -and [string]$a -ieq "-ShowHelp") {
        $hasShowHelp = $true
        break
    }
}
if ($hasShowHelp) {
    Write-Host "ONVIF manual verification (legacy entry point; forwards to onvif-events-api-verification.ps1)" -ForegroundColor DarkGray
    Write-Host ""
    & $inner -ShowHelp
    exit $LASTEXITCODE
}

& $inner @args
