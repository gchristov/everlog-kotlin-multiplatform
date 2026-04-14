#!/bin/bash
set -e
# Exports required CI environment secrets to local secrets so that the project can use them.
# Should be invoked from the root of the project as all paths are relative.

# App credentials (the >> is intentional to append to a file)
echo E2E_TEST_USER_EMAIL="$E2E_TEST_USER_EMAIL" >> mobile/secrets.properties
echo E2E_TEST_USER_PASSWORD="$E2E_TEST_USER_PASSWORD" >> mobile/secrets.properties

# Firebase credentials
mkdir -p mobile/src/debug
echo "$GOOGLE_SERVICES_DEV_JSON" > mobile/src/debug/google-services.json
mkdir -p mobile/src/release
echo "$GOOGLE_SERVICES_PROD_JSON" > mobile/src/release/google-services.json

# Keystores
mkdir -p mobile/src/debug
echo "$KEYSTORE_DEBUG_BASE64" | base64 --decode > mobile/src/debug/debug.keystore
mkdir -p mobile/src/release
echo "$KEYSTORE_RELEASE_BASE64" | base64 --decode > mobile/src/release/release_key.jks
