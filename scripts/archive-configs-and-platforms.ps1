# Archive unused NAS platforms, duplicate configs and certificate-pins
$rootDir = "."

# Archive NAS platforms
$nasDirs = @('nas-asustor','nas-qnap','nas-synology','nas-truenas','nas-common')
foreach ($dir in $nasDirs) {
    $src = "$rootDir/platforms/$dir"
    $dst = "$rootDir/archive/platforms/$dir"
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dst -Recurse -Force
        Remove-Item -Path $src -Recurse -Force
        Write-Host "Archived: $dir"
    }
}

# Archive duplicate test-cameras configs
$cameras = @('test-cameras.example.json','test-cameras-docker.json','test-cameras-local-network.example.json','test-cameras-local-network.json','test-cameras.rtsp.json')
foreach ($f in $cameras) {
    $src = "$rootDir/config/$f"
    $dst = "$rootDir/archive/config/test-cameras/$f"
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dst -Force
        Remove-Item -Path $src -Force
        Write-Host "Archived: $f"
    }
}

# Archive duplicate certificate-pins
$certs = @(
    @("$rootDir/core/network/certificate-pins.example.json", "$rootDir/archive/config/certificate-pins/certificate-pins.example.json"),
    @("$rootDir/core/network/CERTIFICATE_PINNING.md", "$rootDir/archive/config/certificate-pins/CERTIFICATE_PINNING.md"),
    @("$rootDir/core/network/IOS_CERTIFICATE_PINNING_COMPLETE.md", "$rootDir/archive/config/certificate-pins/IOS_CERTIFICATE_PINNING_COMPLETE.md"),
    @("$rootDir/config/README-CERTIFICATE-PINS.md", "$rootDir/archive/config/certificate-pins/README-CERTIFICATE-PINS.md"),
    @("$rootDir/docs/CERTIFICATE_PINNING_SETUP.md", "$rootDir/archive/config/certificate-pins/CERTIFICATE_PINNING_SETUP.md"),
    @("$rootDir/docs/CERTIFICATE_PINNING_USAGE.md", "$rootDir/archive/config/certificate-pins/CERTIFICATE_PINNING_USAGE.md")
)
foreach ($pair in $certs) {
    $src = $pair[0]
    $dst = $pair[1]
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dst -Force
        Remove-Item -Path $src -Force
        Write-Host "Archived: $src"
    }
}

Write-Host "Phase 1.3-1.4 complete!"