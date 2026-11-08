#!/usr/bin/env bash
set -euo pipefail

# Run from anywhere; this script always works from the project root.
cd "$(dirname "$0")/.."

STORE_FILE="${1:-upload-keystore.jks}"

if [ ! -f "$STORE_FILE" ]; then
  echo "Keystore not found: $STORE_FILE" >&2
  exit 1
fi

read -r -p "Key alias [upload]: " KEY_ALIAS
KEY_ALIAS="${KEY_ALIAS:-upload}"

read -r -s -p "Keystore password: " STORE_PASSWORD
echo
read -r -s -p "Key password (press Enter if same as keystore password): " KEY_PASSWORD
echo
KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

STORE_FILE_ABS="$(cd "$(dirname "$STORE_FILE")" && pwd)/$(basename "$STORE_FILE")"

umask 077
{
  printf 'storeFile=%s\n' "$STORE_FILE_ABS"
  printf 'storePassword=%s\n' "$STORE_PASSWORD"
  printf 'keyAlias=%s\n' "$KEY_ALIAS"
  printf 'keyPassword=%s\n' "$KEY_PASSWORD"
} > keystore.properties

echo "Wrote keystore.properties for alias '$KEY_ALIAS'."
echo "Next: ./gradlew :app:bundleRelease --console=plain"
