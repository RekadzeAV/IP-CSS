#!/bin/bash
# Скрипт мониторинга производительности FFmpeg
# Отслеживает использование CPU, памяти и производительность записи

DURATION=${1:-60}  # Длительность мониторинга в секундах
OUTPUT_FILE=${2:-"ffmpeg-performance-report.txt"}

# Цвета
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка наличия FFmpeg
if ! command -v ffmpeg &> /dev/null; then
    log_error "FFmpeg не установлен. Установите FFmpeg перед запуском мониторинга."
    log_info "Используйте: ./scripts/install-ffmpeg.sh"
    exit 1
fi

log_info "Мониторинг производительности FFmpeg"
log_info "Длительность: $DURATION секунд"
log_info "Отчет будет сохранен в: $OUTPUT_FILE"

# Создаем отчет
{
    echo "Отчет о производительности FFmpeg"
    echo "=================================="
    echo "Дата: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Длительность мониторинга: $DURATION секунд"
    echo ""

    # Информация о системе
    log_info "Сбор информации о системе..."
    echo "Информация о системе:"
    echo "---------------------"
    echo "Процессор: $(lscpu | grep 'Model name' | cut -d: -f2 | xargs)"
    echo "Ядра: $(nproc)"
    echo "Память: $(free -h | grep Mem | awk '{print $2}')"
    echo ""

    # Информация о FFmpeg
    log_info "Сбор информации о FFmpeg..."
    echo "Информация о FFmpeg:"
    echo "--------------------"
    ffmpeg -version 2>&1 | head -n 1
    echo ""

    # Проверка hardware acceleration
    log_info "Проверка hardware acceleration..."
    echo "Hardware acceleration:"
    ENCODERS=$(ffmpeg -encoders 2>&1)

    if echo "$ENCODERS" | grep -q "h264_nvenc"; then
        echo "  ✓ NVIDIA NVENC (h264_nvenc) доступен"
    fi
    if echo "$ENCODERS" | grep -q "h264_qsv"; then
        echo "  ✓ Intel Quick Sync (h264_qsv) доступен"
    fi
    if echo "$ENCODERS" | grep -q "h264_videotoolbox"; then
        echo "  ✓ VideoToolbox (h264_videotoolbox) доступен"
    fi
    if echo "$ENCODERS" | grep -q "h264_vaapi"; then
        echo "  ✓ VAAPI (h264_vaapi) доступен"
    fi

    if ! echo "$ENCODERS" | grep -qE "(nvenc|qsv|videotoolbox|vaapi)"; then
        echo "  ⚠ Hardware acceleration НЕ доступен (используется программное кодирование)"
    fi
    echo ""

    # Проверка доступных кодеков
    log_info "Проверка доступных кодеков..."
    echo "Доступные кодеки:"
    echo "-----------------"
    CODECS=$(ffmpeg -codecs 2>&1)

    echo -n "Аудио: "
    AUDIO_CODECS=()
    echo "$CODECS" | grep -q "DEA.*aac" && AUDIO_CODECS+=("AAC")
    echo "$CODECS" | grep -q "DEA.*mp3" && AUDIO_CODECS+=("MP3")
    echo "$CODECS" | grep -q "DEA.*pcm" && AUDIO_CODECS+=("PCM")
    echo "$CODECS" | grep -q "DEA.*g711" && AUDIO_CODECS+=("G.711")
    echo "${AUDIO_CODECS[*]:-Не найдены}"

    echo -n "Видео: "
    VIDEO_CODECS=()
    echo "$CODECS" | grep -q "DEV.*h264" && VIDEO_CODECS+=("H.264")
    echo "$CODECS" | grep -q "DEV.*h265\|DEV.*hevc" && VIDEO_CODECS+=("H.265/HEVC")
    echo "${VIDEO_CODECS[*]:-Не найдены}"
    echo ""

    # Мониторинг производительности
    log_info "Мониторинг процессов FFmpeg..."
    echo "Результаты мониторинга:"
    echo "----------------------"

    SAMPLES=0
    TOTAL_CPU=0
    MAX_CPU=0
    TOTAL_MEM=0
    MAX_MEM=0

    for ((i=0; i<DURATION; i++)); do
        FFMPEG_PIDS=$(pgrep -f ffmpeg)

        if [ -n "$FFMPEG_PIDS" ]; then
            for PID in $FFMPEG_PIDS; do
                if ps -p $PID > /dev/null 2>&1; then
                    CPU=$(ps -p $PID -o %cpu= | tr -d ' ')
                    MEM=$(ps -p $PID -o rss= | awk '{print $1/1024}')

                    TOTAL_CPU=$(echo "$TOTAL_CPU + $CPU" | bc)
                    TOTAL_MEM=$(echo "$TOTAL_MEM + $MEM" | bc)

                    if (( $(echo "$CPU > $MAX_CPU" | bc -l) )); then
                        MAX_CPU=$CPU
                    fi
                    if (( $(echo "$MEM > $MAX_MEM" | bc -l) )); then
                        MAX_MEM=$MEM
                    fi

                    SAMPLES=$((SAMPLES + 1))
                fi
            done
        fi

        sleep 1

        if [ $((i % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""

    if [ $SAMPLES -gt 0 ]; then
        AVG_CPU=$(echo "scale=2; $TOTAL_CPU / $SAMPLES" | bc)
        AVG_MEM=$(echo "scale=2; $TOTAL_MEM / $SAMPLES" | bc)

        echo "Количество образцов: $SAMPLES"
        echo "Средняя нагрузка CPU: ${AVG_CPU}%"
        echo "Максимальная нагрузка CPU: ${MAX_CPU}%"
        echo "Среднее использование памяти: ${AVG_MEM} MB"
        echo "Максимальное использование памяти: ${MAX_MEM} MB"
        echo ""

        # Рекомендации
        echo "Рекомендации:"
        echo "------------"

        if (( $(echo "$MAX_CPU > 80" | bc -l) )); then
            echo "⚠ ВНИМАНИЕ: Высокая нагрузка на CPU (${MAX_CPU}%)"
            echo "  - Рассмотрите использование hardware acceleration"
            echo "  - Снизьте качество записи (LOW или MEDIUM)"
            echo "  - Уменьшите количество одновременных записей"
        elif (( $(echo "$MAX_CPU > 50" | bc -l) )); then
            echo "ℹ Умеренная нагрузка на CPU (${MAX_CPU}%)"
            echo "  - Система работает нормально"
        else
            echo "✓ Низкая нагрузка на CPU (${MAX_CPU}%)"
            echo "  - Система работает оптимально"
        fi

        if ! echo "$ENCODERS" | grep -qE "(nvenc|qsv|videotoolbox|vaapi)" && (( $(echo "$MAX_CPU > 50" | bc -l) )); then
            echo ""
            echo "💡 СОВЕТ: Hardware acceleration не используется"
            echo "  - Установите драйверы GPU для снижения нагрузки на CPU"
        fi
    else
        echo "Активные процессы FFmpeg не обнаружены во время мониторинга."
        echo "Запустите запись и повторите мониторинг."
    fi

    echo ""
    echo "Конец отчета"
    echo "============"

} > "$OUTPUT_FILE"

log_info "Отчет сохранен в: $OUTPUT_FILE"

# Вывод краткой информации
echo ""
log_info "Краткая сводка:"
cat "$OUTPUT_FILE"

