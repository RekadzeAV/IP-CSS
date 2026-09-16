#!/bin/bash
# Анализатор логов сборок IP-CSS

LOG_DIR="build-logs"

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

show_stats() {
    local days="${1:-30}"
    
    echo "=== Build Statistics (Last $days Days) ==="
    echo ""
    
    local total_builds=$(find "$LOG_DIR" -name "*.log" -mtime -${days} 2>/dev/null | wc -l)
    local successful_builds=$(find "$LOG_DIR" -name "*.json" -mtime -${days} 2>/dev/null -exec grep -l '"status": "success"' {} \; | wc -l)
    local failed_builds=$((total_builds - successful_builds))
    
    echo "Total Builds:      $total_builds"
    echo "Successful:        $successful_builds"
    echo "Failed:            $failed_builds"
    
    if [ $total_builds -gt 0 ]; then
        local success_rate=$((successful_builds * 100 / total_builds))
        echo "Success Rate:      ${success_rate}%"
    fi
    
    echo ""
    
    echo "Most Common Errors:"
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep "\[ERROR\]" {} \; 2>/dev/null | \
        sed 's/.*\[ERROR\] //' | sort | uniq -c | sort -rn | head -10
}

search_errors() {
    local pattern="$1"
    local days="${2:-7}"
    
    echo "=== Searching for Errors (Last $days Days) ==="
    
    if [ -n "$pattern" ]; then
        echo "Search Pattern: $pattern"
        echo ""
    fi
    
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep -l "\[ERROR\]" {} \; 2>/dev/null | while read log_file; do
        local build_id=$(basename "$log_file" .log | sed 's/build_//')
        local build_date=$(stat -c %y "$log_file" 2>/dev/null | cut -d' ' -f1)
        
        echo "--- $build_id ($build_date) ---"
        
        if [ -n "$pattern" ]; then
            grep -i "\[ERROR\].*${pattern}" "$log_file" | tail -20
        else
            grep "\[ERROR\]" "$log_file" | tail -10
        fi
        
        echo ""
    done
}

show_timeline() {
    local days="${1:-7}"
    
    echo "=== Build Timeline (Last $days Days) ==="
    echo ""
    
    ls -t "$LOG_DIR"/*.log 2>/dev/null | head -20 | while read log_file; do
        local build_id=$(basename "$log_file" .log | sed 's/build_//')
        local build_time=$(stat -c %y "$log_file" 2>/dev/null | cut -d' ' -f1)
        local status="?"
        
        if grep -q "Status:.*SUCCESS" "$log_file" 2>/dev/null; then
            status="SUCCESS"
        elif grep -q "Status:.*FAILED" "$log_file" 2>/dev/null; then
            status="FAILED"
        fi
        
        local duration=$(grep "Duration:" "$log_file" 2>/dev/null | sed 's/.*Duration: *//' | head -1)
        
        printf "%-20s | %s | %s\n" "$build_id" "$status" "$duration"
    done
}

show_summary() {
    echo "=== IP-CSS Build Log Summary ==="
    echo ""
    
    local total_logs=$(find "$LOG_DIR" -name "*.log" 2>/dev/null | wc -l)
    local total_json=$(find "$LOG_DIR" -name "*.json" 2>/dev/null | wc -l)
    
    echo "Total Logs:        $total_logs"
    echo "JSON Reports:      $total_json"
    
    echo ""
    echo "Recent Activity:"
    show_timeline 7
    
    echo ""
    echo "Use '$0 stats' for detailed statistics"
    echo "Use '$0 errors' to search for errors"
}

show_latest() {
    local latest=$(ls -t "$LOG_DIR"/*.log 2>/dev/null | head -1)
    
    if [ -z "$latest" ]; then
        echo "No logs found in $LOG_DIR"
        exit 1
    fi
    
    echo "=== Latest Build Log ==="
    echo "File: $latest"
    echo ""
    cat "$latest"
}

clean_logs() {
    local days="${1:-30}"
    
    echo "Cleaning logs older than $days days..."
    
    local count=$(find "$LOG_DIR" -name "*.log" -mtime -${days} | wc -l)
    find "$LOG_DIR" -name "*.log" -mtime -${days} -delete
    find "$LOG_DIR" -name "*.json" -mtime -${days} -delete
    
    echo "Removed $count old log files"
}

export_logs() {
    local days="${1:-7}"
    local output_dir="./logs-export-$(date +%Y%m%d_%H%M%S)"
    
    mkdir -p "$output_dir"
    
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec cp {} "$output_dir/" \;
    find "$LOG_DIR" -name "*.json" -mtime -${days} -exec cp {} "$output_dir/" \;
    
    local count=$(find "$output_dir" -type f | wc -l)
    echo "Exported $count files to: $output_dir"
}

# =============================================================================
# MAIN
# =============================================================================

main() {
    if [ ! -d "$LOG_DIR" ]; then
        echo "Log directory '$LOG_DIR' not found"
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
            echo "Warnings functionality coming soon"
            ;;
        timeline)
            show_timeline "$1"
            ;;
        compare)
            echo "Compare functionality coming soon"
            ;;
        slow)
            echo "Slow builds functionality coming soon"
            ;;
        failed)
            echo "Failed builds functionality coming soon"
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
            echo "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
