# Versioning System

This directory contains the configuration and logic for the automated versioning system used in the Everlog project for both **Android** and **iOS**.

## Overview

The versioning system generates unique, incremental versioning identifiers (`versionCode` and `versionName`) based on base values, the build type, and the commit history. This ensures that every build has a distinct and traceable version across both platforms.

## Components

- **`version.txt`**: Contains the base version name (e.g., `2.9.0`). This is used as the foundation for the `versionName`.
- **`version_code.txt`**: Contains the base version code (e.g., `2090000`). This represents the major/minor versioning of the application, and goes up by hand with each version bump.
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
  - `4`: Release (the `--release` flag), so a release is always above the nightlies and `master` builds of its version
- **`SS` (Sequence)**: The number of commits since the last git tag, modulo 100 (`00` to `99`).

Play only accepts uploads with a higher version code than it already has. Nightlies (Play internal track) and releases (Play production) have their own type digits, so they never compete, and every build of the next version (a higher base) is above them all. The staging and `master` builds go to Firebase App Distribution, which accepts any version code.

**The sequence wraps after 100 commits** without a tag: the 100th commit gives `00`, and Play rejects that nightly as lower than the ones it has. A release resets the count, as it tags `master`.

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

A hotfix is a patch release (`2.21.0` → `2.21.1`) that ships only fixes on top of a released version. It always goes through a `hotfix-<version>` branch off the release tag, whether or not `master` has moved on, so `master` never has to be frozen.

The patch is the `P` digit of the version code, so `version_code.txt` goes up by `1000` (`2210000` → `2211000`). Every build of the patch then has a higher version code than any build of the release it fixes, and a lower one than the next minor version.

1. **Branch** from the release tag and push it: `git switch -c hotfix-2.21.1 2.21.0`, `git push -u origin hotfix-2.21.1`.
2. **Version bump** in a PR into the hotfix branch: `version.txt` to `2.21.1`, `version_code.txt` to `2211000`.
3. **Fixes** in PRs into the hotfix branch, from branches named after their tickets as usual. `staging-check.yml` runs on PRs into `hotfix-*` branches like it does for `master`. When a fix is already on `master`, cherry-pick it.
4. **Release:** run the release job on the hotfix branch (`release-check.yml` allows `master` and `hotfix-*`). It tags `2.21.1` on the hotfix branch, and the GitHub release lists only the commits since `2.21.0`.
5. **Roll out** once Play's pre-launch report on the production upload is clean. The nightly only runs on `master`, so this is the hotfix's only run on Play's test devices. If the release being fixed hasn't been rolled out yet, roll out the patch instead and skip it.
6. **Merge back** into `master` with a PR from the hotfix branch, squash merged like any other, so the next release has the fixes. If `master` is already on the next minor version, keep `master`'s side of `version.txt` and `version_code.txt` in the conflict. Delete the hotfix branch afterwards: the tag keeps the released commit.

The squash merge leaves the `2.21.1` tag off `master`'s history, so the next minor release's GitHub notes count from `2.21.0` and list the hotfix's commits again. They're part of that release too.

If the crash is behind a Remote Config flag, turning the flag off stops it straight away while the hotfix is on its way.
