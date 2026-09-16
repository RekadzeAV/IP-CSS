param(
    [switch]$ShowHelp,
    [string]$ConfigPath = "config\test-cameras.local.json",
    [switch]$AsJson = $false
)

$ErrorActionPreference = "Stop"

if ($ShowHelp) {
    Write-Host "Validate ONVIF test camera JSON config (structure + required fields)"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\validate-onvif-test-camera-config.ps1"
    Write-Host "  .\scripts\validate-onvif-test-camera-config.ps1 -ConfigPath config\test-cameras.local.json"
    Write-Host "  .\scripts\validate-onvif-test-camera-config.ps1 -AsJson"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -ConfigPath  Repo-relative or absolute path (default config\test-cameras.local.json)."
    Write-Host "  -AsJson  Print validation object as JSON to stdout."
    Write-Host ""
    Write-Host "Exit codes:"
    Write-Host "  0  PASS (no errors; no or ignored warnings for exit)"
    Write-Host "  1  WARN (no errors, at least one warning)"
    Write-Host "  2  FAIL (errors present)"
    exit 0
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$fullPath = if ([System.IO.Path]::IsPathRooted($ConfigPath)) { $ConfigPath } else { Join-Path $projectRoot $ConfigPath }

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]
$cameraCount = 0

if (-not (Test-Path $fullPath)) {
    $errors.Add("Config file not found: $fullPath")
} else {
    try {
        $raw = Get-Content $fullPath -Raw
        $cfg = $raw | ConvertFrom-Json
        $cameras = @($cfg.cameras)
        $cameraCount = $cameras.Count
        if ($cameraCount -eq 0) {
            $errors.Add("No cameras defined in config.")
        }

        for ($i = 0; $i -lt $cameraCount; $i++) {
            $cam = $cameras[$i]
            foreach ($field in @("host", "username", "password")) {
                if (-not ($cam.PSObject.Properties.Name -contains $field) -or [string]::IsNullOrWhiteSpace([string]$cam.$field)) {
                    $errors.Add("Camera[$i] missing required field '$field'.")
                }
            }

            if ($cam.PSObject.Properties.Name -contains "rtspPort") {
                $portVal = [string]$cam.rtspPort
                if ($portVal -and -not [int]::TryParse($portVal, [ref]([int]0))) {
                    $warnings.Add("Camera[$i] rtspPort is not an integer: '$portVal'.")
                }
            }
        }

        if (-not $cfg.defaults) {
            $warnings.Add("defaults section is missing (optional but recommended).")
        } elseif (-not $cfg.defaults.httpPorts) {
            $warnings.Add("defaults.httpPorts missing (recommended for ONVIF HTTP checks).")
        }
    } catch {
        $errors.Add("Failed to parse json: $($_.Exception.Message)")
    }
}

$status = if ($errors.Count -gt 0) { "FAIL" } elseif ($warnings.Count -gt 0) { "WARN" } else { "PASS" }

$result = [ordered]@{
    status = $status
    configPath = $fullPath
    cameraCount = $cameraCount
    errors = @($errors)
    warnings = @($warnings)
}

if ($AsJson) {
    $result | ConvertTo-Json -Depth 6
} else {
    Write-Host "ONVIF config validation: $status" -ForegroundColor $(if ($status -eq "PASS") { "Green" } elseif ($status -eq "WARN") { "Yellow" } else { "Red" })
    Write-Host ("Config: {0}" -f $fullPath)
    Write-Host ("Cameras: {0}" -f $cameraCount)
    if ($errors.Count -gt 0) {
        Write-Host "Errors:" -ForegroundColor Red
        $errors | ForEach-Object { Write-Host ("  - {0}" -f $_) -ForegroundColor Red }
    }
    if ($warnings.Count -gt 0) {
        Write-Host "Warnings:" -ForegroundColor Yellow
        $warnings | ForEach-Object { Write-Host ("  - {0}" -f $_) -ForegroundColor Yellow }
    }
}

if ($status -eq "FAIL") { exit 2 }
if ($status -eq "WARN") { exit 1 }
exit 0
