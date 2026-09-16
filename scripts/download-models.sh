#!/usr/bin/env bash
# Download AI analytics models (YOLO, etc.) into data/models or specified directory
# Usage: ./download-models.sh [OUT_DIR]
set -e
OUT_DIR="${1:-data/models}"
mkdir -p "$OUT_DIR"

# YOLOv8n ONNX (small, from Ultralytics)
YOLO_URL="https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n.onnx"
YOLO_PATH="$OUT_DIR/yolov8n.onnx"

if [ ! -f "$YOLO_PATH" ]; then
  echo "Downloading YOLOv8n ONNX to $YOLO_PATH ..."
  if command -v curl &>/dev/null; then
    curl -sL "$YOLO_URL" -o "$YOLO_PATH"
  elif command -v wget &>/dev/null; then
    wget -q -O "$YOLO_PATH" "$YOLO_URL"
  else
    echo "Install curl or wget to download models."
    exit 1
  fi
  echo "Done: $YOLO_PATH"
else
  echo "Already exists: $YOLO_PATH"
fi

echo "Models directory: $OUT_DIR"
