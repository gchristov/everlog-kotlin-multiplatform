#!/usr/bin/env bash
# Applies this repository's GitHub settings (merge options and protection of
# the default branch). GitHub can't read settings from a file, so this script
# is the source of truth: edit it, then run it. Safe to re-run.
#
# Needs the GitHub CLI, logged in as a repo admin: `gh auth login`.
# Usage: scripts/github-settings.sh
set -euo pipefail

REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)
BRANCH=master

echo "Merge settings for $REPO"
gh api -X PATCH "repos/$REPO" --silent \
  -F allow_merge_commit=false \
  -F allow_squash_merge=true \
  -F allow_rebase_merge=false \
  -F allow_auto_merge=true \
  -F allow_update_branch=true \
  -F delete_branch_on_merge=true \
  -f squash_merge_commit_title=PR_TITLE \
  -f squash_merge_commit_message=BLANK

# Changes go through a PR with one approval, CI must pass on an up-to-date
# branch, history stays linear, and master can't be force-pushed or deleted.
# Admins aren't bound by the rules. App 15368 is GitHub Actions.
echo "Protection for $BRANCH"
gh api -X PUT "repos/$REPO/branches/$BRANCH/protection" --silent --input - <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "checks": [
      {"context": "build-android", "app_id": 15368},
      {"context": "unit-test", "app_id": 15368},
      {"context": "test-android", "app_id": 15368}
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": true,
    "require_last_push_approval": false
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": false
}
JSON

echo "Done"
