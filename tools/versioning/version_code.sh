#!/bin/bash
# Prints the version code for a build: MmmPTSS (see README.md).
#
#   version_code.sh pr <base-ref> <head-ref>   SS = the PR's commit number
#   version_code.sh master                     SS = merges since the version bump
#   version_code.sh nightly                    SS = days since the version bump
#   version_code.sh release                    SS = the release candidate number
#
# VERSION_CODE_NOW (seconds since the epoch) stands in for the current time, for tests.
set -euo pipefail

VERSION_FILE="tools/versioning/version.txt"
MAX_SEQUENCE=99

fail() {
    echo "Error: $*" >&2
    exit 1
}

TYPE=${1:-}
[[ -n "$TYPE" ]] || fail "usage: $0 <pr <base-ref> <head-ref>|master|nightly|release>"
[[ -f "$VERSION_FILE" ]] || fail "$VERSION_FILE not found"

VERSION=$(tr -d '[:space:]' < "$VERSION_FILE")
[[ "$VERSION" =~ ^([0-9])\.([0-9]{1,2})\.([0-9])$ ]] \
    || fail "version $VERSION must be M.m.P with a one-digit major and patch and a minor up to 99"
BASE=$((BASH_REMATCH[1] * 1000000 + 10#${BASH_REMATCH[2]} * 10000 + BASH_REMATCH[3] * 1000))

# The commit that set the current version. Commits that touch the file without changing the
# version (formatting, a merge keeping it) don't restart the sequences.
version_bump_commit() {
    local bump="" commit
    for commit in $(git log --format=%H -- "$VERSION_FILE"); do
        [[ "$(git show "$commit:$VERSION_FILE" | tr -d '[:space:]')" == "$VERSION" ]] || break
        bump=$commit
    done
    [[ -n "$bump" ]] || fail "no commit sets version $VERSION in $VERSION_FILE"
    echo "$bump"
}

case $TYPE in
    pr)
        TYPE_INDEX=1
        [[ $# -eq 3 ]] || fail "usage: $0 pr <base-ref> <head-ref>"
        SEQUENCE=$(git rev-list --count "$2..$3")
        # App Distribution accepts any code, and Android installs a build over one with the same code
        SEQUENCE=$((SEQUENCE > MAX_SEQUENCE ? MAX_SEQUENCE : SEQUENCE))
        ;;
    master)
        TYPE_INDEX=2
        SEQUENCE=$(git rev-list --count --first-parent "$(version_bump_commit)..HEAD")
        SEQUENCE=$((SEQUENCE > MAX_SEQUENCE ? MAX_SEQUENCE : SEQUENCE))
        ;;
    nightly)
        TYPE_INDEX=3
        BUMP_TIME=$(git log -1 --format=%ct "$(version_bump_commit)")
        NOW=${VERSION_CODE_NOW:-$(date -u +%s)}
        SEQUENCE=$((NOW / 86400 - BUMP_TIME / 86400))
        # Play rejects a code it already has, so a nightly can't share the last day's code
        ((SEQUENCE <= MAX_SEQUENCE)) \
            || fail "$VERSION started $SEQUENCE days ago, and nightlies only have $MAX_SEQUENCE days: release it or bump the version"
        ;;
    release)
        TYPE_INDEX=4
        CANDIDATES=$(git tag --list "$VERSION-rc*" | grep -cE "^$VERSION-rc[0-9]+$" || true)
        SEQUENCE=$((CANDIDATES + 1))
        ((SEQUENCE <= MAX_SEQUENCE)) || fail "$VERSION already has $CANDIDATES release candidates"
        ;;
    *)
        fail "invalid type '$TYPE', must be one of: pr, master, nightly, release"
        ;;
esac

echo $((BASE + TYPE_INDEX * 100 + SEQUENCE))
