#!/bin/bash
# Downloads the third-party parts that are not kept in this repository and checks each file against its
# SHA-256 (a wrong file stops the build):
#   FlatLaf 3.7.2 (look and feel, Apache 2.0)                      Maven Central
#   OpenJFX 23.0.2 for Windows (browser engine WebKit, GPL+CE)     Maven Central
#   Eclipse Temurin 21.0.8+9 for Windows and Linux (Java runtime)  GitHub, adoptium/temurin21-binaries
#   Liberica JDK 21.0.8+12 "full" for Linux and macOS (with JavaFX) GitHub, bell-sw/Liberica
# usage: tools/deps.sh [FOLDER]      (default: $DEPS or .deps in the repository)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
D="${1:-${DEPS:-$ROOT/.deps}}"
DL="$D/dl"
mkdir -p "$DL" "$D/javafx-win"

MAVEN=https://repo1.maven.org/maven2
TEMURIN=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9
LIBERICA=https://github.com/bell-sw/Liberica/releases/download/21.0.8%2B12

# name | url | sha256
LIST="
flatlaf-3.7.2.jar|$MAVEN/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar|917aff3963c88d797d0fd9b9ccbd70f7681c101df9d11c59e2bc7a3a6c0fabf4
javafx-base-23.0.2-win.jar|$MAVEN/org/openjfx/javafx-base/23.0.2/javafx-base-23.0.2-win.jar|72f8958b4a552fca14ab3628c672f399aa68bbe6b4cd45107fe174b4670c7cb6
javafx-controls-23.0.2-win.jar|$MAVEN/org/openjfx/javafx-controls/23.0.2/javafx-controls-23.0.2-win.jar|874daf48b0ec01e4eb0caf24cdb85b32d951999837f8e1c285f6d1e1e34870ba
javafx-graphics-23.0.2-win.jar|$MAVEN/org/openjfx/javafx-graphics/23.0.2/javafx-graphics-23.0.2-win.jar|6b5b3041f2735508a8aad23c5795e745d5bf78a0a6aa920bae43ab41ec22d8c2
javafx-media-23.0.2-win.jar|$MAVEN/org/openjfx/javafx-media/23.0.2/javafx-media-23.0.2-win.jar|eb2ae99397333094172ad73c7689b6a48ae59b2b7a0d80d571521214f4a770ec
javafx-swing-23.0.2-win.jar|$MAVEN/org/openjfx/javafx-swing/23.0.2/javafx-swing-23.0.2-win.jar|875d14a5c2cf8ce04e5c48c39b5c9a3ff1d794820b8a473ec010af1c631afcb3
javafx-web-23.0.2-win.jar|$MAVEN/org/openjfx/javafx-web/23.0.2/javafx-web-23.0.2-win.jar|a18008ae0d3df1b8046a99d44f761dd01cce279ae1539a7e4685263aec2ea5fc
temurin-21.0.8-windows-x64.zip|$TEMURIN/OpenJDK21U-jdk_x64_windows_hotspot_21.0.8_9.zip|5dbb74a4edda94c2aae97a1e30f448db0ad9023a9466fe31a8c68377d9dfbac4
temurin-21.0.8-linux-x64.tar.gz|$TEMURIN/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz|f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219
liberica-21.0.8-linux-amd64-full.tar.gz|$LIBERICA/bellsoft-jdk21.0.8%2B12-linux-amd64-full.tar.gz|8a373aa9c18ceb34dcbad36a31043a5eb88add54cf817d6fc9fe71452e779a89
liberica-21.0.8-macos-aarch64-full.tar.gz|$LIBERICA/bellsoft-jdk21.0.8%2B12-macos-aarch64-full.tar.gz|6714989c24b387e237d4e066ce1a47bb4f45fc9a1d1b444d16a16b69e6534824
liberica-21.0.8-macos-amd64-full.tar.gz|$LIBERICA/bellsoft-jdk21.0.8%2B12-macos-amd64-full.tar.gz|e7844ee0d41fd6247f6efe5e3fe3e17c469dec7fbdb43aaade264daeb77d0fcc
"

pruefe() { echo "$2  $1" | sha256sum -c --quiet - >/dev/null 2>&1; }

FEHLER=0
while IFS='|' read -r name url sha; do
  [ -n "$name" ] || continue
  f="$DL/$name"
  if [ -f "$f" ] && pruefe "$f" "$sha"; then echo "ok (vorhanden)  $name"; continue; fi
  echo "lade            $name"
  curl -fsSL --retry 4 --retry-delay 5 -o "$f.part" "$url"
  if ! pruefe "$f.part" "$sha"; then
    echo "FEHLER: SHA-256 von $name stimmt nicht (erwartet $sha, erhalten $(sha256sum "$f.part" | cut -d' ' -f1))" >&2
    rm -f "$f.part"; FEHLER=1; continue
  fi
  mv "$f.part" "$f"
  echo "ok              $name"
done <<< "$LIST"
[ "$FEHLER" = 0 ] || exit 1

cp "$DL/flatlaf-3.7.2.jar" "$D/"
cp "$DL"/javafx-*-win.jar "$D/javafx-win/"
echo "Abhängigkeiten in $D"
