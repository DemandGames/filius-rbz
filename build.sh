#!/bin/bash
# Builds Filius (RBZ Wirtschaft Kiel Version): filius-rbz.jar (the layer on top of an unchanged Filius 2.9.3)
# and filius-rbz-fx.jar (the browser engine, built against OpenJFX).
#   third_party/filius-2.9.3/   original Filius 2.9.3 program files (filius.jar, lib/, ...), unchanged
#   src/                        sources of this version
#   .deps/                      FlatLaf and OpenJFX, fetched by tools/deps.sh
#   out/build/                  result
# Needs a JDK 21 (javac, jar).
set -euo pipefail
export JAVA_TOOL_OPTIONS=
ROOT="$(cd "$(dirname "$0")" && pwd)"
BASE="$ROOT/third_party/filius-2.9.3"
DEPS="${DEPS:-$ROOT/.deps}"
OUT="$ROOT/out/build"
FLATLAF="$DEPS/flatlaf-3.7.2.jar"
test -f "$FLATLAF" || { echo "fehlt: $FLATLAF (zuerst tools/deps.sh)"; exit 1; }
rm -rf "$OUT" && mkdir -p "$OUT/classes"

CP="$BASE/filius.jar:$FLATLAF"
for j in "$BASE"/lib/*.jar; do CP="$CP:$j"; done

find "$ROOT/src/java" -name "*.java" | sort > "$OUT/sources.txt"
# stop on any compile error (javac may still write some classes, which would hide the failure)
if ! javac -nowarn --release 17 -encoding UTF-8 -Xlint:none -cp "$CP" -d "$OUT/classes" @"$OUT/sources.txt" > "$OUT/javac.log" 2>&1; then
  grep -v "^Note:" "$OUT/javac.log"; echo "compile failed"; exit 1
fi
test -f "$OUT/classes/filius/modern/ModernFilius.class"

cp -r "$ROOT/src/resources/." "$OUT/classes/"
cat > "$OUT/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
Main-Class: filius.modern.ModernFilius
Class-Path: lib/flatlaf-3.7.2.jar filius.jar
Implementation-Title: Filius (RBZ Wirtschaft Kiel Version)
Implementation-Version: 2.9.3-modern.1
EOF
(cd "$OUT/classes" && jar cfm "$OUT/filius-rbz.jar" "$OUT/MANIFEST.MF" .)
echo "built $OUT/filius-rbz.jar ($(du -h "$OUT/filius-rbz.jar" | cut -f1))"

# browser engine (WebKit from JavaFX): the view class lives in its own jar next to the JavaFX jars and
# is loaded at run time as an automatic module (see filius.modern.web.FxSupport)
FXDIR="$DEPS/javafx-win"
if [ -d "$FXDIR" ]; then
  mkdir -p "$OUT/fxclasses"
  FXCP="$OUT/classes:$CP"
  for j in "$FXDIR"/*.jar; do FXCP="$FXCP:$j"; done
  find "$ROOT/src/fx" -name "*.java" | sort > "$OUT/fxsources.txt"
  javac -nowarn --release 21 -encoding UTF-8 -Xlint:none -cp "$FXCP" -d "$OUT/fxclasses" @"$OUT/fxsources.txt"
  printf "Manifest-Version: 1.0\nAutomatic-Module-Name: filius.modern.fx\nImplementation-Title: Filius (RBZ Wirtschaft Kiel Version) browser engine\n" > "$OUT/FX-MANIFEST.MF"
  jar cfm "$OUT/filius-rbz-fx.jar" "$OUT/FX-MANIFEST.MF" -C "$OUT/fxclasses" .
  echo "built $OUT/filius-rbz-fx.jar"
fi
