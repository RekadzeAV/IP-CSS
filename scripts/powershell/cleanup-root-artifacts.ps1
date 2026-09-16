# Скрипт для удаления артефактов сборок из корня проекта
$root = "e:\GitHub-Ai\IP-CSS"

# build_output*.txt
Get-ChildItem -Path $root -Filter "build_output*.txt" -File | ForEach-Object {
    Remove-Item -Path $_.FullName -Force
    Write-Host "Removed: $($_.Name)"
}

# ktlint_*.txt
Get-ChildItem -Path $root -Filter "ktlint_*.txt" -File | ForEach-Object {
    Remove-Item -Path $_.FullName -Force
    Write-Host "Removed: $($_.Name)"
}

# test_output.txt
if (Test-Path "$root\test_output.txt") {
    Remove-Item -Path "$root\test_output.txt" -Force
    Write-Host "Removed: test_output.txt"
}

# build-errors.log
Get-ChildItem -Path $root -Filter "build-errors*.log" -File | ForEach-Object {
    Remove-Item -Path $_.FullName -Force
    Write-Host "Removed: $($_.Name)"
}

# video_processing.dll (в корне)
if (Test-Path "$root\video_processing.dll") {
    Remove-Item -Path "$root\video_processing.dll" -Force
    Write-Host "Removed: video_processing.dll"
}

# onvif_results.json, rtsp_test_results.json
if (Test-Path "$root\onvif_results.json") { Remove-Item -Path "$root\onvif_results.json" -Force }
if (Test-Path "$root\rtsp_test_results.json") { Remove-Item -Path "$root\rtsp_test_results.json" -Force }

Write-Host "=== Cleanup complete ==="