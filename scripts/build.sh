#!/bin/bash
# Универсальный скрипт-обёртка для сборки IP-CSS
# Автоматически выбирает bash или PowerShell версию

set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Определение ОС
detect_os() {
    if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]] || [[ "$(uname -s)" == "MINGW"* ]] || [[ "$(uname -s)" == "MYSYS"* ]]; then
        echo "windows"
    elif [[ "$(uname -s)" == "Linux" ]]; then
        echo "linux"
    elif [[ "$(uname -s)" == "Darwin" ]]; then
        echo "macos"
    else
        echo "unknown"
    fi
}

# Проверка зависимостей для запуска скрипта
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing=0
    
    # Проверка Git
    if ! command -v git &> /dev/null; then
        log_error "Git is required but not installed"
        missing=1
    fi
    
    # Проверка bash 4+ или PowerShell
    local OS=$(detect_os)
    if [ "$OS" = "windows" ]; then
        if ! command -v powershell &> /dev/null; then
            log_error "PowerShell is required on Windows"
            missing=1
        fi
    else
        local BASH_VERSION=$(bash --version | head -1 | cut -d' ' -f1 | cut -d'.' -f1)
        if [ "$BASH_VERSION" -lt 4 ]; then
            log_error "Bash 4+ is required (found $BASH_VERSION)"
            missing=1
        fi
    fi
    
    return $missing
}

# Запуск PowerShell версии на Windows
run_windows() {
    log_info "Detected Windows - using PowerShell pipeline"
    
    local BUILD_MODE="${1:-full}"
    
    cd "$PROJECT_ROOT"
    
    # Проверка наличия скрипта
    if [ ! -f "$SCRIPT_DIR/build-pipeline.ps1" ]; then
        log_error "build-pipeline.ps1 not found"
        exit 1
    fi
    
    # Запуск PowerShell скрипта
    powershell -ExecutionPolicy Bypass -File "$SCRIPT_DIR/build-pipeline.ps1" -BuildMode "$BUILD_MODE"
    return $?
}

# Запуск bash версии на Linux/macOS
run_unix() {
    log_info "Detected $(uname -s) - using Bash pipeline"
    
    local BUILD_MODE="${1:-full}"
    
    cd "$PROJECT_ROOT"
    
    # Проверка наличия скрипта
    if [ ! -f "$SCRIPT_DIR/build-pipeline.sh" ]; then
        log_error "build-pipeline.sh not found"
        exit 1
    fi
    
    # Запуск bash скрипта
    bash "$SCRIPT_DIR/build-pipeline.sh" "$BUILD_MODE"
    return $?
}

# Вывод справки
show_help() {
    cat << EOF
IP-CSS Build Script

Usage: $0 [OPTIONS] [MODE]

Modes:
  full      Full build (all components) - default
  quick     Quick build (tests only)
  native    Native C++ libraries only
  kotlin    Kotlin Multiplatform modules only
  web       Web interface only
  clean     Clean caches before build

Options:
  -h, --help    Show this help message
  -v, --verbose Verbose output
  --native      Set BUILD_NATIVE=true
  --kotlin      Set BUILD_KOTLIN=true
  --web         Set BUILD_WEB=true
  --docker      Set BUILD_DOCKER=true

Examples:
  $0                    # Full build
  $0 quick              # Quick build
  $0 --native native    # Native only
  $0 --web web          # Web only
  $0 --docker full      # Full build with Docker

Environment Variables:
  BUILD_NATIVE=true     Enable native C++ build
  BUILD_KOTLIN=true     Enable Kotlin build
  BUILD_WEB=true        Enable web build
  BUILD_DOCKER=false    Enable Docker build

EOF
}

# Парсинг аргументов
parse_args() {
    BUILD_MODE="full"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                set -x
                shift
                ;;
            --native)
                export BUILD_NATIVE=true
                shift
                ;;
            --kotlin)
                export BUILD_KOTLIN=true
                shift
                ;;
            --web)
                export BUILD_WEB=true
                shift
                ;;
            --docker)
                export BUILD_DOCKER=true
                shift
                ;;
            full|quick|native|kotlin|web|clean)
                BUILD_MODE="$1"
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    echo "$BUILD_MODE"
}

# =============================================================================
# MAIN
# =============================================================================

main() {
    # Парсинг аргументов
    BUILD_MODE=$(parse_args "$@")
    
    echo "================================================================"
    echo "         IP-CSS Build Script"
    echo "================================================================"
    echo "Build Mode: $BUILD_MODE"
    echo "Project Root: $PROJECT_ROOT"
    echo ""
    
    # Проверка зависимостей
    if ! check_prerequisites; then
        log_error "Prerequisites check failed"
        exit 1
    fi
    
    # Определение ОС и запуск соответствующего скрипта
    OS=$(detect_os)
    
    case $OS in
        windows)
            run_windows "$BUILD_MODE"
            ;;
        linux|macos)
            run_unix "$BUILD_MODE"
            ;;
        *)
            log_error "Unsupported OS: $OS"
            exit 1
            ;;
    esac
    
    exit $?
}

main "$@"
