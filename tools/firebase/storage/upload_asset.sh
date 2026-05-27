#!/bin/bash
set -e

# Usage: ./upload_asset.sh <bucket_name> <input_dir> <credentials_file>

BUCKET=$1
INPUT_DIR=$2
CREDENTIALS=$3

if [ -z "$BUCKET" ] || [ -z "$INPUT_DIR" ] || [ -z "$CREDENTIALS" ]; then
  echo "❌ Error: All arguments are required."
  echo "Usage: ./upload_asset.sh <bucket_name> <input_dir> <credentials_file>"
  exit 1
fi

if [ ! -d "tools/firebase/storage/$INPUT_DIR" ]; then
  echo "❌ Error: Input directory 'tools/firebase/storage/$INPUT_DIR' does not exist."
  exit 1
fi

if [ ! -f "tools/firebase/$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file 'tools/firebase/$CREDENTIALS' does not exist."
  exit 1
fi

echo "🏗 Building Docker image..."
docker build -t everlog-storage-upload tools/firebase/storage

echo "🚀 Uploading all assets from: $INPUT_DIR to $BUCKET"

# We mount the local tools/firebase/storage directory to /app in the container
# We mount the credentials to a separate path to avoid overlapping mount issues on some Docker configurations
docker run --rm \
  -v "$(pwd)/tools/firebase/storage:/app" \
  -v "$(pwd)/tools/firebase/$CREDENTIALS:/$CREDENTIALS:ro" \
  everlog-storage-upload --bucket "$BUCKET" --input "$INPUT_DIR" --credentials "/$CREDENTIALS"

echo "✅ Done!"
