#!/bin/bash
# Скрипт для создания веток платформ в Git
# Bash версия

echo "Creating platform branches..."

# Основная ветка (уже должна существовать)
MAIN_BRANCH="main"

# Проверяем, что мы на главной ветке
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$MAIN_BRANCH" ]; then
    echo "Switching to $MAIN_BRANCH branch..."
    git checkout $MAIN_BRANCH
fi

# Список платформ
PLATFORMS=(
    "platform-sbc-arm"
    "platform-server-x86_64"
    "platform-nas-arm"
    "platform-nas-x86_64"
    "platform-client-desktop-x86_64"
    "platform-client-desktop-arm"
    "platform-client-android"
    "platform-client-ios"
)

# Создаем ветки develop и test для каждой платформы
for PLATFORM in "${PLATFORMS[@]}"; do
    DEVELOP_BRANCH="develop/$PLATFORM"
    TEST_BRANCH="test/$PLATFORM"

    # Создаем develop ветку
    if git show-ref --verify --quiet refs/heads/$DEVELOP_BRANCH; then
        echo "Branch $DEVELOP_BRANCH already exists, skipping..."
    else
        echo "Creating branch: $DEVELOP_BRANCH"
        git checkout -b $DEVELOP_BRANCH
        git checkout $MAIN_BRANCH
    fi

    # Создаем test ветку от develop
    if git show-ref --verify --quiet refs/heads/$TEST_BRANCH; then
        echo "Branch $TEST_BRANCH already exists, skipping..."
    else
        echo "Creating branch: $TEST_BRANCH"
        git checkout $DEVELOP_BRANCH
        git checkout -b $TEST_BRANCH
        git checkout $MAIN_BRANCH
    fi
done

echo ""
echo "All platform branches created successfully!"
echo ""
echo "Branch structure:"
git branch -a | grep -E "(develop|test)/platform"



