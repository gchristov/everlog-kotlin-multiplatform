#!/bin/bash
set -e

# Usage: ./bq_query.sh <credentials_file> [query.sql | -] [--query SQL] [--dry-run] [--csv] [--output file.csv] [--max-gb N]
# Example: ./bq_query.sh bigquery-credentials-prod.json query.sql
#          echo "SELECT 1" | ./bq_query.sh bigquery-credentials-prod.json

CREDENTIALS=$1

if [ -z "$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file is required."
  echo "Usage: ./bq_query.sh <credentials_file> [query.sql | -] [options]"
  exit 1
fi

if [ ! -f "tools/bigquery/$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file 'tools/bigquery/$CREDENTIALS' does not exist."
  exit 1
fi
shift

docker build -q -t everlog-bigquery tools/bigquery > /dev/null

# The repo is mounted at /repo so SQL files and --output paths work relative to the repo root.
# Credentials are mounted separately and read-only.
docker run --rm -i \
  -v "$(pwd):/repo" \
  -w /repo \
  -v "$(pwd)/tools/bigquery/$CREDENTIALS:/$CREDENTIALS:ro" \
  everlog-bigquery --credentials "/$CREDENTIALS" "$@"
