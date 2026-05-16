#!/bin/bash
set -e

# Get collection name from argument, or use a default
COLLECTION=${1:-"global/exercises/all"}

echo "🏗 Building Docker image..."
docker build -t everlog-firestore-export tools/firestore

echo "🚀 Exporting collection: $COLLECTION"
# We mount the local tools/firestore directory to /app in the container
# so that the exported export.json appears on your machine.
docker run --rm \
  -v "$(pwd)/tools/firestore:/app" \
  everlog-firestore-export export "$COLLECTION"

echo "✅ Done! You can find the export at tools/firestore/export.json"
