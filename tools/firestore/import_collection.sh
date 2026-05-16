#!/bin/bash
set -e

# Get collection name from argument, or use a default
COLLECTION=${1:-"global/exercises/all"}
IMPORT_FILE="import.json"

echo "🏗 Building Docker image..."
docker build -t everlog-firestore-import tools/firestore

echo "🚀 Importing into collection: $COLLECTION from $IMPORT_FILE"
# We mount the local tools/firestore directory to /app in the container
# so that the script can read import.json from your machine.
docker run --rm \
  -v "$(pwd)/tools/firestore:/app" \
  everlog-firestore-import import "$COLLECTION" --input "$IMPORT_FILE"

echo "✅ Done! Collection $COLLECTION has been updated using tools/firestore/$IMPORT_FILE"
