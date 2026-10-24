# Versioning System

This directory contains the configuration and logic for the automated versioning system used in the Everlog project for both **Android** and **iOS**.

## Overview

The versioning system generates unique, incremental versioning identifiers (`versionCode` and `versionName`) based on base values, the build type, and the commit history. This ensures that every build has a distinct and traceable version across both platforms.

## Components

- **`version.txt`**: Contains the base version name (e.g., `2.9.0`). This is used as the foundation for the `versionName`.
- **`version_code.txt`**: Contains the base version code (e.g., `2090000`). This represents the major/minor versioning of the application. **This base version is automatically incremented weekly** via CI/CD tasks to ensure continuous progression.
- **`version_code.sh`**: The script responsible for calculating the final version code.
- **`release_notes.sh`**: Prints GitHub release notes (one bullet per commit since the previous tag, with its PR link). Used by `release-check.yml`, which tags the released commit with `version.txt` and creates the GitHub release after a successful production deploy. The version bump PR for the next release is still done by hand.

## Version Name

The `versionName` is derived from `version.txt`. 
On CI, a suffix is appended to the base version via the `ciVersionNameSuffix` project property:
- **Staging (PRs)**: `2.9.0-pr-<number>-<short-sha>`
- **Master Branch**: `2.9.0-master`
- **Nightly Builds**: `2.9.0-nightly-ddmmyyyy`
- **Local Builds**: `2.9.0-local` (unless a suffix is manually provided via `ciVersionNameSuffix`)

## Version Code Structure

The generated version code follows a specific pattern: `MmmPTSS`

- **`MmmP`**: The base version extracted from `version_code.txt`.
- **`T` (Type Index)**: Represents the build environment:
  - `1`: Staging
  - `2`: Nightly
  - `3`: Master
- **`SS` (Sequence)**: A two-digit incremental number based on the number of commits since the last git tag.
  - For standard builds, this ranges from `00` to `79`.
  - For release builds (using the `--release` flag), this ranges from `80` to `99`.

## Usage

The version code is typically generated during the CI/CD process or local builds by executing the `version_code.sh` script with the appropriate environment argument:

```bash
./tools/versioning/version_code.sh <staging|nightly|master> [--release]
```

When building via Gradle, the suffix can be passed as:
```bash
./gradlew assembleDebug -PciVersionNameSuffix=my-suffix
```

## Hotfixes

A hotfix is a patch release (`2.21.0` → `2.21.1`) that ships only a fix on top of a released version. The patch is the `P` digit of the version code, so `version_code.txt` goes up by `1000` (`2210000` → `2211000`). Every build of the patch then has a higher version code than any build of the release it fixes, and a lower one than the next minor version.

`release-check.yml` only runs on `master` and releases whatever `master` holds, so which steps to take depends on whether `master` has moved on since the release tag (`git log --oneline <tag>..master`).

### `master` still holds the released version

`version.txt` still says the released version, e.g. `2.21.0`, and there's nothing on `master` after its tag that shouldn't ship.

1. Merge the fix into `master`.
2. Merge a version bump PR: `version.txt` to `2.21.1`, `version_code.txt` to `2211000`.
3. Merge nothing else until the patch is out, or it ships too.
4. Let a nightly pass with the fix, then run the release job as usual. It tags `2.21.1`, and the GitHub release lists only the commits since `2.21.0`.
5. If the release being fixed hasn't been rolled out on Play yet, roll out the patch instead and skip it.

### `master` has moved on

`master` has commits after the release tag that aren't ready to ship, or `version.txt` is already bumped for the next minor version, e.g. `2.22.0`.

The release job can't ship a patch from here: running it would release all of `master`. The choices:

- **Ship the fix with the next minor version** if it can wait. Merge it into `master` as usual.
- **Hotfix branch:** branch from the release tag (`git switch -c hotfix-2.21.1 2.21.0`), cherry-pick the fix, and bump `version.txt` to `2.21.1` and `version_code.txt` to `2211000` there. `release-check.yml`'s `require-master` job must first allow that branch (e.g. `refs/heads/hotfix-*`), since it refuses everything but `master` today. Run the release job on the hotfix branch, then merge the fix (not the version bump) into `master` too, so the next minor version has it.
- **Turn the feature off** with its Remote Config flag, if the crash is behind one, until either of the above is out.
