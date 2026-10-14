#!/bin/bash
set -e

# Runs every query in tools/monitoring/sql against BigQuery (in parallel), then builds
# out/data.json and out/dashboard.html from the results and config.json.
# Usage, from the repo root: ./tools/monitoring/refresh.sh
# Then publish tools/monitoring/out/dashboard.html to the dashboard's artifact (see README.md).

cd "$(dirname "$0")/../.."
mkdir -p tools/monitoring/out

# Build the image once up front, so the parallel runs don't all build it
docker build -q -t everlog-bigquery tools/bigquery > /dev/null

pids=()
for sql in tools/monitoring/sql/*.sql; do
  name=$(basename "$sql" .sql)
  ./tools/bigquery/bq_query.sh bigquery-credentials-prod.json "$sql" --max-gb 5 \
    --output "tools/monitoring/out/$name.csv" > "tools/monitoring/out/$name.log" 2>&1 &
  pids+=("$!:$name")
done

failed=0
for entry in "${pids[@]}"; do
  pid=${entry%%:*}
  name=${entry#*:}
  if wait "$pid"; then
    echo "✅ $name: $(tail -1 "tools/monitoring/out/$name.log")"
  else
    echo "❌ $name failed:"
    cat "tools/monitoring/out/$name.log"
    failed=1
  fi
done
[ "$failed" = 0 ] || exit 1

python3 tools/monitoring/build.py
