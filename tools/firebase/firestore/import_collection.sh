#!/bin/bash
set -e

# Usage: ./import_collection.sh <collection_name> <input_file> <credentials_file>

COLLECTION=$1
IMPORT_FILE=$2
CREDENTIALS=$3

if [ -z "$COLLECTION" ] || [ -z "$IMPORT_FILE" ] || [ -z "$CREDENTIALS" ]; then
  echo "❌ Error: All arguments are required."
  echo "Usage: ./import_collection.sh <collection_name> <input_file> <credentials_file>"
  exit 1
fi

if [ ! -f "tools/firebase/firestore/$IMPORT_FILE" ]; then
  echo "❌ Error: Import file 'tools/firebase/firestore/$IMPORT_FILE' does not exist."
  exit 1
fi

if [ ! -f "tools/firebase/$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file 'tools/firebase/$CREDENTIALS' does not exist."
  exit 1
fi

echo "🏗 Building Docker image..."
docker build -t everlog-firestore-import tools/firebase/firestore

echo "🚀 Importing into collection: $COLLECTION from $IMPORT_FILE using $CREDENTIALS"
# We mount the local tools/firebase/firestore directory to /app in the container
# so that the script can read import.json from your machine.
# We mount the credentials to a separate path to avoid overlapping mount issues on some Docker configurations.
docker run --rm \
  -v "$(pwd)/tools/firebase/firestore:/app" \
  -v "$(pwd)/tools/firebase/$CREDENTIALS:/$CREDENTIALS:ro" \
  everlog-firestore-import import "$COLLECTION" --input "$IMPORT_FILE" --credentials "/$CREDENTIALS"

echo "✅ Done! Collection $COLLECTION has been updated using tools/firebase/firestore/$IMPORT_FILE"
