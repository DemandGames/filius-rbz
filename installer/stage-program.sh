#!/bin/bash
# Copies the program files for macOS/Linux packages (no Windows runtime, no Windows JavaFX jars, no readme,
# no examples; licences and the source zip are in lizenzen/): stage-program.sh ZIEL
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
DIST="$ROOT/out/Filius-RBZ-Wirtschaft-Kiel"
Z="$1"
test -f "$DIST/filius-rbz.jar" || { echo "missing $DIST (zuerst package.sh)"; exit 1; }
rm -rf "$Z" && mkdir -p "$Z"
for f in filius-rbz.jar filius.jar; do cp "$DIST/$f" "$Z/"; done
for d in config hilfe img lernpfad lib tmpl lizenzen; do cp -r "$DIST/$d" "$Z/"; done
# the browser engine comes from the Java runtime of the package; only the bridge jar stays
rm -f "$Z"/lib/javafx/javafx-*.jar
