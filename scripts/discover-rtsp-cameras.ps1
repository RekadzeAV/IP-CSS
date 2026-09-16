#!/usr/bin/env pwsh
# Скрипт для обнаружения RTSP камер в локальной сети

param(
    [string]$Subnet = "192.168.1.0/24",
    [int]$RtspPort = 554,
    [int]$TimeoutSeconds = 2,
    [string]$OutputFile = "diagnostics/discovered-cameras.json",
    [switch]$ShowHelp
)

if ($ShowHelp) {
    Write-Host @"
Обнаружение RTSP камер в локальной сети

Usage:
  .\scripts\discover-rtsp-cameras.ps1 [-Subnet <subnet>] [-RtspPort <port>]

Options:
  -Subnet         Подсеть для сканирования (default: 192.168.1.0/24)
  -RtspPort       Port RTSP (default: 554)
  -TimeoutSeconds Таймаут подключения (default: 2)
  -OutputFile     Файл для результатов (default: diagnostics/discovered-cameras.json)
  -ShowHelp       Показать эту справку

Examples:
  # Сканирование домашней сети
  .\scripts\discover-rtsp-cameras.ps1 -Subnet 192.168.1.0/24

  # Сканирование офисной сети
  .\scripts\discover-rtsp-cameras.ps1 -Subnet 10.0.0.0/24 -RtspPort 5540

"@
    exit 0
}

# ============================================================================
# Setup
# ============================================================================

$ErrorActionPreference = "Continue"

# Resolve paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$OutputFile = Join-Path $projectRoot $OutputFile

$outputDir = Split-Path -Parent $OutputFile
if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

Write-Host "RTSP Camera Discovery" -ForegroundColor Cyan
Write-Host "Subnet:     $Subnet" -ForegroundColor White
Write-Host "Port:       $RtspPort" -ForegroundColor White
Write-Host "Timeout:    ${TimeoutSeconds}s" -ForegroundColor White
Write-Host "Output:     $OutputFile" -ForegroundColor White
Write-Host ""

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Success {
    param([string]$Message)
    Write-Host "  ✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "  ✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ $Message" -ForegroundColor Cyan
}

function Get-IPRange {
    param([string]$Cidr)
    
    $parts = $Cidr -split '/'
    $ip = $parts[0]
    $mask = [int]$parts[1]
    
    $ipParts = $ip -split '\.' | ForEach-Object { [int]$_ }
    $baseIP = ([int]$ipParts[0] * 16777216) + ([int]$ipParts[1] * 65536) + ([int]$ipParts[2] * 256) + [int]$ipParts[3]
    $maskBits = [math]::Pow(2, (32 - $mask)) - 1
    
    $startIP = $baseIP
    $endIP = $baseIP + $maskBits
    
    $ips = @()
    for ($i = $startIP; $i -le $endIP; $i++) {
        $octet4 = $i % 256
        $octet3 = ($i / 256) % 256
        $octet2 = ($i / 65536) % 256
        $octet1 = ($i / 16777216) % 256
        $ips += "$octet1.$octet2.$octet3.$octet4"
    }
    
    return $ips
}

function Test-RTSPConnection {
    param(
        [string]$IP,
        [int]$Port,
        [int]$TimeoutSeconds
    )
    
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $asyncResult = $tcpClient.BeginConnect($IP, $Port, $null, $null)
        $wait = $asyncResult.AsyncWaitHandle.WaitOne($TimeoutSeconds * 1000)
        
        if ($wait) {
            $tcpClient.EndConnect($asyncResult)
            $tcpClient.Close()
            return $true
        } else {
            $tcpClient.Close()
            return $false
        }
    } catch {
        return $false
    }
}

function Test-OnvifDiscovery {
    param(
        [string]$IP,
        [int]$TimeoutSeconds
    )
    
    # Попытка определить производителя через HTTP
    try {
        $webClient = New-Object System.Net.WebClient
        $webClient.Timeout = $TimeoutSeconds * 1000
        
        $urls = @(
            "http://$IP/",
            "http://$IP:8080/",
            "http://$IP/onvif/device_service"
        )
        
        foreach ($url in $urls) {
            try {
                $response = $webClient.DownloadString($url)
                if ($response -match "(HIKVISION|Dahua|Axis|Bosch|Canon|Sony|Netcam|IP Camera)") {
                    return $Matches.Values[0]
                }
            } catch {
                continue
            }
        }
        
        return "Unknown"
    } catch {
        return "Unknown"
    }
}

# ============================================================================
# Main Execution
# ============================================================================

Write-Host "Scanning subnet..." -ForegroundColor Yellow
Write-Host ""

$ips = Get-IPRange -Cidr $Subnet
$discoveredCameras = @()
$totalIPs = $ips.Count
$foundCount = 0

Write-Host "Total IPs to scan: $totalIPs" -ForegroundColor Gray
Write-Host ""

$progress = 0
foreach ($ip in $ips) {
    $progress++
    Write-Progress -Activity "Scanning..." -Status "$ip" -PercentComplete (($progress / $totalIPs) * 100) -Id 1
    
    # Проверка RTSP порта
    if (Test-RTSPConnection -IP $ip -Port $RtspPort -TimeoutSeconds $TimeoutSeconds) {
        Write-Success "RTSP port open: $ip`:$RtspPort"
        
        # Попытка определить производителя
        $manufacturer = Test-OnvifDiscovery -IP $ip -TimeoutSeconds $TimeoutSeconds
        
        $camera = @{
            ip = $ip
            port = $RtspPort
            rtspUrl = "rtsp://$ip`:$RtspPort/stream1"
            manufacturer = $manufacturer
            status = "discovered"
            discoveredAt = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
            notes = "Auto-discovered camera"
        }
        
        $discoveredCameras += $camera
        $foundCount++
    }
}

Write-Progress -Activity "Scanning..." -Completed -Id 1

Write-Host ""
Write-Host "Discovery Complete" -ForegroundColor Cyan
Write-Host "-----------------" -ForegroundColor Gray
Write-Host "IPs scanned:    $totalIPs" -ForegroundColor White
Write-Host "Cameras found:  $foundCount" -ForegroundColor $(if ($foundCount -gt 0) { "Green" } else { "Yellow" })
Write-Host ""

# ============================================================================
# Generate Report
# ============================================================================

if ($foundCount -gt 0) {
    $report = @{
        discoveryTime = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
        subnet = $Subnet
        port = $RtspPort
        timeoutSeconds = $TimeoutSeconds
        totalIPsScanned = $totalIPs
        camerasFound = $foundCount
        cameras = $discoveredCameras
    }
    
    # Сохранение JSON
    $report | ConvertTo-Json -Depth 10 | Set-Content -Path $OutputFile -Encoding UTF8
    Write-Success "Results saved to: $OutputFile"
    
    # Вывод таблицы
    Write-Host ""
    Write-Host "Discovered Cameras:" -ForegroundColor Cyan
    Write-Host "-------------------" -ForegroundColor Gray
    Write-Host ""
    
    foreach ($camera in $discoveredCameras) {
        Write-Host "IP:           $($camera.ip)`:$($camera.port)" -ForegroundColor White
        Write-Host "RTSP URL:     $($camera.rtspUrl)" -ForegroundColor Gray
        Write-Host "Manufacturer: $($camera.manufacturer)" -ForegroundColor Gray
        Write-Host "Status:       $($camera.status)" -ForegroundColor Green
        Write-Host "Notes:        $($camera.notes)" -ForegroundColor DarkGray
        Write-Host ""
    }
    
    # Предложение добавить в конфиг
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "-----------" -ForegroundColor Gray
    Write-Host "1. Review discovered cameras in: $OutputFile" -ForegroundColor White
    Write-Host "2. Update config/test-cameras-local-network.example.json with credentials" -ForegroundColor Yellow
    Write-Host "3. Run RTSP tests: .\scripts\test-rtsp-real-cameras.ps1" -ForegroundColor White
    Write-Host ""
    
} else {
    Write-Error "No RTSP cameras found in subnet $Subnet"
    Write-Host ""
    Write-Host "Possible reasons:" -ForegroundColor Yellow
    Write-Host "  - Cameras are on a different subnet" -ForegroundColor Gray
    Write-Host "  - RTSP port is different (try --RtspPort)" -ForegroundColor Gray
    Write-Host "  - Firewalls blocking access" -ForegroundColor Gray
    Write-Host "  - Cameras use non-standard RTSP implementation" -ForegroundColor Gray
    Write-Host ""
}

exit 0
