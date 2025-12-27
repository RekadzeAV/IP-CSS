# Скрипт для создания веток платформ в Git
# PowerShell версия

Write-Host "Creating platform branches..." -ForegroundColor Green

# Основная ветка (уже должна существовать)
$mainBranch = "main"

# Проверяем, что мы на главной ветке
$currentBranch = git rev-parse --abbrev-ref HEAD
if ($currentBranch -ne $mainBranch) {
    Write-Host "Switching to $mainBranch branch..." -ForegroundColor Yellow
    git checkout $mainBranch
}

# Список платформ
$platforms = @(
    "platform-sbc-arm",
    "platform-server-x86_64",
    "platform-nas-arm",
    "platform-nas-x86_64",
    "platform-client-desktop-x86_64",
    "platform-client-desktop-arm",
    "platform-client-android",
    "platform-client-ios"
)

# Создаем ветки develop и test для каждой платформы
foreach ($platform in $platforms) {
    $developBranch = "develop/$platform"
    $testBranch = "test/$platform"

    # Создаем develop ветку
    if (git show-ref --verify --quiet refs/heads/$developBranch) {
        Write-Host "Branch $developBranch already exists, skipping..." -ForegroundColor Yellow
    } else {
        Write-Host "Creating branch: $developBranch" -ForegroundColor Cyan
        git checkout -b $developBranch
        git checkout $mainBranch
    }

    # Создаем test ветку от develop
    if (git show-ref --verify --quiet refs/heads/$testBranch) {
        Write-Host "Branch $testBranch already exists, skipping..." -ForegroundColor Yellow
    } else {
        Write-Host "Creating branch: $testBranch" -ForegroundColor Cyan
        git checkout $developBranch
        git checkout -b $testBranch
        git checkout $mainBranch
    }
}

Write-Host "`nAll platform branches created successfully!" -ForegroundColor Green
Write-Host "`nBranch structure:" -ForegroundColor Cyan
git branch -a | Select-String -Pattern "(develop|test)/platform"



