#!/bin/bash
# Скрипт для сборки и публикации всех пакетов в локальный Maven репозиторий
# Использование: ./scripts/publish-local.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функции для вывода сообщений
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

section() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Проверка зависимостей
check_dependencies() {
    section "Проверка зависимостей"
    
    if ! command -v java &> /dev/null; then
        error "Java не установлена. Установите JDK 17+"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        error "Требуется Java 17 или выше. Текущая версия: $JAVA_VERSION"
        exit 1
    fi
    
    if [ ! -f "./gradlew" ]; then
        error "Gradle wrapper не найден"
        exit 1
    fi
    
    info "Зависимости проверены успешно"
    info "Java версия: $(java -version 2>&1 | head -n 1)"
}

# Очистка предыдущих сборок
clean_build() {
    section "Очистка предыдущих сборок"
    info "Очистка build директорий..."
    ./gradlew clean
    info "Очистка завершена"
}

# Сборка всех модулей
build_modules() {
    section "Сборка всех модулей"
    info "Сборка модулей в правильном порядке зависимостей..."
    
    # Порядок сборки согласно зависимостям:
    # 1. core:common (не зависит ни от чего)
    # 2. core:network (зависит от core:common)
    # 3. core:license (зависит от shared, но shared зависит от core:network)
    # 4. shared (зависит от core:common и core:network)
    
    info "Сборка core:common..."
    ./gradlew :core:common:build --no-daemon
    
    info "Сборка core:network..."
    ./gradlew :core:network:build --no-daemon
    
    info "Сборка shared..."
    ./gradlew :shared:build --no-daemon
    
    info "Сборка core:license..."
    ./gradlew :core:license:build --no-daemon
    
    info "Все модули собраны успешно"
}

# Публикация в локальный Maven репозиторий
publish_to_local() {
    section "Публикация в локальный Maven репозиторий"
    
    LOCAL_MAVEN_REPO="$HOME/.m2/repository"
    info "Локальный Maven репозиторий: $LOCAL_MAVEN_REPO"
    
    info "Публикация всех модулей..."
    
    # Публикация в правильном порядке зависимостей
    info "Публикация core:common..."
    ./gradlew :core:common:publishToMavenLocal --no-daemon
    
    info "Публикация core:network..."
    ./gradlew :core:network:publishToMavenLocal --no-daemon
    
    info "Публикация shared..."
    ./gradlew :shared:publishToMavenLocal --no-daemon
    
    info "Публикация core:license..."
    ./gradlew :core:license:publishToMavenLocal --no-daemon
    
    info "Все модули опубликованы в локальный Maven репозиторий"
}

# Проверка опубликованных артефактов
verify_publication() {
    section "Проверка опубликованных артефактов"
    
    LOCAL_MAVEN_REPO="$HOME/.m2/repository/com/company/ipcamera"
    VERSION=$(grep "^version=" gradle.properties | cut -d'=' -f2)
    
    info "Проверка версии: $VERSION"
    
    modules=("core-common" "core-network" "core-license" "shared")
    
    for module in "${modules[@]}"; do
        MODULE_PATH="$LOCAL_MAVEN_REPO/$module/$VERSION"
        if [ -d "$MODULE_PATH" ]; then
            info "✓ $module опубликован: $MODULE_PATH"
            ls -lh "$MODULE_PATH" | tail -n +2 | while read line; do
                echo "    $line"
            done
        else
            warning "✗ $module не найден: $MODULE_PATH"
        fi
    done
}

# Вывод информации об использовании
show_usage_info() {
    section "Информация об использовании"
    
    VERSION=$(grep "^version=" gradle.properties | cut -d'=' -f2)
    
    cat << EOF
Пакеты опубликованы в локальный Maven репозиторий.

Для использования в других проектах добавьте в build.gradle.kts:

repositories {
    mavenLocal()
    // ... другие репозитории
}

dependencies {
    // Core modules
    implementation("com.company.ipcamera:core-common:$VERSION")
    implementation("com.company.ipcamera:core-network:$VERSION")
    implementation("com.company.ipcamera:core-license:$VERSION")
    
    // Shared module
    implementation("com.company.ipcamera:shared:$VERSION")
}

Локальный репозиторий: ~/.m2/repository/com/company/ipcamera/
Версия: $VERSION
EOF
}

# Основная функция
main() {
    section "Сборка и публикация пакетов в локальный Maven репозиторий"
    
    check_dependencies
    clean_build
    build_modules
    publish_to_local
    verify_publication
    show_usage_info
    
    section "Готово!"
    info "Все пакеты успешно собраны и опубликованы в локальный Maven репозиторий"
}

# Запуск
main "$@"

