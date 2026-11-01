#!/bin/bash
# Checks version_code.sh, app_version.sh, tag_release.sh and release_notes.sh against throwaway git repos.
# Run from the repo root: bash tools/versioning/version_code_test.sh
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
SCRIPT="$DIR/version_code.sh"
NOTES="$DIR/release_notes.sh"
APP_VERSION="$DIR/app_version.sh"
TAG_RELEASE="$DIR/tag_release.sh"
FAILED=0
REPOS=()
trap 'rm -rf "${REPOS[@]}"' EXIT

DAY=86400
# 26 Oct 2026, 09:00 UTC
BUMP_TIME=1793005200

new_repo() {
    local dir
    dir=$(mktemp -d)
    REPOS+=("$dir")
    git -C "$dir" init -q -b master
    git -C "$dir" config user.email test@example.com
    git -C "$dir" config user.name Test
    mkdir -p "$dir/tools/versioning"
    REPO=$dir
}

# Commits at the given time (seconds since the epoch), so day counts are known
commit_at() {
    local time=$1 message=$2
    GIT_AUTHOR_DATE="@$time +0000" GIT_COMMITTER_DATE="@$time +0000" \
        git -C "$REPO" commit -q --allow-empty -m "$message"
}

set_version() {
    local version=$1 time=$2
    echo "$version" > "$REPO/tools/versioning/version.txt"
    git -C "$REPO" add -A
    commit_at "$time" "Version bump to $version"
}

merges() {
    local count=$1 time=$2
    for ((i = 1; i <= count; i++)); do
        commit_at "$time" "Change $i (#$i)"
    done
}

expect() {
    local description=$1 expected=$2
    shift 2
    local actual
    actual=$(cd "$REPO" && bash "$SCRIPT" "$@" 2>&1) || actual="failed: $actual"
    if [[ "$actual" == "$expected" ]]; then
        echo "ok   $description: $actual"
    else
        echo "FAIL $description: expected $expected, got $actual"
        FAILED=1
    fi
}

expect_failure() {
    local description=$1
    shift
    if (cd "$REPO" && bash "$SCRIPT" "$@" >/dev/null 2>&1); then
        echo "FAIL $description: expected it to fail"
        FAILED=1
    else
        echo "ok   $description: fails"
    fi
}

expect_notes() {
    local description=$1 expected=$2
    local actual
    actual=$(cd "$REPO" && bash "$NOTES" owner/repo | tr '\n' '|')
    if [[ "$actual" == "$expected" ]]; then
        echo "ok   $description"
    else
        echo "FAIL $description: expected $expected, got $actual"
        FAILED=1
    fi
}

# The version comes from version.txt, and a version bump restarts master's count
new_repo
set_version 2.22.0 "$BUMP_TIME"
expect "master on the version bump" 2220200 master
merges 3 $((BUMP_TIME + DAY))
expect "master after three merges" 2220203 master
merges 96 $((BUMP_TIME + DAY))
expect "master after 99 merges" 2220299 master
merges 1 $((BUMP_TIME + DAY))
expect "master stays at 99 after 100 merges" 2220299 master
set_version 2.23.0 $((BUMP_TIME + 2 * DAY))
merges 1 $((BUMP_TIME + 2 * DAY))
expect "master restarts with the next version" 2230201 master

# Merges on a side branch don't count, only master's own history
new_repo
set_version 2.22.0 "$BUMP_TIME"
git -C "$REPO" switch -q -c feature
merges 4 "$BUMP_TIME"
git -C "$REPO" switch -q master
GIT_COMMITTER_DATE="@$BUMP_TIME +0000" git -C "$REPO" merge -q --no-ff -m "Merge feature" feature
expect "master counts a merge commit once" 2220201 master

# Touching version.txt without changing the version doesn't restart the count
new_repo
set_version 2.22.0 "$BUMP_TIME"
merges 5 $((BUMP_TIME + DAY))
printf '2.22.0\n\n' > "$REPO/tools/versioning/version.txt"
git -C "$REPO" add -A
commit_at $((BUMP_TIME + DAY)) "Reformat version.txt"
expect "master keeps counting from the real bump" 2220206 master
VERSION_CODE_NOW=$((BUMP_TIME + 2 * DAY)) expect "nightly keeps counting from the real bump" 2220302 nightly

# Nightlies count days since the version bump, in UTC
new_repo
set_version 2.22.0 "$BUMP_TIME"
VERSION_CODE_NOW=$BUMP_TIME expect "nightly on the day of the bump" 2220300 nightly
VERSION_CODE_NOW=$((BUMP_TIME + 15 * 3600)) expect "nightly at midnight after the bump" 2220301 nightly
VERSION_CODE_NOW=$((BUMP_TIME + 6 * DAY)) expect "nightly six days later" 2220306 nightly
VERSION_CODE_NOW=$((BUMP_TIME + 99 * DAY)) expect "nightly on day 99" 2220399 nightly
VERSION_CODE_NOW=$((BUMP_TIME + 100 * DAY)) expect_failure "nightly on day 100" nightly

# Release candidates count the version's rc tags
new_repo
set_version 2.22.0 "$BUMP_TIME"
expect "first release candidate" 2220401 release
git -C "$REPO" tag 2.22.0-rc1
merges 1 "$BUMP_TIME"
expect "second release candidate" 2220402 release
git -C "$REPO" tag 2.22.0-rc2
git -C "$REPO" tag 2.21.0-rc5
git -C "$REPO" tag 2.22.0-rc-notes
expect "other versions' candidates and other tags don't count" 2220403 release
for ((i = 3; i <= 99; i++)); do git -C "$REPO" tag "2.22.0-rc$i"; done
expect_failure "a 100th release candidate" release

# A hotfix raises the patch digit, above every build of the release it fixes
new_repo
set_version 2.22.1 "$BUMP_TIME"
expect "hotfix release candidate" 2221401 release

# PR builds count the PR's own commits, not the base branch's
new_repo
set_version 2.22.0 "$BUMP_TIME"
merges 7 "$BUMP_TIME"
git -C "$REPO" switch -q -c pr
merges 2 "$BUMP_TIME"
expect "second commit of a PR" 2220102 pr master pr
merges 120 "$BUMP_TIME"
expect "PR stays at 99" 2220199 pr master pr
expect_failure "PR without its refs" pr

# The order within a version: PR < master < nightly < release, and every build of the next version is above them
new_repo
set_version 2.22.0 "$BUMP_TIME"
merges 99 "$BUMP_TIME"
for ((i = 1; i <= 99; i++)); do git -C "$REPO" tag "2.22.0-rc$i"; done
git -C "$REPO" tag -d 2.22.0-rc99 >/dev/null
MASTER=$(cd "$REPO" && bash "$SCRIPT" master)
NIGHTLY=$(cd "$REPO" && VERSION_CODE_NOW=$((BUMP_TIME + 99 * DAY)) bash "$SCRIPT" nightly)
RELEASE=$(cd "$REPO" && bash "$SCRIPT" release)
set_version 2.22.1 $((BUMP_TIME + DAY))
git -C "$REPO" switch -q -c pr
NEXT_PR=$(cd "$REPO" && bash "$SCRIPT" pr master master)
if ((MASTER < NIGHTLY && NIGHTLY < RELEASE && RELEASE < NEXT_PR)); then
    echo "ok   order: master $MASTER < nightly $NIGHTLY < release $RELEASE < next version's first PR build $NEXT_PR"
else
    echo "FAIL order: master $MASTER, nightly $NIGHTLY, release $RELEASE, next version's first PR build $NEXT_PR"
    FAILED=1
fi

new_repo
echo "2.22" > "$REPO/tools/versioning/version.txt"
git -C "$REPO" add -A
commit_at "$BUMP_TIME" "Bad version"
expect_failure "a version without a patch" master
expect_failure "an unknown type" weekly

# Release notes start from the previous version's tag, skipping release candidates and the version being released
new_repo
set_version 2.21.0 "$BUMP_TIME"
git -C "$REPO" tag 2.21.0
set_version 2.22.0 "$BUMP_TIME"
commit_at "$BUMP_TIME" "First change (#10)"
git -C "$REPO" tag 2.22.0-rc1
git -C "$REPO" tag 2.22.0
commit_at "$BUMP_TIME" "Fix found in testing (#11)"
expect_notes "notes for a second candidate" \
    "- Version bump to 2.22.0|- First change (https://github.com/owner/repo/pull/10)|- Fix found in testing (https://github.com/owner/repo/pull/11)|"

# The CI outputs: the version code and the version name suffix for each type
new_repo
set_version 2.22.0 "$BUMP_TIME"
merges 3 "$BUMP_TIME"
git -C "$REPO" switch -q -c pr
merges 2 "$BUMP_TIME"
HEAD_SHA=$(git -C "$REPO" rev-parse HEAD)
expect_output() {
    local description=$1 expected=$2
    shift 2
    local actual
    actual=$(cd "$REPO" && bash "$APP_VERSION" "$@" 2>&1 | tr '\n' ' ') || true
    if [[ "$actual" == "$expected" ]]; then
        echo "ok   $description: $actual"
    else
        echo "FAIL $description: expected $expected, got $actual"
        FAILED=1
    fi
}
expect_output "PR outputs" "versionCode=2220102 versionNameSuffix=pr-253-${HEAD_SHA:0:7} " pr 253 master "$HEAD_SHA"
git -C "$REPO" switch -q master
expect_output "master outputs" "versionCode=2220203 versionNameSuffix=master " master
VERSION_CODE_NOW=$((BUMP_TIME + 6 * DAY)) expect_output "nightly outputs, dated in UTC" "versionCode=2220306 versionNameSuffix=nightly-01112026 " nightly
expect_output "release outputs, with the bare version name" "versionCode=2220401 versionNameSuffix=none " release
if (cd "$REPO" && bash "$APP_VERSION" pr 253 >/dev/null 2>&1); then
    echo "FAIL PR outputs without the refs: expected it to fail"
    FAILED=1
else
    echo "ok   PR outputs without the refs: fails"
fi

# Tagging a release candidate, with gh replaced by a stub that logs its calls
STUB=$(mktemp -d)
REPOS+=("$STUB")
cat > "$STUB/gh" <<'STUB'
#!/bin/bash
echo "gh $*" >> "$GH_LOG"
if [[ "$1 $2" == "release view" ]]; then
    [[ -n "${GH_RELEASE_EXISTS:-}" ]]
    exit
fi
if [[ "$1 $2" == "release create" ]]; then
    while [[ $# -gt 0 ]]; do
        [[ "$1" == "--notes-file" ]] && sed 's/^/  notes: /' "$2" >> "$GH_LOG"
        shift
    done
fi
exit 0
STUB
chmod +x "$STUB/gh"
export GH_LOG="$STUB/calls"

expect_calls() {
    local description=$1 expected=$2 actual
    actual=$(sed -E 's/--notes-file [^ ]+/--notes-file NOTES/' "$GH_LOG" | tr '\n' '|')
    if [[ "$actual" == "$expected" ]]; then
        echo "ok   $description"
    else
        echo "FAIL $description: expected $expected, got $actual"
        FAILED=1
    fi
}

new_repo
set_version 2.21.0 "$BUMP_TIME"
git -C "$REPO" tag 2.21.0
set_version 2.22.0 "$BUMP_TIME"
commit_at "$BUMP_TIME" "First change (#10)"
SHA=$(git -C "$REPO" rev-parse HEAD)
: > "$GH_LOG"
(cd "$REPO" && PATH="$STUB:$PATH" bash "$TAG_RELEASE" owner/repo 2220401 "$SHA" >/dev/null)
expect_calls "first candidate: tags rc1 and creates the release" \
"gh api repos/owner/repo/git/refs -f ref=refs/tags/2.22.0-rc1 -f sha=$SHA|gh release view 2.22.0 --repo owner/repo|gh release create 2.22.0 --repo owner/repo --target $SHA --title 2.22.0 --notes-file NOTES|  notes: - Version bump to 2.22.0|  notes: - First change (https://github.com/owner/repo/pull/10)|"

git -C "$REPO" tag 2.22.0-rc1
git -C "$REPO" tag 2.22.0
commit_at "$BUMP_TIME" "Fix found in testing (#11)"
SHA=$(git -C "$REPO" rev-parse HEAD)
: > "$GH_LOG"
(cd "$REPO" && GH_RELEASE_EXISTS=1 PATH="$STUB:$PATH" bash "$TAG_RELEASE" owner/repo 2220402 "$SHA" >/dev/null)
expect_calls "second candidate: tags rc2 and replaces the release, with notes since the previous version" \
"gh api repos/owner/repo/git/refs -f ref=refs/tags/2.22.0-rc2 -f sha=$SHA|gh release view 2.22.0 --repo owner/repo|gh release delete 2.22.0 --repo owner/repo --cleanup-tag --yes|gh release create 2.22.0 --repo owner/repo --target $SHA --title 2.22.0 --notes-file NOTES|  notes: - Version bump to 2.22.0|  notes: - First change (https://github.com/owner/repo/pull/10)|  notes: - Fix found in testing (https://github.com/owner/repo/pull/11)|"

for code in 2220306 2230401 2220400; do
    : > "$GH_LOG"
    if (cd "$REPO" && PATH="$STUB:$PATH" bash "$TAG_RELEASE" owner/repo "$code" "$SHA" >/dev/null 2>&1); then
        echo "FAIL tagging $code, not a 2.22.0 release candidate: expected it to fail"
        FAILED=1
    elif [[ -s "$GH_LOG" ]]; then
        echo "FAIL tagging $code, not a 2.22.0 release candidate: called gh before failing"
        FAILED=1
    else
        echo "ok   tagging $code, not a 2.22.0 release candidate: fails before calling gh"
    fi
done

exit $FAILED
