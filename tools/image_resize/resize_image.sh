#!/bin/bash
set -e

# Configuration
IMAGE_NAME="everlog-image-resize"
INPUT_DIR_NAME="input"
OUTPUT_DIR_NAME="output"

echo "🏗 Building Docker image..."
docker build -t "$IMAGE_NAME" tools/image_resize

echo "🚀 Running image optimization..."
# We mount the local tools/image_resize directory to /app in the container
docker run --rm \
  -v "$(pwd)/tools/image_resize:/app" \
  "$IMAGE_NAME" \
  --input "/app/$INPUT_DIR_NAME" \
  --output "/app/$OUTPUT_DIR_NAME" \
  "$@"

echo "✅ Done! Optimized WebP files are in tools/image_resize/$OUTPUT_DIR_NAME"
