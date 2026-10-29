#!/bin/bash
set -e
# Exports required CI environment secrets to local secrets so that the project can use them.
# Should be invoked from the root of the project as all paths are relative.

# App credentials (the >> is intentional to append to a file)
echo E2E_TEST_USER_EMAIL="$E2E_TEST_USER_EMAIL" >> android/app/secrets.properties
echo E2E_TEST_USER_PASSWORD="$E2E_TEST_USER_PASSWORD" >> android/app/secrets.properties

# Firebase credentials
mkdir -p android/app/src/debug
echo "$GOOGLE_SERVICES_DEV_JSON" > android/app/src/debug/google-services.json
mkdir -p android/app/src/release
echo "$GOOGLE_SERVICES_PROD_JSON" > android/app/src/release/google-services.json

# Google credentials
mkdir -p android/app/src/debug
echo "$GOOGLE_DEPLOYMENT_KEY_DEV_JSON" > android/app/src/debug/google-deployment-key.json
mkdir -p android/app/src/release
echo "$GOOGLE_DEPLOYMENT_KEY_PROD_JSON" > android/app/src/release/google-deployment-key.json

# Keystores
mkdir -p android/app/src/debug
echo "$KEYSTORE_DEBUG_BASE64" | base64 --decode > android/app/src/debug/debug.keystore
mkdir -p android/app/src/release
echo "$KEYSTORE_RELEASE_BASE64" | base64 --decode > android/app/src/release/release_key.jks
echo KEYSTORE_RELEASE_PASSWORD="$KEYSTORE_RELEASE_PASSWORD" >> android/app/secrets.properties
echo KEYSTORE_RELEASE_ALIAS="$KEYSTORE_RELEASE_ALIAS" >> android/app/secrets.properties
echo KEYSTORE_RELEASE_ALIAS_PASSWORD="$KEYSTORE_RELEASE_ALIAS_PASSWORD" >> android/app/secrets.properties
