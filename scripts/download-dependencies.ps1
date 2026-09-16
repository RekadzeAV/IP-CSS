# Скрипт для скачивания всех зависимостей проекта в локальный Maven-репозиторий
# Использование: .\scripts\download-dependencies.ps1

$ErrorActionPreference = "Stop"

$LocalRepo = "$PSScriptRoot\..\local-maven-repo"
$GradleCache = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Downloading all dependencies to local-maven-repo" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Gradle cache: $GradleCache" -ForegroundColor Yellow
Write-Host "Local repo:   $LocalRepo" -ForegroundColor Yellow
Write-Host ""

if (!(Test-Path $GradleCache)) {
    Write-Host "ERROR: Gradle cache not found at: $GradleCache" -ForegroundColor Red
    Write-Host "Run a Gradle build first to download dependencies, then run this script." -ForegroundColor Red
    exit 1
}

# Создаём директорию если не существует
if (!(Test-Path $LocalRepo)) {
    New-Item -ItemType Directory -Force -Path $LocalRepo | Out-Null
}

$totalFiles = 0
$totalSize = 0L

# Копируем все зависимости из Gradle cache
# Структура: group/artifact/version/hash/filename.jar
$groups = Get-ChildItem -Path $GradleCache -Directory
Write-Host "Found $($groups.Count) groups in Gradle cache..." -ForegroundColor Green

foreach ($group in $groups) {
    $artifacts = Get-ChildItem -Path $group.FullName -Directory
    foreach ($artifact in $artifacts) {
        $versions = Get-ChildItem -Path $artifact.FullName -Directory
        foreach ($version in $versions) {
            # В каждой версии могут быть подкаталоги с хешами
            $hashDirs = Get-ChildItem -Path $version.FullName -Directory
            foreach ($hashDir in $hashDirs) {
                $files = Get-ChildItem -Path $hashDir.FullName -File | Where-Object {
                    $_.Name -match '\.(jar|pom)$'
                }
                
                foreach ($file in $files) {
                    $destDir = Join-Path $LocalRepo "$($group.Name)/$($artifact.Name)/$($version.Name)"
                    if (!(Test-Path $destDir)) {
                        New-Item -ItemType Directory -Force -Path $destDir | Out-Null
                    }
                    
                    $destFile = Join-Path $destDir $file.Name
                    if (!(Test-Path $destFile)) {
                        Copy-Item -Path $file.FullName -Destination $destFile -Force
                        $totalFiles++
                        $totalSize += $file.Length
                    }
                }
            }
        }
    }
}
            
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Download complete!" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total files copied: $totalFiles" -ForegroundColor Green
Write-Host "Total size: $([math]::Round($totalSize / 1MB, 2)) MB" -ForegroundColor Green
Write-Host "Local repository: $LocalRepo" -ForegroundColor Green
Write-Host ""
Write-Host "The project is now configured to use local-maven-repo first." -ForegroundColor Yellow
Write-Host "Builds will use local dependencies without network access." -ForegroundColor Yellow
Write-Host ""
