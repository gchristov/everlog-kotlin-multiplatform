#!/bin/bash
set -e

# Usage: ./tools/firebase/remoteconfig/remote_config.sh <credentials_file> <list [text] | get <name> | set <name> [--value V] [--type T] [--description D]>
# Example: ./tools/firebase/remoteconfig/remote_config.sh admin-credentials-dev.json get history_v2
#          ./tools/firebase/remoteconfig/remote_config.sh admin-credentials-prod.json set history_v2 --value true
# The credentials file is one of tools/firebase's admin keys, and picks the project (dev or prod).

CREDENTIALS=$1

if [ -z "$CREDENTIALS" ] || [ -z "$2" ]; then
  echo "❌ Error: Credentials file and command are required."
  echo "Usage: ./tools/firebase/remoteconfig/remote_config.sh <credentials_file> <list | get | set> ..."
  exit 1
fi

if [ ! -f "tools/firebase/$CREDENTIALS" ]; then
  echo "❌ Error: Credentials file 'tools/firebase/$CREDENTIALS' does not exist."
  exit 1
fi
shift

docker build -q -t everlog-remote-config tools/firebase/remoteconfig > /dev/null

docker run --rm \
  -v "$(pwd)/tools/firebase/$CREDENTIALS:/$CREDENTIALS:ro" \
  everlog-remote-config --credentials "/$CREDENTIALS" "$@"
