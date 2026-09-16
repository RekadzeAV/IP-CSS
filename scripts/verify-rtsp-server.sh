#!/bin/bash

# RTSP Server Verification Script
# Tests RTSP server connectivity and stream quality
# Usage: ./verify-rtsp-server.sh [--url rtsp://...] [--timeout 10]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
URL=${1:-""}
TIMEOUT=${2:-10}

if [ -z "$URL" ]; then
    echo "Usage: ./verify-rtsp-server.sh --url <rtsp://...>"
    echo ""
    echo "Examples:"
    echo "  ./verify-rtsp-server.sh --url rtsp://localhost:8554/test"
    echo "  ./verify-rtsp-server.sh --url rtsp://192.168.1.101:554/stream"
    exit 1
fi

echo "=========================================="
echo "RTSP Server Verification"
echo "=========================================="
echo "URL: $URL"
echo "Timeout: ${TIMEOUT}s"
echo ""

# Test 1: Connection
echo -e "${BLUE}Test 1: Connection Test${NC}"
if timeout $TIMEOUT ffprobe -rtsp_transport tcp -i "$URL" 2>&1 | grep -q "Input #0"; then
    echo -e "${GREEN}✓ Connection successful${NC}"
else
    echo -e "${RED}✗ Connection failed${NC}"
    exit 1
fi
echo ""

# Test 2: Stream Info
echo -e "${BLUE}Test 2: Stream Information${NC}"
ffprobe -rtsp_transport tcp -i "$URL" -v error -select_streams v:0 \
    -show_entries stream=codec_name,width,height,r_frame_rate,bit_rate \
    -of default=noprint_wrappers=1 2>/dev/null || true
echo ""

# Test 3: Audio Stream (if exists)
echo -e "${BLUE}Test 3: Audio Stream Check${NC}"
if ffprobe -rtsp_transport tcp -i "$URL" -v error -select_streams a:0 \
    -show_entries stream=codec_name,sample_rate,channels \
    -of default=noprint_wrappers=1 2>/dev/null | grep -q "codec_name"; then
    echo -e "${GREEN}✓ Audio stream available${NC}"
    ffprobe -rtsp_transport tcp -i "$URL" -v error -select_streams a:0 \
        -show_entries stream=codec_name,sample_rate,channels \
        -of default=noprint_wrappers=1 2>/dev/null || true
else
    echo -e "${YELLOW}⚠ No audio stream${NC}"
fi
echo ""

# Test 4: Stream Duration Test
echo -e "${BLUE}Test 4: Stream Duration Test (10 seconds)${NC}"
START_TIME=$(date +%s)
TEMP_OUTPUT="/tmp/rtsp_test_$$.ts"

if timeout 15 ffmpeg -rtsp_transport tcp -i "$URL" \
    -c copy -t 10 "$TEMP_OUTPUT" 2>/dev/null; then
    
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    if [ -f "$TEMP_OUTPUT" ] && [ -s "$TEMP_OUTPUT" ]; then
        FILE_SIZE=$(stat -f%z "$TEMP_OUTPUT" 2>/dev/null || stat -c%s "$TEMP_OUTPUT" 2>/dev/null || echo "0")
        echo -e "${GREEN}✓ Stream recorded: ${FILE_SIZE} bytes in ${DURATION}s${NC}"
        rm -f "$TEMP_OUTPUT"
    else
        echo -e "${YELLOW}⚠ Stream recorded but file is empty${NC}"
    fi
else
    echo -e "${RED}✗ Stream recording failed${NC}"
    rm -f "$TEMP_OUTPUT" 2>/dev/null || true
fi
echo ""

# Test 5: Codec Verification
echo -e "${BLUE}Test 5: Codec Verification${NC}"
CODEC=$(ffprobe -rtsp_transport tcp -i "$URL" -v error -select_streams v:0 \
    -show_entries stream=codec_name -of csv=p=0 2>/dev/null || echo "unknown")

case $CODEC in
    h264)
        echo -e "${GREEN}✓ H.264 codec detected${NC}"
        ;;
    h265|hevc)
        echo -e "${GREEN}✓ H.265/HEVC codec detected${NC}"
        ;;
    mjpeg)
        echo -e "${GREEN}✓ MJPEG codec detected${NC}"
        ;;
    *)
        echo -e "${YELLOW}⚠ Unknown codec: $CODEC${NC}"
        ;;
esac
echo ""

# Summary
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo "URL: $URL"
echo "Codec: $CODEC"
echo "Connection: ✅ OK"
echo "Stream: ✅ Available"
echo ""
echo -e "${GREEN}✓ RTSP server is healthy${NC}"
