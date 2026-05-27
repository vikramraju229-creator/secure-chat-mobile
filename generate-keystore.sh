#!/usr/bin/env bash
set -euo pipefail

# SECURITY: Read passwords from environment variables with fallback to prompt.
# NEVER hardcode passwords in version-controlled scripts.
KEYSTORE_FILE="${KEYSTORE_FILE:-release.keystore}"
KEY_ALIAS="${KEY_ALIAS:-securechat}"

if [ -z "${KEYSTORE_PASS:-}" ]; then
  echo -n "Enter keystore password: "
  read -rs KEYSTORE_PASS
  echo
fi

if [ -z "${KEY_PASS:-}" ]; then
  echo -n "Enter key password: "
  read -rs KEY_PASS
  echo
fi

echo "Generating release keystore..."
keytool -genkey -v -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$KEYSTORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=SecureChat, OU=Development, O=SecureChat, L=Unknown, ST=Unknown, C=US"

echo ""
echo "Keystore generated: $KEYSTORE_FILE"
echo ""
echo "Add these to local.properties (or set as CI secrets):"
echo "  keystore.path=$(pwd)/$KEYSTORE_FILE"
echo "  keystore.password=$KEYSTORE_PASS"
echo "  key.alias=$KEY_ALIAS"
echo "  key.password=$KEY_PASS"
echo ""
echo "SECURITY: For production, use strong unique passwords and back up the keystore file!"
