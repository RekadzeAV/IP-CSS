param(
    [switch]$ShowHelp,
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Copy example JSON configs under config/ to *.local.json (video e2e / ONVIF / runtime matrix)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\bootstrap-local-test-configs.ps1"
    Write-Host "  .\scripts\bootstrap-local-test-configs.ps1 -Force"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Force  Overwrite existing *.local.json files"
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  Completed (skipped or copied)"
    Write-Host "  Non-zero  Missing example file or copy error"
    exit 0
}

function Set-LocalConfig {
    param(
        [string]$ExamplePath,
        [string]$LocalPath
    )

    if (-not (Test-Path -LiteralPath $ExamplePath)) {
        throw "Example config not found: $ExamplePath"
    }

    if ((Test-Path -LiteralPath $LocalPath) -and -not $Force) {
        Write-Host "Skip (already exists): $LocalPath" -ForegroundColor DarkGray
        return
    }

    Copy-Item -LiteralPath $ExamplePath -Destination $LocalPath -Force
    Write-Host "Created local config: $LocalPath" -ForegroundColor Green
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$configDir = Join-Path $projectRoot "config"

$pairs = @(
    @{
        Example = (Join-Path $configDir "test-cameras.example.json")
        Local = (Join-Path $configDir "test-cameras.local.json")
    },
    @{
        Example = (Join-Path $configDir "video-runtime-matrix.example.json")
        Local = (Join-Path $configDir "video-runtime-matrix.local.json")
    },
    @{
        Example = (Join-Path $configDir "video-e2e-acceptance-profile.example.json")
        Local = (Join-Path $configDir "video-e2e-acceptance-profile.local.json")
    }
)

foreach ($p in $pairs) {
    Set-LocalConfig -ExamplePath $p.Example -LocalPath $p.Local
}

Write-Host "Local config bootstrap complete." -ForegroundColor Cyan
