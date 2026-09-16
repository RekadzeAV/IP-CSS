# Download AI analytics models (YOLO, etc.) into data/models or specified directory
# Usage: .\download-models.ps1 [-OutDir "data/models"]
param(
    [switch]$ShowHelp,
    [string]$OutDir = "data/models"
)

if ($ShowHelp) {
    Write-Host "Download small YOLOv8n ONNX into a models directory (default data/models)."
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\download-models.ps1 -ShowHelp"
    Write-Host "  .\scripts\download-models.ps1 [-OutDir <path>]"
    Write-Host ""
    Write-Host "Exit codes: 0 always after run (failed download is a warning only)."
    exit 0
}

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# YOLOv8n ONNX (small, from Ultralytics)
$yoloUrl = "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.onnx"
$yoloPath = Join-Path $OutDir "yolov8n.onnx"

if (-not (Test-Path $yoloPath)) {
    Write-Host "Downloading YOLOv8n ONNX to $yoloPath ..."
    try {
        Invoke-WebRequest -Uri $yoloUrl -OutFile $yoloPath -UseBasicParsing
        Write-Host "Done: $yoloPath"
    } catch {
        Write-Warning "Download failed: $_. Message: $($_.Exception.Message)"
        Write-Host "You can export YOLOv8 to ONNX manually: pip install ultralytics && yolo export model=yolov8n.pt format=onnx"
    }
} else {
    Write-Host "Already exists: $yoloPath"
}

Write-Host "Models directory: $OutDir"
