#!/bin/bash
set -e

# Usage: ./tools/exercises/run.sh <script> [args]
# e.g.   ./tools/exercises/run.sh push --env dev --exercises exercises.json
#
# Runs a tools/exercises script in Docker, with the repo (read-only) at /repo and, when it's there, the Exercise
# Animatic pack (read-only) at /pack. Results go to tools/exercises/out (gitignored).
# EXERCISE_PACK_DIR defaults to ~/Documents/Archive/Exercises; only scripts that read the pack need it.

SCRIPT=$1
shift || true
PACK_DIR=${EXERCISE_PACK_DIR:-$HOME/Documents/Archive/Exercises}

if [ -z "$SCRIPT" ] || [ ! -f "tools/exercises/$SCRIPT.py" ]; then
  echo "❌ Usage: ./tools/exercises/run.sh <script> [args] (run from the repo root)"
  exit 1
fi
PACK_MOUNT=()
if [ -d "$PACK_DIR" ]; then
  PACK_MOUNT=(-v "$PACK_DIR:/pack:ro")
fi

docker build -q -t everlog-exercises tools/exercises > /dev/null
mkdir -p tools/exercises/out
docker run --rm \
  -e PYTHONDONTWRITEBYTECODE=1 \
  -v "$(pwd):/repo:ro" \
  -v "$(pwd)/tools/exercises/out:/out" \
  "${PACK_MOUNT[@]}" \
  -w /repo/tools/exercises \
  everlog-exercises python "$SCRIPT.py" --out /out "$@"
