# Скрипт для удаления старых веток, которые заменены новой структурой платформ
# PowerShell версия

Write-Host "Analyzing old branches..." -ForegroundColor Green

# Список старых веток для удаления
$oldBranches = @(
    "dev/android",
    "dev/desktop",
    "dev/ios",
    "develop",
    "test/android",
    "test/desktop",
    "test/ios",
    "test/integration",
    "feature/native-libraries",
    "feature/network-layer",
    "feature/server-part",
    "feature/ui-components"
)

$currentBranch = git rev-parse --abbrev-ref HEAD

Write-Host "Current branch: $currentBranch" -ForegroundColor Cyan
Write-Host "`nOld branches to delete:" -ForegroundColor Yellow
foreach ($branch in $oldBranches) {
    if (git show-ref --verify --quiet refs/heads/$branch) {
        Write-Host "  - $branch (local)" -ForegroundColor Red
    } elseif (git show-ref --verify --quiet refs/remotes/origin/$branch) {
        Write-Host "  - $branch (remote only)" -ForegroundColor Yellow
    }
}

Write-Host "`nWARNING: This will delete the old branch structure." -ForegroundColor Red
Write-Host "New branch structure uses: develop/platform-* and test/platform-*" -ForegroundColor Cyan
Write-Host "`nTo delete these branches, run:" -ForegroundColor Yellow
Write-Host "  # Local branches:" -ForegroundColor Cyan
foreach ($branch in $oldBranches) {
    if (git show-ref --verify --quiet refs/heads/$branch) {
        Write-Host "  git branch -D $branch" -ForegroundColor White
    }
}
Write-Host "`n  # Remote branches (use with caution):" -ForegroundColor Cyan
Write-Host "  git push origin --delete <branch-name>" -ForegroundColor White

