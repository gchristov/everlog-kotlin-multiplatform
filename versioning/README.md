# Versioning System

This directory contains the configuration and logic for the automated versioning system used in the Everlog project for both **Android** and **iOS**.

## Overview

The versioning system generates a unique, incremental versioning identifier (e.g., `versionCode` on Android) based on a base version, the build type, and the commit history. This ensures that every build has a distinct and traceable version number across both platforms.

## Components

- **`version.txt`**: Contains the base version code (e.g., `2090000`). This represents the major/minor versioning of the application. **This base version is automatically incremented weekly** via CI/CD tasks to ensure continuous progression.
- **`version_code.sh`**: The script responsible for calculating the final version code.

## Version Code Structure

The generated version code follows a specific pattern: `MmmPTSS`

- **`MmmP`**: The base version extracted from `version.txt`.
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
./version_code.sh <staging|nightly|master> [--release]
```

This system allows for a high degree of automation while maintaining a clear hierarchy and traceability for all builds across different environments.
