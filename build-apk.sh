#!/usr/bin/env bash
set -euo pipefail

echo "=== Building SecureChat Release APK ==="

if [ ! -f release.keystore ]; then
    echo ""
    echo "No keystore found! Run ./generate-keystore.sh first."
    echo "Then update local.properties with keystore config."
    echo ""
    echo "For testing without a keystore (unsigned debug build):"
    echo "  ./gradlew assembleDebug"
    echo ""
    exit 1
fi

echo "Building signed release APK..."
./gradlew assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK" ]; then
    echo ""
    echo "=== SUCCESS ==="
    echo "Signed APK: $APK"
    ls -lh "$APK"
else
    echo "Build finished but APK not found at $APK"
    exit 1
fi
