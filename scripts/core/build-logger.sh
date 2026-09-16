#!/bin/bash
# Логирование сборок IP-CSS
# Библиотека функций для структурированного ведения логов

LOG_DIR="build-logs"
CURRENT_BUILD_LOG=""
LOG_FILE=""

# Инициализация логирования
init_logging() {
    local build_id="${1:-$(date +%Y%m%d_%H%M%S)}"
    
    mkdir -p "$LOG_DIR"
    LOG_FILE="$LOG_DIR/build_${build_id}.log"
    CURRENT_BUILD_LOG="$build_id"
    
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
    
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
    
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

log_info() { log_message "INFO" "$1"; }
log_warn() { log_message "WARN" "$1"; }
log_error() { log_message "ERROR" "$1"; }
log_success() { log_message "SUCCESS" "$1"; }
log_debug() { log_message "DEBUG" "$1"; }

# Логирование этапа
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
    local cpu_usage=0
    local mem_used=0
    local mem_total=0
    
    if [ "$(uname -s)" = "Linux" ]; then
        cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
        mem_total=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        mem_used=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
        mem_used=$(( (mem_total - mem_used) / 1024 / 1024 ))
        mem_total=$((mem_total / 1024 / 1024))
    elif [ "$(uname -s)" = "Darwin" ]; then
        cpu_usage=$(top -l 1 | grep "load avg" | awk '{print $3}' | tr -d ',')
        mem_total=$(sysctl -n hw.memsize)
        mem_total=$((mem_total / 1024 / 1024))
    fi
    
    {
        echo "=== Metrics for Stage: $stage ==="
        echo "CPU Usage: ${cpu_usage}%"
        echo "Memory: ${mem_used}GB / ${mem_total}GB"
        echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
        echo ""
    } >> "$LOG_FILE"
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
        echo "Status:                $([ $exit_code -eq 0 ] && echo "SUCCESS" || echo "FAILED")"
        echo "================================================================"
    } >> "$LOG_FILE"
    
    create_json_report "$exit_code"
    log_info "Build log finalized: $LOG_FILE"
}

# Создание JSON-отчёта
create_json_report() {
    local build_id="${CURRENT_BUILD_LOG:-unknown}"
    local exit_code="${1:-0}"
    local report_file="$LOG_DIR/build_${build_id}.json"
    
    local error_count=0
    local warn_count=0
    if [ -f "$LOG_FILE" ]; then
        error_count=$(grep -c "\[ERROR\]" "$LOG_FILE" 2>/dev/null || echo 0)
        warn_count=$(grep -c "\[WARN\]" "$LOG_FILE" 2>/dev/null || echo 0)
    fi
    
    cat > "$report_file" << EOF
{
    "build_id": "$build_id",
    "timestamp": "$(date -Iseconds)",
    "exit_code": $exit_code,
    "status": $([ $exit_code -eq 0 ] && echo "success" || echo "failed"),
    "metrics": {
        "error_count": $error_count,
        "warning_count": $warn_count
    },
    "log_file": "$LOG_FILE"
}
EOF
}
