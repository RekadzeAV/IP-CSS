$src = "e:\GitHub-Ai\IP-CSS"
$dst = "e:\GitHub-Ai\IP-CSS\scripts\powershell"

$files = @(
    "build-pipeline.ps1",
    "configure_nas_recording.ps1",
    "setup_nas_integration.ps1",
    "start_docker_with_nas.ps1",
    "test-rtsp-client.ps1",
    "test-rtsp-connection.ps1",
    "test-rtsp-integration.ps1",
    "test_cameras.ps1",
    "test_docker_nas_ldap.ps1",
    "test_ldap_connection.ps1",
    "test_onvif_simple.ps1",
    "test_rtsp_integration.ps1",
    "test_rtsp_streams.ps1",
    "update_camera_rtsp_urls.ps1",
    "update_rtsp_urls_db.ps1",
    ".ps1"
)

foreach ($f in $files) {
    $s = Join-Path $src $f
    $d = Join-Path $dst $f
    if (Test-Path $s) {
        Move-Item -Path $s -Destination $d -Force
        Write-Host "Moved: $f"
    } else {
        Write-Host "Not found: $f"
    }
}
Write-Host "Done moving PS1 files."