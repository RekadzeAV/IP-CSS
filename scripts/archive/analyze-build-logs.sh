#!/bin/bash
# Анализ логов сборок IP-CSS
# Поиск ошибок, статистика, тренды

LOG_DIR="build-logs"

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

# Показ справки
show_help() {
    cat << EOF
Анализатор логов сборок IP-CSS

Использование: $0 [ОПЦИЯ] [ПАРАМЕТР]

Опции:
  stats [ДНИ]                 Показать статистику за последние ДНИ (по умолчанию 30)
  errors [ПАТТЕРН] [ДНИ]      Поиск ошибок по паттерну за последние ДНИ
  warnings [ДНИ]              Показать все предупреждения за последние ДНИ
  timeline                    Показать временную шкалу сборок
  compare BUILD1 BUILD2       Сравнить две сборки по ID
  slow [ДНИ]                  Показать самые медленные сборки
  failed [ДНИ]                Показать неудачные сборки
  export [ДНИ]                Экспорт логов (по умолчанию 7 дней)
  summary                     Краткая сводка всех сборок
  latest                      Последняя сборка (полный лог)
  clean [ДНИ]                 Удалить логи старше ДНИ дней

Примеры:
  $0 stats 7                  Статистика за неделю
  $0 errors "OutOfMemory"     Поиск ошибок памяти
  $0 compare 20250115_120000 20250114_100000
  $0 slow 30                  Топ медленных сборок за месяц

EOF
}

# Статистика сборок
show_stats() {
    local days="${1:-30}"
    
    echo -e "${CYAN}=== Build Statistics (Last $days Days) ===${NC}"
    echo ""
    
    local total_builds=$(find "$LOG_DIR" -name "*.log" -mtime -${days} 2>/dev/null | wc -l)
    local successful_builds=$(find "$LOG_DIR" -name "*.json" -mtime -${days} 2>/dev/null -exec grep -l '"status": "success"' {} \; | wc -l)
    local failed_builds=$((total_builds - successful_builds))
    
    echo "Total Builds:      $total_builds"
    echo "Successful:        $successful_builds"
    echo "Failed:            $failed_builds"
    
    if [ $total_builds -gt 0 ]; then
        local success_rate=$((successful_builds * 100 / total_builds))
        echo -e "Success Rate:      ${GREEN}${success_rate}%${NC}"
    fi
    
    echo ""
    
    # Среднее время сборки
    echo "Build Duration Stats:"
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep -H "Duration:" {} \; 2>/dev/null | \
        sed 's/.*Duration: *//' | sort | uniq -c | sort -rn | head -5
    
    echo ""
    
    # Частые ошибки
    echo "Most Common Errors:"
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep "\[ERROR\]" {} \; 2>/dev/null | \
        sed 's/.*\[ERROR\] //' | sort | uniq -c | sort -rn | head -10
    
    echo ""
}

# Поиск ошибок
search_errors() {
    local pattern="$1"
    local days="${2:-7}"
    
    echo -e "${CYAN}=== Searching for Errors (Last $days Days) ===${NC}"
    
    if [ -n "$pattern" ]; then
        echo "Search Pattern: $pattern"
        echo ""
    fi
    
    local found=0
    
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep -l "\[ERROR\]" {} \; 2>/dev/null | while read log_file; do
        local build_id=$(basename "$log_file" .log | sed 's/build_//')
        local build_date=$(stat -c %y "$log_file" 2>/dev/null | cut -d' ' -f1)
        
        echo -e "${YELLOW}--- $build_id ($build_date) ---${NC}"
        
        if [ -n "$pattern" ]; then
            grep -i "\[ERROR\].*${pattern}" "$log_file" | tail -20
        else
            grep "\[ERROR\]" "$log_file" | tail -10
        fi
        
        echo ""
        found=1
    done
    
    if [ $found -eq 0 ]; then
        log_success "No errors found in last $days days"
    fi
}

# Показать предупреждения
show_warnings() {
    local days="${1:-7}"
    
    echo -e "${CYAN}=== Warnings (Last $days Days) ===${NC}"
    echo ""
    
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep -l "\[WARN\]" {} \; 2>/dev/null | head -10 | while read log_file; do
        local build_id=$(basename "$log_file" .log | sed 's/build_//')
        
        echo -e "${YELLOW}--- $build_id ---${NC}"
        grep "\[WARN\]" "$log_file" | tail -5
        echo ""
    done
}

# Временная шкала сборок
show_timeline() {
    local days="${1:-7}"
    
    echo -e "${CYAN}=== Build Timeline (Last $days Days) ===${NC}"
    echo ""
    
    ls -t "$LOG_DIR"/*.log 2>/dev/null | head -20 | while read log_file; do
        local build_id=$(basename "$log_file" .log | sed 's/build_//')
        local build_time=$(stat -c %y "$log_file" 2>/dev/null | cut -d'.' -f1)
        local status="?"
        
        # Определение статуса
        if grep -q "Status:.*SUCCESS" "$log_file" 2>/dev/null; then
            status="${GREEN}SUCCESS${NC}"
        elif grep -q "Status:.*FAILED" "$log_file" 2>/dev/null; then
            status="${RED}FAILED${NC}"
        fi
        
        local duration=$(grep "Duration:" "$log_file" 2>/dev/null | sed 's/.*Duration: *//' | head -1)
        
        printf "%-20s | %s | %s\n" "$build_id" "$status" "$duration"
    done
}

# Сравнение сборок
compare_builds() {
    local build1="$1"
    local build2="$2"
    
    if [ -z "$build1" ] || [ -z "$build2" ]; then
        log_error "Usage: $0 compare BUILD1 BUILD2"
        exit 1
    fi
    
    local file1="$LOG_DIR/build_${build1}.log"
    local file2="$LOG_DIR/build_${build2}.log"
    
    if [ ! -f "$file1" ]; then
        log_error "Build $build1 not found: $file1"
        exit 1
    fi
    
    if [ ! -f "$file2" ]; then
        log_error "Build $build2 not found: $file2"
        exit 1
    fi
    
    echo -e "${CYAN}=== Comparing Builds ===${NC}"
    echo ""
    
    echo "Build $build1:"
    echo "  Status:    $(grep 'Status:' "$file1" | sed 's/.*: *//')"
    echo "  Duration:  $(grep 'Duration:' "$file1" | sed 's/.*: *//')"
    echo "  Errors:    $(grep -c '\[ERROR\]' "$file1")"
    echo "  Warnings:  $(grep -c '\[WARN\]' "$file1")"
    echo ""
    
    echo "Build $build2:"
    echo "  Status:    $(grep 'Status:' "$file2" | sed 's/.*: *//')"
    echo "  Duration:  $(grep 'Duration:' "$file2" | sed 's/.*: *//')"
    echo "  Errors:    $(grep -c '\[ERROR\]' "$file2")"
    echo "  Warnings:  $(grep -c '\[WARN\]' "$file2")"
    echo ""
    
    # Различия в метриках
    echo "Differences:"
    diff <(grep -E "(CPU|RAM|SAFE_JOBS):" "$file1" | sort) <(grep -E "(CPU|RAM|SAFE_JOBS):" "$file2" | sort) | head -20
}

# Показать медленные сборки
show_slow_builds() {
    local days="${1:-30}"
    
    echo -e "${CYAN}=== Slowest Builds (Last $days Days) ===${NC}"
    echo ""
    
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep -H "Duration:" {} \; 2>/dev/null | \
        sed 's/.*\///; s/\.log:Duration: *//' | \
        awk -F'm' '{
            min = $1;
            sec = $2;
            gsub(/s/, "", sec);
            total = min * 60 + sec;
            print total, $0
        }' | \
        sort -rn | \
        head -10 | \
        awk '{
            $1 = "";
            printf "%-10s %s\n", $1, substr($0, 2)
        }' | \
        nl
}

# Показать неудачные сборки
show_failed_builds() {
    local days="${1:-7}"
    
    echo -e "${RED}=== Failed Builds (Last $days Days) ===${NC}"
    echo ""
    
    find "$LOG_DIR" -name "*.json" -mtime -${days} -exec grep -l '"status": "failed"' {} \; 2>/dev/null | while read json_file; do
        local build_id=$(basename "$json_file" .json | sed 's/build_//')
        local timestamp=$(grep -o '"timestamp": "[^"]*"' "$json_file" | cut -d'"' -f4)
        local error_count=$(grep -o '"error_count": [0-9]*' "$json_file" | grep -o '[0-9]*')
        
        echo -e "${RED}Build: $build_id${NC}"
        echo "  Time: $timestamp"
        echo "  Errors: $error_count"
        
        # Показать первую ошибку
        local log_file="$LOG_DIR/build_${build_id}.log"
        if [ -f "$log_file" ]; then
            echo "  First error:"
            grep "\[ERROR\]" "$log_file" | head -1 | sed 's/^/    /'
        fi
        echo ""
    done
}

# Экспорт логов
export_logs() {
    local days="${1:-7}"
    local output_dir="./logs-export-$(date +%Y%m%d_%H%M%S)"
    
    mkdir -p "$output_dir"
    
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec cp {} "$output_dir/" \;
    find "$LOG_DIR" -name "*.json" -mtime -${days} -exec cp {} "$output_dir/" \;
    
    local count=$(find "$output_dir" -type f | wc -l)
    log_success "Exported $count files to: $output_dir"
}

# Краткая сводка
show_summary() {
    echo -e "${CYAN}=== IP-CSS Build Log Summary ===${NC}"
    echo ""
    
    local total_logs=$(find "$LOG_DIR" -name "*.log" 2>/dev/null | wc -l)
    local total_json=$(find "$LOG_DIR" -name "*.json" 2>/dev/null | wc -l)
    local oldest=$(ls -t "$LOG_DIR"/*.log 2>/dev/null | tail -1)
    local newest=$(ls -t "$LOG_DIR"/*.log 2>/dev/null | head -1)
    
    echo "Total Logs:        $total_logs"
    echo "JSON Reports:      $total_json"
    
    if [ -n "$oldest" ]; then
        echo "Oldest Build:      $(basename "$oldest" .log)"
    fi
    
    if [ -n "$newest" ]; then
        echo "Newest Build:      $(basename "$newest" .log)"
    fi
    
    echo ""
    echo "Recent Activity:"
    show_timeline 7
    
    echo ""
    echo "Use '$0 stats' for detailed statistics"
    echo "Use '$0 errors' to search for errors"
}

# Показать последнюю сборку
show_latest() {
    local latest=$(ls -t "$LOG_DIR"/*.log 2>/dev/null | head -1)
    
    if [ -z "$latest" ]; then
        log_error "No logs found in $LOG_DIR"
        exit 1
    fi
    
    echo -e "${CYAN}=== Latest Build Log ===${NC}"
    echo "File: $latest"
    echo ""
    cat "$latest"
}

# Очистка старых логов
clean_logs() {
    local days="${1:-30}"
    
    log_info "Cleaning logs older than $days days..."
    
    local count=$(find "$LOG_DIR" -name "*.log" -mtime -${days} | wc -l)
    find "$LOG_DIR" -name "*.log" -mtime -${days} -delete
    find "$LOG_DIR" -name "*.json" -mtime -${days} -delete
    
    log_success "Removed $count old log files"
}

# =============================================================================
# MAIN
# =============================================================================

main() {
    # Проверка директории логов
    if [ ! -d "$LOG_DIR" ]; then
        log_warn "Log directory '$LOG_DIR' not found"
        echo "Run a build first to create logs"
        exit 0
    fi
    
    local command="${1:-summary}"
    shift || true
    
    case "$command" in
        stats)
            show_stats "$1"
            ;;
        errors)
            search_errors "$1" "$2"
            ;;
        warnings)
            show_warnings "$1"
            ;;
        timeline)
            show_timeline "$1"
            ;;
        compare)
            compare_builds "$1" "$2"
            ;;
        slow)
            show_slow_builds "$1"
            ;;
        failed)
            show_failed_builds "$1"
            ;;
        export)
            export_logs "$1"
            ;;
        summary)
            show_summary
            ;;
        latest)
            show_latest
            ;;
        clean)
            clean_logs "$1"
            ;;
        -h|--help|help)
            show_help
            ;;
        *)
            log_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
