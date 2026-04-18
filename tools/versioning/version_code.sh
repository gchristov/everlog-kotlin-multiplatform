#!/bin/bash

# Arguments: TYPE (staging, nightly, or master) and optional --release flag
TYPE=$1
RELEASE_FLAG=$2

if [[ -z "$TYPE" ]]; then
    echo "Usage: $0 <staging|nightly|master> [--release]"
    exit 1
fi

# Map string type to index
case $TYPE in
    "staging")
        TYPE_INDEX=1
        ;;
    "nightly")
        TYPE_INDEX=2
        ;;
    "master")
        TYPE_INDEX=3
        ;;
    *)
        echo "Error: Invalid type '$TYPE'. Must be one of: staging, nightly, master"
        exit 1
        ;;
esac

# 1. Read Base Version
VERSION_FILE="tools/versioning/version.txt"
if [[ ! -f "$VERSION_FILE" ]]; then
    echo "Error: $VERSION_FILE not found"
    exit 1
fi
BASE_VERSION=$(cat "$VERSION_FILE")

# 2. Count commits since latest tag
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)
if [[ -z "$LATEST_TAG" ]]; then
    COMMIT_COUNT=$(git rev-list --count HEAD)
else
    COMMIT_COUNT=$(git rev-list --count "$LATEST_TAG"..HEAD)
fi

# 3. Calculate Sequence (SS)
if [[ "$RELEASE_FLAG" == "--release" ]]; then
    SS=$((80 + (COMMIT_COUNT % 20)))
else
    SS=$((COMMIT_COUNT % 80))
fi

# 4. Calculate Final Version Code
# Position: MmmPTSS
# Base Version is MmmP000 (e.g., 2090000)
# T is Type Index (100s place)
# SS is Sequence (units and tens)
VERSION_CODE=$((BASE_VERSION + (TYPE_INDEX * 100) + SS))

echo "$VERSION_CODE"
