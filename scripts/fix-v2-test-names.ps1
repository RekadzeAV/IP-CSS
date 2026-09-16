# Fix V2 test class names and references in all repository test files
$testDir = "shared/src/commonTest/kotlin/com/company/ipcamera/shared/data/repository"
$files = @(
    "$testDir/CameraRepositoryImplTest.kt",
    "$testDir/EventRepositoryImplTest.kt",
    "$testDir/RecordingRepositoryImplTest.kt",
    "$testDir/SettingsRepositoryImplTest.kt",
    "$testDir/UserRepositoryImplTest.kt",
    "$testDir/NotificationRepositoryImplTest.kt"
)

$count = 0
foreach ($f in $files) {
    if (Test-Path $f) {
        $content = Get-Content $f -Raw
        $content = $content -replace 'ImplV2Test', 'ImplTest'
        $content = $content -replace 'ImplV2\(', 'Impl('
        $content = $content -replace 'ImplV2 ', 'Impl '
        Set-Content $f $content
        Write-Host "  Fixed: $f"
        $count++
    }
}
Write-Host "`nDone! $count test files updated."