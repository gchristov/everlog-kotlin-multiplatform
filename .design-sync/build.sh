#!/bin/sh
# Builds ./ds-bundle for the Claude Design sync. Off-script tokens-only layout: the design system is
# Compose, so there are no web components, only the tokens, guidelines and preview cards in
# .design-sync/bundle. See .design-sync/NOTES.md.
set -e
cd "$(dirname "$0")/.."
rm -rf ds-bundle
cp -R .design-sync/bundle ds-bundle
cp .design-sync/conventions.md ds-bundle/README.md
{
  echo '/* @ds-bundle: {"namespace":"Everlog","components":[],"sourceHashes":{},"inlinedExternals":[],"builtBy":"cc-design-sync"} */'
  echo 'window.Everlog = window.Everlog || {};'
} > ds-bundle/_ds_bundle.js
echo '{"by":"design-sync-cli"}' > ds-bundle/_ds_needs_recompile
echo '{"componentCount":0,"shape":"package"}' > ds-bundle/.ds-build-meta.json
