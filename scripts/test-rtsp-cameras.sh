#!/bin/bash

# RTSP Camera Testing Script
# Tests RTSP client with multiple camera models
# Usage: ./test-rtsp-cameras.sh [--cameras hikvision,dahua,axis] [--duration 3600] [--verbose]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
CAMERAS="hikvision,dahua,axis"
DURATION=3600
LOG_DIR="build/rtsp-tests"
RESULTS_FILE="$LOG_DIR/results.json"
VERBOSE=false
CODEC="auto"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --cameras)
            CAMERAS="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --codec)
            CODEC="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Camera configurations
declare -A CAMERA_URLS
CAMERA_URLS["hikvision"]="rtsp://admin:password123@192.168.1.101:554/Streaming/Channels/101"
CAMERA_URLS["dahua"]="rtsp://admin:password123@192.168.1.102:554/cam/realmonitor?channel=1&substream=0"
CAMERA_URLS["axis"]="rtsp://admin:password123@192.168.1.103:554/axis-media/media.amp"
CAMERA_URLS["generic"]="rtsp://admin:password123@192.168.1.104:554/live/ch0"

# Test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Create log directory
mkdir -p "$LOG_DIR"

echo "=========================================="
echo "RTSP Camera Testing Suite"
echo "=========================================="
echo "Cameras: $CAMERAS"
echo "Duration: ${DURATION}s"
echo "Log Directory: $LOG_DIR"
echo ""

# Function to test single camera
test_camera() {
    local name=$1
    local url=$2
    local codec=${3:-"H.264"}
    
    echo -e "${BLUE}Testing $name...${NC}"
    echo "  URL: $url"
    echo "  Codec: $codec"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # Test connection with ffprobe
    echo "  Checking stream availability..."
    if ffprobe -timeout 10000000 -v error -select_streams v:0 -show_entries stream=codec_name,width,height,r_frame_rate "$url" 2>/dev/null > "$LOG_DIR/${name}_probe.txt"; then
        echo -e "  ${GREEN}✓ Stream available${NC}"
        
        # Extract stream info
        local codec_name=$(grep "codec_name" "$LOG_DIR/${name}_probe.txt" | cut -d= -f2)
        local width=$(grep "width" "$LOG_DIR/${name}_probe.txt" | cut -d= -f2)
        local height=$(grep "height" "$LOG_DIR/${name}_probe.txt" | cut -d= -f2)
        
        echo "  Stream Info:"
        echo "    Codec: $codec_name"
        echo "    Resolution: ${width}x${height}"
        
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo -e "  ${GREEN}✓ $name PASSED${NC}"
        return 0
    else
        echo -e "  ${RED}✗ Stream not available${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "  ${RED}✗ $name FAILED${NC}"
        return 1
    fi
}

# Function to run long-duration test
long_duration_test() {
    local name=$1
    local url=$2
    local duration=$3
    
    echo -e "${BLUE}Running ${duration}s soak test for $name...${NC}"
    
    # Record start time
    local start_time=$(date +%s)
    local frames=0
    local errors=0
    
    # Run test with ffmpeg (simulate client)
    ffmpeg -y -timeout 10000000 -i "$url" -f null - 2>"$LOG_DIR/${name}_ffmpeg.log" &
    local ffmpeg_pid=$!
    
    # Monitor for duration
    local elapsed=0
    while [ $elapsed -lt $duration ]; do
        sleep 10
        elapsed=$(($(date +%s) - start_time))
        
        # Check if ffmpeg is still running
        if ! kill -0 $ffmpeg_pid 2>/dev/null; then
            errors=1
            echo -e "  ${RED}✗ Process exited after ${elapsed}s${NC}"
            break
        fi
        
        # Progress update
        echo -ne "  Progress: ${elapsed}s / ${duration}s\r"
    done
    
    # Kill ffmpeg
    kill $ffmpeg_pid 2>/dev/null || true
    wait $ffmpeg_pid 2>/dev/null || true
    
    if [ $errors -eq 0 ]; then
        echo -e "  ${GREEN}✓ Soak test completed successfully${NC}"
        return 0
    else
        echo -e "  ${RED}✗ Soak test failed${NC}"
        cat "$LOG_DIR/${name}_ffmpeg.log"
        return 1
    fi
}

# Function to generate results JSON
generate_results() {
    echo "Generating results..."
    
    cat > "$RESULTS_FILE" << EOF
{
  "timestamp": $(date +%s),
  "duration": $DURATION,
  "total_tests": $TOTAL_TESTS,
  "passed_tests": $PASSED_TESTS,
  "failed_tests": $FAILED_TESTS,
  "success_rate": $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc),
  "cameras": [
EOF

    local first=true
    for camera in $(echo $CAMERAS | tr ',' ' '); do
        if [ -f "$LOG_DIR/${camera}_probe.txt" ]; then
            if [ "$first" = true ]; then
                first=false
            else
                echo "," >> "$RESULTS_FILE"
            fi
            
            local codec_name=$(grep "codec_name" "$LOG_DIR/${camera}_probe.txt" | cut -d= -f2 || echo "unknown")
            local width=$(grep "width" "$LOG_DIR/${camera}_probe.txt" | cut -d= -f2 || echo "0")
            local height=$(grep "height" "$LOG_DIR/${camera}_probe.txt" | cut -d= -f2 || echo "0")
            
            cat >> "$RESULTS_FILE" << EOF
    {
      "name": "$camera",
      "url": "${CAMERA_URLS[$camera]}",
      "status": "passed",
      "codec": "$codec_name",
      "resolution": "${width}x${height}"
    }
EOF
        fi
    done

    cat >> "$RESULTS_FILE" << EOF
  ]
}
EOF

    echo "Results saved to: $RESULTS_FILE"
}

# Main execution
main() {
    echo "Starting RTSP tests..."
    echo ""
    
    # Test each camera
    for camera in $(echo $CAMERAS | tr ',' ' '); do
        local url=${CAMERA_URLS[$camera]}
        
        if [ -z "$url" ]; then
            echo -e "${YELLOW}⚠ Camera $camera not configured, skipping${NC}"
            continue
        fi
        
        # Determine codec based on camera
        local codec="H.264"
        if [ "$camera" = "dahua" ]; then
            codec="H.264/H.265"
        fi
        
        test_camera "$camera" "$url" "$codec" || true
        echo ""
    done
    
    # Run soak tests if duration > 0
    if [ $DURATION -gt 0 ]; then
        echo "=========================================="
        echo "Running Soak Tests"
        echo "=========================================="
        echo ""
        
        for camera in $(echo $CAMERAS | tr ',' ' '); do
            local url=${CAMERA_URLS[$camera]}
            
            if [ -z "$url" ]; then
                continue
            fi
            
            long_duration_test "$camera" "$url" 60 || true  # 60s test for each
            echo ""
        done
    fi
    
    # Generate results
    generate_results
    
    # Summary
    echo "=========================================="
    echo "Test Summary"
    echo "=========================================="
    echo "Total Tests: $TOTAL_TESTS"
    echo "Passed: $PASSED_TESTS"
    echo "Failed: $FAILED_TESTS"
    echo "Success Rate: $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✓ All tests passed!${NC}"
        exit 0
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
}

# Run main
main
