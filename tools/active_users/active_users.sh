#!/bin/bash
set -e

# Usage: ./active_users.sh <credentials_file> [--min-workouts N] [--max-inactive-days N] [--top N] [--output file.csv]
# Example: ./active_users.sh admin-credentials-prod.json --output active_users.csv

CREDENTIALS=$1

if [ -z "$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file is required."
  echo "Usage: ./active_users.sh <credentials_file> [options]"
  exit 1
fi

if [ ! -f "tools/firebase/$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file 'tools/firebase/$CREDENTIALS' does not exist."
  exit 1
fi
shift

echo "🏗 Building Docker image..."
docker build -t everlog-active-users tools/active_users

echo "🚀 Finding most active users using $CREDENTIALS"
# Mount the tool directory so any --output CSV lands on this machine.
# Credentials are mounted separately and read-only.
docker run --rm \
  -v "$(pwd)/tools/active_users:/app" \
  -v "$(pwd)/tools/firebase/$CREDENTIALS:/$CREDENTIALS:ro" \
  everlog-active-users --credentials "/$CREDENTIALS" "$@"
