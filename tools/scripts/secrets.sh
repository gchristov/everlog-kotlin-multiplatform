#!/bin/bash
set -e
# Exports required CI environment secrets to local secrets so that the project can use them.
# Should be invoked from the root of the project as all paths are relative.

# App credentials (the >> is intentional to append to a file)
#echo GUARDIAN_API_KEY="$GUARDIAN_API_KEY" >> multiplatform/common/network/secrets.properties
#echo GUARDIAN_API_URL="$GUARDIAN_API_URL" >> multiplatform/common/network/secrets.properties

# Firebase credentials
mkdir -p mobile/src/debug
echo "$GOOGLE_SERVICES_DEV_JSON" > mobile/src/debug/google-services.json
mkdir -p mobile/src/release
echo "$GOOGLE_SERVICES_PROD_JSON" > mobile/src/release/google-services.json

# Keystores
mkdir -p deploy
echo "$KEYSTORE_DEBUG_BASE64" | base64 --decode > deploy/debug.keystore
echo "$KEYSTORE_RELEASE_BASE64" | base64 --decode > deploy/release_key.jks
