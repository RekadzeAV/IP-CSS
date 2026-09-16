#!/bin/bash

# RTSP Performance Monitoring Script
# Monitors performance metrics during RTSP streaming
# Usage: ./monitor-rtsp-performance.sh [--camera hikvision] [--duration 3600]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
CAMERA=${1:-"hikvision"}
DURATION=${2:-3600}
LOG_DIR="build/performance-monitor"
RESULTS_FILE="$LOG_DIR/performance.json"
INTERVAL=10  # Monitor every 10 seconds

# Create log directory
mkdir -p "$LOG_DIR"

echo "=========================================="
echo "RTSP Performance Monitor"
echo "=========================================="
echo "Camera: $CAMERA"
echo "Duration: ${DURATION}s"
echo "Monitor Interval: ${INTERVAL}s"
echo "Log Directory: $LOG_DIR"
echo ""

# Camera URL
case $CAMERA in
    hikvision)
        URL="rtsp://admin:password123@192.168.1.101:554/Streaming/Channels/101"
        ;;
    dahua)
        URL="rtsp://admin:password123@192.168.1.102:554/cam/realmonitor?channel=1&substream=0"
        ;;
    axis)
        URL="rtsp://admin:password123@192.168.1.103:554/axis-media/media.amp?videocodec=h264"
        ;;
    *)
        echo -e "${RED}Unknown camera: $CAMERA${NC}"
        exit 1
        ;;
esac

echo "RTSP URL: $URL"
echo ""

# Initialize metrics
START_TIME=$(date +%s)
CPU_SAMPLES=""
MEMORY_SAMPLES=""
FRAME_COUNT=0
ERROR_COUNT=0

# Function to get process info
get_process_stats() {
    local pid=$1
    
    if [ -d "/proc/$pid" ]; then
        # Get CPU usage
        local cpu=$(ps -p $pid -o %cpu= 2>/dev/null || echo "0")
        
        # Get memory usage (RSS in MB)
        local mem_kb=$(ps -p $pid -o rss= 2>/dev/null || echo "0")
        local mem_mb=$(echo "scale=2; $mem_kb / 1024" | bc)
        
        echo "$cpu $mem_mb"
    else
        echo "0 0"
    fi
}

# Function to start stream
start_stream() {
    echo -e "${BLUE}Starting RTSP stream...${NC}"
    
    # Start ffmpeg in background (simulates RTSP client)
    ffmpeg -y -rtsp_transport tcp -i "$URL" -f null - \
        -loglevel info \
        2>"$LOG_DIR/ffmpeg.log" &
    
    local ffmpeg_pid=$!
    echo "FFmpeg PID: $ffmpeg_pid"
    echo ""
    
    return $ffmpeg_pid
}

# Function to monitor performance
monitor_performance() {
    local ffmpeg_pid=$1
    local duration=$2
    local interval=$3
    
    echo -e "${BLUE}Starting performance monitoring...${NC}"
    echo ""
    
    local elapsed=0
    local sample_count=0
    
    while [ $elapsed -lt $duration ]; do
        sleep $interval
        
        # Check if ffmpeg is still running
        if ! kill -0 $ffmpeg_pid 2>/dev/null; then
            echo -e "${RED}✗ Stream process exited after ${elapsed}s${NC}"
            ERROR_COUNT=$((ERROR_COUNT + 1))
            break
        fi
        
        # Get process stats
        local stats=$(get_process_stats $ffmpeg_pid)
        local cpu=$(echo $stats | cut -d' ' -f1)
        local mem=$(echo $stats | cut -d' ' -f2)
        
        # Store samples
        CPU_SAMPLES="$CPU_SAMPLES$cpu,"
        MEMORY_SAMPLES="$MEMORY_SAMPLES$mem,"
        sample_count=$((sample_count + 1))
        
        # Calculate elapsed time
        elapsed=$(($(date +%s) - START_TIME))
        
        # Progress update
        echo -e "Progress: ${elapsed}s / ${duration}s | CPU: ${cpu}% | Memory: ${mem}MB"
    done
    
    # Kill ffmpeg
    echo -e "${YELLOW}Stopping stream...${NC}"
    kill $ffmpeg_pid 2>/dev/null || true
    wait $ffmpeg_pid 2>/dev/null || true
    
    echo ""
    echo -e "${GREEN}Monitoring completed${NC}"
}

# Function to analyze results
analyze_results() {
    echo -e "${BLUE}Analyzing results...${NC}"
    
    # Count frames from ffmpeg log
    if [ -f "$LOG_DIR/ffmpeg.log" ]; then
        FRAME_COUNT=$(grep -c "frame=" "$LOG_DIR/ffmpeg.log" || echo "0")
    fi
    
    # Calculate averages
    local avg_cpu=$(echo $CPU_SAMPLES | tr ',' '\n' | grep -v '^$' | awk '{sum+=$1; count++} END {if(count>0) printf "%.2f", sum/count; else print "0"}')
    local avg_mem=$(echo $MEMORY_SAMPLES | tr ',' '\n' | grep -v '^$' | awk '{sum+=$1; count++} END {if(count>0) printf "%.2f", sum/count; else print "0"}')
    
    # Get max values
    local max_cpu=$(echo $CPU_SAMPLES | tr ',' '\n' | grep -v '^$' | sort -n | tail -1)
    local max_mem=$(echo $MEMORY_SAMPLES | tr ',' '\n' | grep -v '^$' | sort -n | tail -1)
    
    # Calculate frame rate
    local duration=$(($(date +%s) - START_TIME))
    local frame_rate=0
    if [ $duration -gt 0 ]; then
        frame_rate=$(echo "scale=2; $FRAME_COUNT / $duration" | bc)
    fi
    
    echo ""
    echo "=========================================="
    echo "Performance Summary"
    echo "=========================================="
    echo "Duration: ${duration}s"
    echo "Total Frames: $FRAME_COUNT"
    echo "Average Frame Rate: ${frame_rate} FPS"
    echo ""
    echo "CPU Usage:"
    echo "  Average: ${avg_cpu}%"
    echo "  Maximum: ${max_cpu}%"
    echo ""
    echo "Memory Usage:"
    echo "  Average: ${avg_mem}MB"
    echo "  Maximum: ${max_mem}MB"
    echo ""
    echo "Errors: $ERROR_COUNT"
    echo ""
    
    # Generate JSON results
    cat > "$RESULTS_FILE" << EOF
{
  "timestamp": $(date +%s),
  "camera": "$CAMERA",
  "duration": $duration,
  "metrics": {
    "frameCount": $FRAME_COUNT,
    "frameRate": $frame_rate,
    "cpuAverage": $avg_cpu,
    "cpuMax": $max_cpu,
    "memoryAverage": $avg_mem,
    "memoryMax": $max_mem,
    "errorCount": $ERROR_COUNT,
    "samples": $sample_count
  },
  "cpuSamples": [${CPU_SAMPLES%,}],
  "memorySamples": [${MEMORY_SAMPLES%,}]
}
EOF
    
    echo "Results saved to: $RESULTS_FILE"
}

# Main execution
main() {
    # Start stream
    start_stream
    local ffmpeg_pid=$?
    
    # Monitor performance
    monitor_performance $ffmpeg_pid $DURATION $INTERVAL
    
    # Analyze results
    analyze_results
    
    # Success criteria check
    echo ""
    echo "=========================================="
    echo "Success Criteria Check"
    echo "=========================================="
    
    local avg_cpu=$(echo $CPU_SAMPLES | tr ',' '\n' | grep -v '^$' | awk '{sum+=$1; count++} END {if(count>0) printf "%.2f", sum/count; else print "0"}')
    local avg_mem=$(echo $MEMORY_SAMPLES | tr ',' '\n' | grep -v '^$' | awk '{sum+=$1; count++} END {if(count>0) printf "%.2f", sum/count; else print "0"}')
    
    local cpu_pass=$(echo "$avg_cpu < 30" | bc -l)
    local mem_pass=$(echo "$avg_mem < 100" | bc -l)
    
    if [ "$cpu_pass" -eq 1 ]; then
        echo -e "✓ CPU Usage: ${avg_cpu}% < 30%"
    else
        echo -e "${RED}✗ CPU Usage: ${avg_cpu}% >= 30%${NC}"
    fi
    
    if [ "$mem_pass" -eq 1 ]; then
        echo -e "✓ Memory Usage: ${avg_mem}MB < 100MB"
    else
        echo -e "${RED}✗ Memory Usage: ${avg_mem}MB >= 100MB${NC}"
    fi
    
    if [ $ERROR_COUNT -eq 0 ]; then
        echo -e "✓ No Errors"
    else
        echo -e "${RED}✗ Errors: $ERROR_COUNT${NC}"
    fi
}

# Run main
main
