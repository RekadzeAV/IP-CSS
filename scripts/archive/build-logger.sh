#!/bin/bash
# Логирование сборок IP-CSS
# Функции для структурированного ведения логов

LOG_DIR="build-logs"
CURRENT_BUILD_LOG=""
LOG_FILE=""

# Инициализация логирования
init_logging() {
    local build_id="${1:-$(date +%Y%m%d_%H%M%S)}"
    
    # Создание директории для логов
    mkdir -p "$LOG_DIR"
    
    # Формирование имени файла лога
    LOG_FILE="$LOG_DIR/build_${build_id}.log"
    CURRENT_BUILD_LOG="$build_id"
    
    # Создание заголовка лога
    {
        echo "================================================================"
        echo "                    BUILD LOG - IP-CSS"
        echo "================================================================"
        echo "Build ID:              $build_id"
        echo "Start Time:            $(date '+%Y-%m-%d %H:%M:%S')"
        echo "Host:                  $(hostname)"
        echo "User:                  $(whoami)"
        echo "OS:                    $(uname -a)"
        echo "Working Directory:     $(pwd)"
        echo "================================================================"
        echo ""
    } >> "$LOG_FILE"
    
    echo "[LOGGER] Build log initialized: $LOG_FILE"
}

# Запись сообщения в лог
log_message() {
    local level="$1"
    local message="$2"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Форматированная запись
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
    
    # Вывод в консоль с цветами
    case "$level" in
        "INFO")
            echo -e "\033[0;34m[$timestamp] [$level]\033[0m $message"
            ;;
        "WARN")
            echo -e "\033[1;33m[$timestamp] [$level]\033[0m $message"
            ;;
        "ERROR")
            echo -e "\033[0;31m[$timestamp] [$level]\033[0m $message"
            ;;
        "SUCCESS")
            echo -e "\033[0;32m[$timestamp] [$level]\033[0m $message"
            ;;
        "DEBUG")
            echo -e "\033[0;36m[$timestamp] [$level]\033[0m $message"
            ;;
        *)
            echo "[$timestamp] [$level] $message"
            ;;
    esac
}

# Упрощённые функции логирования
log_info() {
    log_message "INFO" "$1"
}

log_warn() {
    log_message "WARN" "$1"
}

log_error() {
    log_message "ERROR" "$1"
}

log_success() {
    log_message "SUCCESS" "$1"
}

log_debug() {
    log_message "DEBUG" "$1"
}

# Логирование этапа сборки
log_stage_start() {
    local stage="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    {
        echo ""
        echo "================================================================"
        echo "STAGE: $stage"
        echo "Started: $timestamp"
        echo "================================================================"
    } >> "$LOG_FILE"
    
    log_info "=== Stage '$stage' started ==="
}

log_stage_end() {
    local stage="$1"
    local exit_code="$2"
    local end_time=$(date '+%Y-%m-%d %H:%M:%S')
    
    {
        echo ""
        echo "----------------------------------------------------------------"
        echo "Stage: $stage"
        echo "Ended: $end_time"
        echo "Exit Code: $exit_code"
        echo "----------------------------------------------------------------"
        echo ""
    } >> "$LOG_FILE"
    
    if [ "$exit_code" -eq 0 ]; then
        log_success "=== Stage '$stage' completed successfully ==="
    else
        log_error "=== Stage '$stage' failed with exit code $exit_code ==="
    fi
}

# Логирование метрик
log_metrics() {
    local stage="$1"
    
    # CPU usage
    local cpu_usage=0
    if [ "$(uname -s)" = "Linux" ]; then
        cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    elif [ "$(uname -s)" = "Darwin" ]; then
        cpu_usage=$(top -l 1 | grep "load avg" | awk '{print $3}' | tr -d ',')
    fi
    
    # Memory usage
    local mem_used=0
    local mem_total=0
    if [ "$(uname -s)" = "Linux" ]; then
        mem_total=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        mem_used=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
        mem_used=$(( (mem_total - mem_used) / 1024 / 1024 ))
        mem_total=$((mem_total / 1024 / 1024))
    elif [ "$(uname -s)" = "Darwin" ]; then
        mem_total=$(sysctl -n hw.memsize)
        mem_total=$((mem_total / 1024 / 1024))
    fi
    
    # Disk usage
    local disk_used=$(df -h . | tail -1 | awk '{print $3}')
    
    {
        echo "=== Metrics for Stage: $stage ==="
        echo "CPU Usage: ${cpu_usage}%"
        echo "Memory: ${mem_used}GB / ${mem_total}GB"
        echo "Disk Used: ${disk_used}"
        echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
        echo ""
    } >> "$LOG_FILE"
    
    log_debug "Metrics: CPU=${cpu_usage}%, RAM=${mem_used}/${mem_total}GB, Disk=${disk_used}"
}

# Завершение логирования
finalize_logging() {
    local exit_code="${1:-0}"
    local end_time=$(date '+%Y-%m-%d %H:%M:%S')
    
    {
        echo ""
        echo "================================================================"
        echo "                    BUILD SUMMARY"
        echo "================================================================"
        echo "End Time:              $end_time"
        echo "Exit Code:             $exit_code"
        echo ""
        if [ "$exit_code" -eq 0 ]; then
            echo "Status:                  SUCCESS"
        else
            echo "Status:                  FAILED"
        fi
        echo "================================================================"
    } >> "$LOG_FILE"
    
    # Создание JSON-отчёта для анализа
    create_json_report
    
    log_info "Build log finalized: $LOG_FILE"
}

# Создание JSON-отчёта для автоматического анализа
create_json_report() {
    local build_id="${CURRENT_BUILD_LOG:-unknown}"
    local report_file="$LOG_DIR/build_${build_id}.json"
    
    # Сбор метрик
    local exit_code="${1:-0}"
    local end_time=$(date -Iseconds)
    
    # Анализ лога на ошибки
    local error_count=0
    local warn_count=0
    if [ -f "$LOG_FILE" ]; then
        error_count=$(grep -c "\[ERROR\]" "$LOG_FILE" 2>/dev/null || echo 0)
        warn_count=$(grep -c "\[WARN\]" "$LOG_FILE" 2>/dev/null || echo 0)
    fi
    
    # Формирование JSON
    cat > "$report_file" << EOF
{
    "build_id": "$build_id",
    "timestamp": "$end_time",
    "exit_code": $exit_code,
    "status": $([ $exit_code -eq 0 ] && echo "success" || echo "failed"),
    "metrics": {
        "error_count": $error_count,
        "warning_count": $warn_count
    },
    "log_file": "$LOG_FILE"
}
EOF
    
    log_debug "JSON report created: $report_file"
}

# Поиск ошибок в логах
find_errors_in_logs() {
    local search_pattern="${1:-}"
    local days="${2:-7}"
    
    echo "=== Searching for Errors in Last $days Days ==="
    
    if [ -n "$search_pattern" ]; then
        echo "Search Pattern: $search_pattern"
    fi
    
    echo ""
    
    # Поиск в логах за последние N дней
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec grep -l "\[ERROR\]" {} \; 2>/dev/null | while read log_file; do
        echo "--- $log_file ---"
        if [ -n "$search_pattern" ]; then
            grep -i "\[ERROR\].*${search_pattern}" "$log_file" | tail -20
        else
            grep "\[ERROR\]" "$log_file" | tail -10
        fi
        echo ""
    done
}

# Статистика сборок
show_build_stats() {
    local days="${1:-30}"
    
    echo "=== Build Statistics (Last $days Days) ==="
    echo ""
    
    local total_builds=$(find "$LOG_DIR" -name "*.log" -mtime -${days} | wc -l)
    local successful_builds=$(find "$LOG_DIR" -name "*.json" -mtime -${days} -exec grep -l '"status": "success"' {} \; | wc -l)
    local failed_builds=$((total_builds - successful_builds))
    
    echo "Total Builds:      $total_builds"
    echo "Successful:        $successful_builds"
    echo "Failed:            $failed_builds"
    
    if [ $total_builds -gt 0 ]; then
        local success_rate=$((successful_builds * 100 / total_builds))
        echo "Success Rate:      ${success_rate}%"
    fi
    
    echo ""
    
    # Последние 5 сборок
    echo "Last 5 Builds:"
    ls -t "$LOG_DIR"/*.json 2>/dev/null | head -5 | while read json_file; do
        local build_id=$(basename "$json_file" .json | sed 's/build_//')
        local status=$(grep -o '"status": "[^"]*"' "$json_file" | cut -d'"' -f4)
        local timestamp=$(grep -o '"timestamp": "[^"]*"' "$json_file" | cut -d'"' -f4)
        echo "  $build_id | $status | $timestamp"
    done
}

# Экспорт логов для анализа
export_logs() {
    local output_dir="${1:-./logs-export}"
    local days="${2:-7}"
    
    mkdir -p "$output_dir"
    
    # Копирование всех логов за последние N дней
    find "$LOG_DIR" -name "*.log" -mtime -${days} -exec cp {} "$output_dir/" \;
    find "$LOG_DIR" -name "*.json" -mtime -${days} -exec cp {} "$output_dir/" \;
    
    echo "[LOGGER] Logs exported to: $output_dir"
    echo "[LOGGER] $(find "$output_dir" -type f | wc -l) files exported"
}
