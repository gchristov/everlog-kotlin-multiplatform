#!/bin/bash
set -e

# Usage: ./tools/exercises/run.sh <script> [args]
# e.g.   ./tools/exercises/run.sh migrate --export tools/firebase/firestore/exercises.json
#
# Runs a tools/exercises script in Docker, with the repo (read-only) at /repo and the Exercise Animatic pack
# (read-only) at /pack. Results go to tools/exercises/out (gitignored).
# EXERCISE_PACK_DIR defaults to ~/Documents/Archive/Exercises.

SCRIPT=$1
shift || true
PACK_DIR=${EXERCISE_PACK_DIR:-$HOME/Documents/Archive/Exercises}

if [ -z "$SCRIPT" ] || [ ! -f "tools/exercises/$SCRIPT.py" ]; then
  echo "❌ Usage: ./tools/exercises/run.sh <script> [args] (run from the repo root)"
  exit 1
fi
if [ ! -d "$PACK_DIR" ]; then
  echo "❌ Exercise pack not found at $PACK_DIR (set EXERCISE_PACK_DIR)"
  exit 1
fi

docker build -q -t everlog-exercises tools/exercises > /dev/null
mkdir -p tools/exercises/out
docker run --rm \
  -e PYTHONDONTWRITEBYTECODE=1 \
  -v "$(pwd):/repo:ro" \
  -v "$(pwd)/tools/exercises/out:/out" \
  -v "$PACK_DIR:/pack:ro" \
  -w /repo/tools/exercises \
  everlog-exercises python "$SCRIPT.py" --out /out "$@"
