#!/bin/bash
# Builds the macOS apps: out/installer/Filius-RBZ-<version>-macOS-AppleSilicon.zip and ...-macOS-Intel.zip.
# Each zip holds "Filius RBZ.app" with the program and a Java runtime (Liberica JDK 21 "full", jlink) that
# contains the browser engine (JavaFX WebKit). The app is not signed: on first start macOS asks for
# permission (System Settings > Privacy & Security > Open Anyway).
#   RUNTIMES: folder with rt-macos-aarch64 and rt-macos-amd64 from tools/runtimes.sh (default .deps)
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
RUNTIMES="${RUNTIMES:-${DEPS:-$ROOT/.deps}}"
OUT="$ROOT/out/installer"
STAND="$(grep -o 'STAND = "[^"]*"' "$ROOT/src/java/filius/gui/InfoDialog.java" | cut -d'"' -f2)"
VERSION="2.9.3"
STANDNR="$(echo "$STAND" | awk -F. '{print $3 $2 $1}')"   # 21.10.2026 -> 20261021
mkdir -p "$OUT"
python3 "$HERE/macicon.py" "$OUT/Filius.icns"

for arch in aarch64 amd64; do
  case $arch in aarch64) name="AppleSilicon"; uname="arm64";; amd64) name="Intel"; uname="x86_64";; esac
  APP="$OUT/mac-$arch/Filius RBZ.app"
  rm -rf "$OUT/mac-$arch" && mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
  "$HERE/stage-program.sh" "$APP/Contents/app"
  cp -r "$RUNTIMES/rt-macos-$arch" "$APP/Contents/runtime"
  cp "$OUT/Filius.icns" "$APP/Contents/Resources/Filius.icns"
  cp "$OUT/Filius.icns" "$APP/Contents/Resources/Projekt.icns"
  sed -e "s/__VERSION__/$VERSION/g; s/__STAND__/$STAND/g; s/__STANDNR__/$STANDNR/g" "$HERE/mac-Info.plist" > "$APP/Contents/Info.plist"
  sed -e "s/__UNAME__/$uname/g; s/__NAME__/$name/g" "$HERE/mac-launcher.sh" > "$APP/Contents/MacOS/filius"
  chmod 755 "$APP/Contents/MacOS/filius"
  chmod 755 "$APP/Contents/runtime/bin/"* "$APP/Contents/runtime/lib/jspawnhelper" 2>/dev/null || true
  printf 'APPL????' > "$APP/Contents/PkgInfo"
  ZIP="$OUT/Filius-RBZ-$VERSION-macOS-$name.zip"
  rm -f "$ZIP"
  (cd "$OUT/mac-$arch" && zip -qr -y -X "$ZIP" "Filius RBZ.app")
  ls -la "$ZIP" | awk '{print $5, $9}'
done
