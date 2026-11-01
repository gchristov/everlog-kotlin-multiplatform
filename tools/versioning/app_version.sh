#!/bin/bash
# Prints a build's version code and version name suffix as GitHub Actions outputs:
#   versionCode=2220101
#   versionNameSuffix=pr-253-09f8159
#
#   app_version.sh pr <number> <base-sha> <head-sha>
#   app_version.sh <master|nightly|release>
#
# VERSION_CODE_NOW (seconds since the epoch) stands in for the current time, for tests.
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
TYPE=${1:-}

utc_date() {
    local format=$1 now=${VERSION_CODE_NOW:-$(date -u +%s)}
    date -u -d "@$now" "+$format" 2>/dev/null || date -u -r "$now" "+$format"
}

case $TYPE in
    pr)
        [[ $# -eq 4 ]] || { echo "Error: usage: $0 pr <number> <base-sha> <head-sha>" >&2; exit 1; }
        CODE=$("$DIR/version_code.sh" pr "$3" "$4")
        SUFFIX="pr-$2-${4:0:7}"
        ;;
    nightly)
        CODE=$("$DIR/version_code.sh" nightly)
        SUFFIX="nightly-$(utc_date %d%m%Y)"
        ;;
    release)
        CODE=$("$DIR/version_code.sh" release)
        # Releases use the bare version name
        SUFFIX="none"
        ;;
    *)
        CODE=$("$DIR/version_code.sh" "$TYPE")
        SUFFIX="$TYPE"
        ;;
esac

echo "versionCode=$CODE"
echo "versionNameSuffix=$SUFFIX"
