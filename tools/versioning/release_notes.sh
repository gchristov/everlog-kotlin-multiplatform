#!/bin/bash

# Prints GitHub release notes for the commits since the previous tag, one bullet per commit:
#   - [TAS-123] Something changed (https://github.com/<repo>/pull/45)
#
# Arguments: REPO (e.g. gchristov/everlog-kotlin-multiplatform) and optional REF (defaults to HEAD)
REPO=$1
REF=${2:-HEAD}

if [[ -z "$REPO" ]]; then
    echo "Usage: $0 <owner/repo> [ref]"
    exit 1
fi

# Look for the previous tag from the parent commit, so REF being tagged already doesn't produce empty notes
PREVIOUS_TAG=$(git describe --tags --abbrev=0 "$REF^" 2>/dev/null)
if [[ -z "$PREVIOUS_TAG" ]]; then
    RANGE="$REF"
else
    RANGE="$PREVIOUS_TAG..$REF"
fi

# Squash-merged PR commits end in "(#123)", which is turned into a link to the PR
git --no-pager log --pretty=format:"%s" --no-merges --reverse "$RANGE" \
    | sed -E "s|^(.*) \(#([0-9]+)\)$|\1 (https://github.com/$REPO/pull/\2)|; s|^|- |"
echo
