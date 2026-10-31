#!/bin/bash
# Checks version_code.sh against throwaway git repos with a known number of commits since a tag.
# Run from the repo root: bash tools/versioning/version_code_test.sh
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/version_code.sh"
BASE=2220000
FAILED=0
REPOS=()
trap 'rm -rf "${REPOS[@]}"' EXIT

# A repo whose latest tag is followed by $1 commits
repo_with_commits() {
    local dir
    dir=$(mktemp -d)
    git -C "$dir" init -q
    git -C "$dir" config user.email test@example.com
    git -C "$dir" config user.name Test
    mkdir -p "$dir/tools/versioning"
    echo "$BASE" > "$dir/tools/versioning/version_code.txt"
    git -C "$dir" add -A
    git -C "$dir" commit -q -m "Release"
    git -C "$dir" tag 2.21.0
    for ((i = 0; i < $1; i++)); do
        git -C "$dir" commit -q --allow-empty -m "Commit $i"
    done
    echo "$dir"
}

expect() {
    local dir=$1 expected=$2
    shift 2
    local actual
    actual=$(cd "$dir" && bash "$SCRIPT" "$@")
    if [[ "$actual" == "$expected" ]]; then
        echo "ok   $* after $(git -C "$dir" rev-list --count 2.21.0..HEAD) commits: $actual"
    else
        echo "FAIL $* after $(git -C "$dir" rev-list --count 2.21.0..HEAD) commits: expected $expected, got $actual"
        FAILED=1
    fi
}

REPO=$(repo_with_commits 68); REPOS+=("$REPO")
expect "$REPO" 2220168 staging
expect "$REPO" 2220268 nightly
expect "$REPO" 2220368 master
expect "$REPO" 2220468 master --release

# A release is above every nightly and master build of its version, even at the end of their range
REPO=$(repo_with_commits 99); REPOS+=("$REPO")
expect "$REPO" 2220299 nightly
expect "$REPO" 2220399 master
expect "$REPO" 2220499 master --release

# The 100th commit wraps (see README), which a release resets
REPO=$(repo_with_commits 100); REPOS+=("$REPO")
expect "$REPO" 2220200 nightly

REPO=$(repo_with_commits 0); REPOS+=("$REPO")
expect "$REPO" 2220200 nightly

if (cd "$REPO" && bash "$SCRIPT" weekly >/dev/null 2>&1); then
    echo "FAIL an unknown type should fail"
    FAILED=1
else
    echo "ok   an unknown type fails"
fi

exit $FAILED
