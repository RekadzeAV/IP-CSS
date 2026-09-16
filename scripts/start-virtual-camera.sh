#!/bin/bash

# RTSP Virtual Camera Server
# Simulates RTSP cameras using FFmpeg for testing
# Usage: ./start-virtual-camera.sh [--camera hikvision|dahua|axis] [--port 8554]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
CAMERA=${1:-"hikvision"}
PORT=${2:-8554}
STREAM_NAME="test"
LOOP_VIDEO=${3:-true}

echo "=========================================="
echo "RTSP Virtual Camera Server"
echo "=========================================="
echo "Camera: $CAMERA"
echo "Port: $PORT"
echo "Stream: $STREAM_NAME"
echo ""

# Camera configurations
case $CAMERA in
    hikvision)
        RESOLUTION="2688x1520"
        FPS="25"
        CODEC="libx264"
        BITRATE="4000k"
        ;;
    dahua)
        RESOLUTION="2688x1520"
        FPS="20"
        CODEC="libx265"
        BITRATE="3000k"
        ;;
    axis)
        RESOLUTION="1920x1080"
        FPS="30"
        CODEC="libx264"
        BITRATE="2000k"
        ;;
    *)
        echo -e "${RED}Unknown camera: $CAMERA${NC}"
        echo "Available: hikvision, dahua, axis"
        exit 1
        ;;
esac

echo "Configuration:"
echo "  Resolution: $RESOLUTION"
echo "  FPS: $FPS"
echo "  Codec: $CODEC"
echo "  Bitrate: $BITRATE"
echo ""

# Create test video if not exists
TEST_VIDEO="test_video.mp4"
if [ ! -f "$TEST_VIDEO" ]; then
    echo -e "${BLUE}Creating test video...${NC}"
    ffmpeg -f lavfi -i testsrc=duration=60:size=$RESOLUTION:rate=$FPS \
           -f lavfi -i sine=frequency=1000:duration=60 \
           -c:v $CODEC -b:v $BITRATE -c:a aac -f mp4 $TEST_VIDEO \
           -y 2>/dev/null
    echo -e "${GREEN}✓ Test video created: $TEST_VIDEO${NC}"
fi

# Start RTSP server
echo -e "${BLUE}Starting RTSP server on port $PORT...${NC}"
echo ""

# Create RTSP URL
RTSP_URL="rtsp://0.0.0.0:$PORT/$STREAM_NAME"
echo "RTSP URL: $RTSP_URL"
echo ""
echo -e "${GREEN}Server started - Press Ctrl+C to stop${NC}"
echo ""

# FFmpeg RTSP server command
if command -v ffmpeg &> /dev/null; then
    ffmpeg -re -stream_loop 0 -i "$TEST_VIDEO" \
           -c:v $CODEC -b:v $BITRATE -maxrate $BITRATE -bufsize $BITRATE \
           -r $FPS -g 50 \
           -c:a aac -ar 48000 -ac 1 -b:a 128k \
           -f rtsp "$RTSP_URL"
else
    echo -e "${RED}FFmpeg not found!${NC}"
    echo "Install FFmpeg 8.0+ to run virtual camera server"
    exit 1
fi
