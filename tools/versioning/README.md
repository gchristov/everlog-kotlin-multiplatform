# Versioning System

This directory contains the configuration and logic for the automated versioning system used in the Everlog project for both **Android** and **iOS**.

## Overview

The versioning system generates unique, incremental versioning identifiers (`versionCode` and `versionName`) based on base values, the build type, and the commit history. This ensures that every build has a distinct and traceable version across both platforms.

## Components

- **`version.txt`**: Contains the base version name (e.g., `2.9.0`). This is used as the foundation for the `versionName`.
- **`version_code.txt`**: Contains the base version code (e.g., `2090000`). This represents the major/minor versioning of the application. **This base version is automatically incremented weekly** via CI/CD tasks to ensure continuous progression.
- **`version_code.sh`**: The script responsible for calculating the final version code.

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
