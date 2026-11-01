#!/bin/bash
# Builds the Windows starter and installers from the program folder out/Filius-RBZ-Wirtschaft-Kiel
# (run package.sh first):
#   installer/build-setup.sh launcher   -> out/installer/launcher/Filius RBZ.exe
#   installer/build-setup.sh setup      -> out/installer/Filius-RBZ-Setup.exe (current user) and
#                                          out/installer/Filius-RBZ-Setup-AlleBenutzer.exe (all users, admin)
#   installer/build-setup.sh            -> both
# For "setup" the starter is taken from $LAUNCHER (e.g. the signed one) or from out/installer/launcher.
# Only the program goes into the installers: no readme, no examples, no Filius 2.9.3 documents; the licence
# texts and the sources (one zip, required by the GPL) are in the subfolder lizenzen.
# NSIS 3: makensis from PATH (e.g. Debian/Ubuntu package nsis) or $NSISDIR/linux/makensis.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
DIST="$ROOT/out/Filius-RBZ-Wirtschaft-Kiel"
OUT="$ROOT/out/installer"
STAGE="$OUT/stage"
MODE="${1:-alles}"
if command -v makensis >/dev/null 2>&1; then MAKENSIS=makensis; else MAKENSIS="${NSISDIR:?makensis fehlt}/linux/makensis"; fi
STAND="$(grep -o 'STAND = "[^"]*"' "$ROOT/src/java/filius/gui/InfoDialog.java" | cut -d'"' -f2)"
test -f "$DIST/filius-rbz.jar" || { echo "fehlt: $DIST (zuerst package.sh)"; exit 1; }
mkdir -p "$OUT"

if [ "$MODE" = launcher ] || [ "$MODE" = alles ]; then
  mkdir -p "$OUT/launcher"
  "$MAKENSIS" -V2 -INPUTCHARSET UTF8 -DOUT="$OUT/launcher/Filius RBZ.exe" -DICON="$DIST/Filius.ico" -DSTAND="$STAND" "$HERE/launcher.nsi"
  ls -la "$OUT/launcher/Filius RBZ.exe"
fi

if [ "$MODE" = setup ] || [ "$MODE" = alles ]; then
  LAUNCHER="${LAUNCHER:-$OUT/launcher/Filius RBZ.exe}"
  test -f "$LAUNCHER" || { echo "fehlt: $LAUNCHER (zuerst: build-setup.sh launcher)"; exit 1; }
  rm -rf "$STAGE" "$OUT/bilder" && mkdir -p "$STAGE" "$OUT/bilder"
  for f in filius-rbz.jar filius.jar Filius.ico; do cp "$DIST/$f" "$STAGE/"; done
  for d in config hilfe img java-runtime lernpfad lib tmpl lizenzen; do cp -r "$DIST/$d" "$STAGE/"; done
  cp "$LAUNCHER" "$STAGE/Filius RBZ.exe"
  python3 "$HERE/bilder.py" "$OUT/bilder" > /dev/null
  "$MAKENSIS" -V2 -INPUTCHARSET UTF8 -DSTAGE="$STAGE" -DBILDER="$OUT/bilder" -DSTAND="$STAND" -DOUT="$OUT/Filius-RBZ-Setup.exe" "$HERE/setup.nsi"
  "$MAKENSIS" -V2 -INPUTCHARSET UTF8 -DALLE -DSTAGE="$STAGE" -DBILDER="$OUT/bilder" -DSTAND="$STAND" -DOUT="$OUT/Filius-RBZ-Setup-AlleBenutzer.exe" "$HERE/setup.nsi"
  ls -la "$OUT/Filius-RBZ-Setup.exe" "$OUT/Filius-RBZ-Setup-AlleBenutzer.exe"
fi
