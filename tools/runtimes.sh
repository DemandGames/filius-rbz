#!/bin/bash
# Builds the Java runtimes that ship with the packages, with jlink from the JDKs fetched by tools/deps.sh:
#   rt-win-x64          Temurin 21 for Windows (JavaFX comes as separate jars in lib/javafx)
#   rt-linux-amd64      Liberica 21 "full" for Linux, with JavaFX (browser engine)
#   rt-macos-aarch64    Liberica 21 "full" for macOS on Apple chips, with JavaFX
#   rt-macos-amd64      Liberica 21 "full" for macOS on Intel, with JavaFX
# The runtimes for other systems are linked on Linux from the jmods of the target system (same JDK version).
# usage: tools/runtimes.sh [FOLDER]   (default: $DEPS or .deps)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
D="${1:-${DEPS:-$ROOT/.deps}}"
DL="$D/dl"
J="$D/jdk"
export JAVA_TOOL_OPTIONS=
rm -rf "$J" && mkdir -p "$J/temurin-linux" "$J/temurin-win" "$J/liberica-linux" "$J/liberica-mac-aarch64" "$J/liberica-mac-amd64"
tar xzf "$DL/temurin-21.0.8-linux-x64.tar.gz" -C "$J/temurin-linux" --strip-components=1
unzip -q "$DL/temurin-21.0.8-windows-x64.zip" -d "$J/temurin-win-tmp" && mv "$J/temurin-win-tmp"/*/* "$J/temurin-win/" && rm -rf "$J/temurin-win-tmp"
tar xzf "$DL/liberica-21.0.8-linux-amd64-full.tar.gz" -C "$J/liberica-linux" --strip-components=1
tar xzf "$DL/liberica-21.0.8-macos-aarch64-full.tar.gz" -C "$J/liberica-mac-aarch64" --strip-components=1
tar xzf "$DL/liberica-21.0.8-macos-amd64-full.tar.gz" -C "$J/liberica-mac-amd64" --strip-components=1

WIN="$(paste -sd, "$ROOT/tools/module-windows.txt")"
FX="$(paste -sd, "$ROOT/tools/module-javafx.txt")"
OPT="--no-header-files --no-man-pages --compress=zip-6"

rm -rf "$D/rt-win-x64" "$D/rt-linux-amd64" "$D/rt-macos-aarch64" "$D/rt-macos-amd64"
"$J/temurin-linux/bin/jlink" --module-path "$J/temurin-win/jmods" --add-modules "$WIN" --strip-debug $OPT --output "$D/rt-win-x64"
"$J/liberica-linux/bin/jlink" --module-path "$J/liberica-linux/jmods" --add-modules "$FX" --strip-debug $OPT --output "$D/rt-linux-amd64"
# objcopy cannot strip macOS libraries, so only the Java debug attributes are removed there
for a in aarch64 amd64; do
  "$J/liberica-linux/bin/jlink" --module-path "$J/liberica-mac-$a/jmods" --add-modules "$FX" --strip-java-debug-attributes $OPT --output "$D/rt-macos-$a"
done
rm -rf "$J"
du -sh "$D"/rt-*
