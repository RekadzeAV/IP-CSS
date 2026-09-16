# Phase 2.1: Migrate repositories - archive old impl, rename V2 to final names
$repoDir = "shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository"
$archiveDir = "archive/repositories"

# Create archive directories
New-Item -ItemType Directory -Force -Path "$archiveDir/impl", "$archiveDir/sqldelight", "$archiveDir/transitional" | Out-Null

# Move old *Impl to archive
$oldImpl = @(
    "CameraRepositoryImpl.kt",
    "EventRepositoryImpl.kt",
    "RecordingRepositoryImpl.kt",
    "SettingsRepositoryImpl.kt",
    "UserRepositoryImpl.kt",
    "NotificationRepositoryImpl.kt"
)
foreach ($f in $oldImpl) {
    $src = "$repoDir/$f"
    $dst = "$archiveDir/impl/$f"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        Remove-Item $src -Force
        Write-Host "Moved: $f -> archive/repositories/impl/"
    }
}

# Move old *ImplSqlDelight to archive
$oldSql = @(
    "EventRepositoryImplSqlDelight.kt",
    "NotificationRepositoryImplSqlDelight.kt",
    "RecordingRepositoryImplSqlDelight.kt",
    "SettingsRepositoryImplSqlDelight.kt",
    "UserRepositoryImplSqlDelight.kt"
)
foreach ($f in $oldSql) {
    $src = "$repoDir/$f"
    $dst = "$archiveDir/sqldelight/$f"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        Remove-Item $src -Force
        Write-Host "Moved: $f -> archive/repositories/sqldelight/"
    }
}

# Move transitional files
$transitional = @(
    "CameraCache.kt",
    "CameraRepositoryWithEventMonitoring.kt"
)
foreach ($f in $transitional) {
    $src = "$repoDir/$f"
    $dst = "$archiveDir/transitional/$f"
    if (Test-Path $src) {
        Copy-Item $src $dst -Force
        Remove-Item $src -Force
        Write-Host "Moved: $f -> archive/repositories/transitional/"
    }
}

Write-Host "`nOld repositories archived successfully"
Write-Host "`nNow renaming V2 files to final names..."

# Rename V2 files to final names (remove 'V2' suffix)
$v2Files = @{
    "CameraRepositoryImplV2.kt" = "CameraRepositoryImpl.kt"
    "EventRepositoryImplV2.kt" = "EventRepositoryImpl.kt"
    "RecordingRepositoryImplV2.kt" = "RecordingRepositoryImpl.kt"
    "SettingsRepositoryImplV2.kt" = "SettingsRepositoryImpl.kt"
    "UserRepositoryImplV2.kt" = "UserRepositoryImpl.kt"
    "NotificationRepositoryImplV2.kt" = "NotificationRepositoryImpl.kt"
}

foreach ($oldName in $v2Files.Keys) {
    $src = "$repoDir/$oldName"
    $newName = $v2Files[$oldName]
    $dst = "$repoDir/$newName"
    if (Test-Path $src) {
        Rename-Item -Path $src -NewName $newName -Force
        Write-Host "Renamed: $oldName -> $newName"
    }
}

Write-Host "`nPhase 2.1: Repository migration complete!"