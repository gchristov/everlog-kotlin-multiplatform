#!/bin/bash
set -e

# Usage: ./export_collection.sh <collection_name> <output_file> <credentials_file>

COLLECTION=$1
OUTPUT_FILE=$2
CREDENTIALS=$3

if [ -z "$COLLECTION" ] || [ -z "$OUTPUT_FILE" ] || [ -z "$CREDENTIALS" ]; then
  echo "❌ Error: All arguments are required."
  echo "Usage: ./export_collection.sh <collection_name> <output_file> <credentials_file>"
  exit 1
fi

if [ ! -f "tools/firebase/$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file 'tools/firebase/$CREDENTIALS' does not exist."
  exit 1
fi

echo "🏗 Building Docker image..."
docker build -t everlog-firestore-export tools/firebase/firestore

echo "🚀 Exporting collection: $COLLECTION to $OUTPUT_FILE using $CREDENTIALS"
# We mount the local tools/firebase/firestore directory to /app in the container
# so that the exported export.json appears on your machine.
# We mount the credentials to a separate path to avoid overlapping mount issues on some Docker configurations.
docker run --rm \
  -v "$(pwd)/tools/firebase/firestore:/app" \
  -v "$(pwd)/tools/firebase/$CREDENTIALS:/$CREDENTIALS:ro" \
  everlog-firestore-export export "$COLLECTION" --output "$OUTPUT_FILE" --credentials "/$CREDENTIALS"

echo "✅ Done! You can find the export at tools/firebase/firestore/$OUTPUT_FILE"
