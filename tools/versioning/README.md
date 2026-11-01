# Versioning System

This directory contains the configuration and logic for the automated versioning system used in the Everlog project for both **Android** and **iOS**.

## Overview

The versioning system generates the `versionCode` and `versionName` of every build from the version in `version.txt`, the build type and that type's own sequence, so the version can be read off the code and each build of a type gets the next number.

## Components

- **`version.txt`**: The version (e.g. `2.22.0`), the base of both the `versionName` and the version code. A version bump changes only this file, by hand in a PR.
- **`version_code.sh`**: Calculates the version code for a build type.
- **`app_version.sh`**: Prints a CI build's version code and version name suffix, as the `compute-app-version` action's outputs.
- **`tag_release.sh`**: Tags an uploaded release candidate and creates (or replaces) the version's GitHub release, run by `release-check.yml`.
- **`release_notes.sh`**: Prints GitHub release notes (one bullet per commit since the previous version's tag, with its PR link). Used by `release-check.yml`, which tags each release candidate and creates the GitHub release after a successful production deploy.

## Version Name

The `versionName` is derived from `version.txt`. 
On CI, a suffix is appended to the base version via the `ciVersionNameSuffix` project property:
- **PRs**: `2.9.0-pr-<number>-<short-sha>`
- **Master Branch**: `2.9.0-master`
- **Nightly Builds**: `2.9.0-nightly-ddmmyyyy`
- **Local Builds**: `2.9.0-local` (unless a suffix is manually provided via `ciVersionNameSuffix`)

## Version Code Structure

The version code is `MmmPTSS`, 7 digits, e.g. `2220402` for 2.22.0's second release candidate:

- **`MmmP`**: The version from `version.txt`: major, minor (two digits) and patch (`2.22.0` → `2220`).
- **`T`**: The build type, in the order its builds supersede each other:
  - `1`: PR (`staging-check.yml` on a pull request, to App Distribution)
  - `2`: `master` (`staging-check.yml` on a push to `master`, to App Distribution)
  - `3`: Nightly (`nightly-check.yml`, every weekday, to the Play internal track)
  - `4`: Release or hotfix (`release-check.yml`, to Play production as a draft)
- **`SS`**: The build's number in its type's own sequence:

| Type | `SS` | Restarts |
| --- | --- | --- |
| PR | The commit's number within the PR: `01`, `02`… | With each PR |
| `master` | Merges into `master` since the version bump: `01`, `02`… | With each version bump |
| Nightly | Days since the version bump, in UTC: `01` for the nightly the night after | With each version bump |
| Release | The release candidate number: `01`, `02`… | With each version |

The version bump is the commit that set the current version in `version.txt`. Commits that touch the file without changing the version don't count.

Within a version, a PR build is below every `master` build, which is below every nightly, which is below every release candidate. Every build of the next version is above all of them, and a hotfix raises the patch digit, so it's above the release it fixes. Play only accepts a code it hasn't had before, above the ones on the same track. App Distribution accepts any code, and Android installs a build over one with the same or a lower code.

### Release candidates

Release runs queue rather than overlap, so each one numbers its candidate after the previous run has tagged. Each run uploads the next candidate, which replaces the earlier candidate's draft on Play, and tags it `<version>-rc<N>`. The next run counts those tags. The GitHub release and the `<version>` tag point at the latest candidate: a later one replaces them. Publish the candidate that passed testing by rolling out the draft in Play.

### Limits

- **99 per sequence.** PR and `master` builds stay at `99` after 99 commits or merges, which only App Distribution sees. A nightly 100 days after the version bump fails with a message to release or bump the version, and so does a 100th release candidate.
- **One nightly per day.** A second nightly on the same day (a manual run) has the same code, and Play rejects it.
- **Nine hotfixes per minor version**, as the patch is one digit, and a minor version up to 99.
- **Different PRs share codes**: the first commit of every PR is `…101`. The version name tells them apart.
- **Installing a PR build over a `master` build** needs an uninstall, as the PR build's code is lower.
- **A rerun of a release whose upload went through** has the same candidate number if tagging failed, and Play rejects it. Tag the uploaded candidate (`<version>-rc<N>`) by hand, then run it again.
- **Hotfixes on a test phone**: Play gives internal testers the highest code they can get, so once the next version's nightlies are up, a hotfix only reaches a test phone through App Distribution or internal app sharing.

## Usage

CI runs the script through the `compute-app-version` action:

```bash
./tools/versioning/version_code.sh pr <base-ref> <head-ref>
./tools/versioning/version_code.sh <master|nightly|release>
```

`version_code_test.sh` checks it, `app_version.sh`, `tag_release.sh` (with `gh` stubbed) and `release_notes.sh` against throwaway repos (`bash tools/versioning/version_code_test.sh`). CI runs it with the unit tests.

Local builds use the version's base code (`2220000` for `2.22.0`), below every CI build of it. The version name suffix can be passed as:
```bash
./gradlew assembleDebug -PciVersionNameSuffix=my-suffix
```

## Hotfixes

A hotfix is a patch release (`2.21.0` → `2.21.1`) that ships only fixes on top of a released version. It always goes through a `hotfix-<version>` branch off the release tag, whether or not `master` has moved on, so `master` never has to be frozen.

The patch is the `P` digit of the version code (`2210…` → `2211…`), so every build of the patch has a higher version code than any build of the release it fixes, and a lower one than the next minor version.

1. **Branch** from the release tag and push it: `git switch -c hotfix-2.21.1 2.21.0`, `git push -u origin hotfix-2.21.1`.
2. **Version bump** in a PR into the hotfix branch: `version.txt` to `2.21.1`.
3. **Fixes** in PRs into the hotfix branch, from branches named after their tickets as usual. `staging-check.yml` runs on PRs into `hotfix-*` branches like it does for `master`. When a fix is already on `master`, cherry-pick it.
4. **Release:** run the release job on the hotfix branch (`release-check.yml` allows `master` and `hotfix-*`). It tags `2.21.1-rc1` and `2.21.1` on the hotfix branch, and the GitHub release lists only the commits since `2.21.0`. Run it again for another candidate.
5. **Roll out** once Play's pre-launch report on the production upload is clean. The nightly only runs on `master`, so this is the hotfix's only run on Play's test devices. If the release being fixed hasn't been rolled out yet, roll out the patch instead and skip it.
6. **Merge back** into `master` with a PR from the hotfix branch, squash merged like any other, so the next release has the fixes. If `master` is already on the next minor version, keep `master`'s side of `version.txt` in the conflict. Delete the hotfix branch afterwards: the tag keeps the released commit.

The squash merge leaves the `2.21.1` tag off `master`'s history, so the next minor release's GitHub notes count from `2.21.0` and list the hotfix's commits again. They're part of that release too.

If the crash is behind a Remote Config flag, turning the flag off stops it straight away while the hotfix is on its way.
