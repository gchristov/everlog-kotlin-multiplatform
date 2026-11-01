#!/bin/bash
# Tags an uploaded release candidate and creates the version's GitHub release, replacing an earlier
# candidate's. Run by release-check.yml after the Play upload, from the repo root.
#
#   tag_release.sh <owner/repo> <version-code> <sha>
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
REPO=${1:-}
CODE=${2:-}
SHA=${3:-}

fail() {
    echo "Error: $*" >&2
    exit 1
}

[[ -n "$REPO" && -n "$CODE" && -n "$SHA" ]] || fail "usage: $0 <owner/repo> <version-code> <sha>"

VERSION=$(tr -d '[:space:]' < tools/versioning/version.txt)
[[ "$VERSION" =~ ^([0-9])\.([0-9]{1,2})\.([0-9])$ ]] || fail "version $VERSION must be M.m.P"
BASE=$((BASH_REMATCH[1] * 1000000 + 10#${BASH_REMATCH[2]} * 10000 + BASH_REMATCH[3] * 1000))
# A release candidate's code is the version's base, type 4 and the candidate number
((CODE > BASE + 400 && CODE <= BASE + 499)) || fail "$CODE isn't a release candidate of $VERSION"
CANDIDATE=$((CODE % 100))

# The candidate's tag is what the next release run counts to number its candidate
gh api "repos/$REPO/git/refs" -f ref="refs/tags/$VERSION-rc$CANDIDATE" -f sha="$SHA" > /dev/null

# A later candidate replaces the earlier one's GitHub release and version tag
if gh release view "$VERSION" --repo "$REPO" > /dev/null 2>&1; then
    gh release delete "$VERSION" --repo "$REPO" --cleanup-tag --yes
fi

NOTES=$(mktemp)
trap 'rm -f "$NOTES"' EXIT
"$DIR/release_notes.sh" "$REPO" "$SHA" > "$NOTES"
gh release create "$VERSION" --repo "$REPO" --target "$SHA" --title "$VERSION" --notes-file "$NOTES"
