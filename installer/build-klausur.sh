#!/bin/bash
# Builds the exam edition (Klausurversion) for Windows as ONE program file:
#   out/klausur/Filius-RBZ-Klausur.exe
# from the program folder out/Filius-RBZ-Wirtschaft-Kiel (run package.sh first). Same program, but:
#   * filius-rbz.jar contains filius/modern/klausur.flag: no learning path at all (filius.modern.Klausur),
#     also not from project files with learning path progress
#   * no lernpfad/ folder, no examples, no Filius introduction (PDF), no readme files
#   * help pages without the learning path and without the tip boxes (tools/klausur-hilfe.py)
#   * licence texts in lizenzen/ with a note where the sources are (GPL)
# The program folder of the exam edition stays in out/klausur/Filius-RBZ-Klausur for tests.
# NSIS 3: makensis from PATH or $NSISDIR/linux/makensis.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
DIST="$ROOT/out/Filius-RBZ-Wirtschaft-Kiel"
OUT="$ROOT/out/klausur"
K="$OUT/Filius-RBZ-Klausur"
if command -v makensis >/dev/null 2>&1; then MAKENSIS=makensis; else MAKENSIS="${NSISDIR:?makensis fehlt}/linux/makensis"; fi
STAND="$(grep -o 'STAND = "[^"]*"' "$ROOT/src/java/filius/gui/InfoDialog.java" | cut -d'"' -f2)"
test -f "$DIST/filius-rbz.jar" || { echo "fehlt: $DIST (zuerst package.sh)"; exit 1; }
test -d "$DIST/java-runtime" || { echo "fehlt: $DIST/java-runtime (zuerst tools/runtimes.sh)"; exit 1; }

rm -rf "$OUT" && mkdir -p "$K"
for f in filius-rbz.jar filius.jar Filius.ico; do cp "$DIST/$f" "$K/"; done
for d in config hilfe img java-runtime lib tmpl lizenzen; do cp -r "$DIST/$d" "$K/"; done
# the sources stay available on GitHub (note below); the zip would only make the exe bigger
rm -f "$K/lizenzen/quellcode-filius-rbz.zip"

# switch: the flag file inside filius-rbz.jar
python3 - "$K/filius-rbz.jar" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1], 'a', zipfile.ZIP_DEFLATED) as z:
    z.writestr('filius/modern/klausur.flag', 'Klausurversion ohne Lernpfad / exam edition without learning path\n')
PY

python3 "$ROOT/tools/klausur-hilfe.py" "$K/hilfe"

cat > "$K/lizenzen/HINWEIS-Klausurversion.txt" <<EOF
Filius (RBZ Wirtschaft Kiel Version) - Klausurversion, Stand $STAND

Gleiches Programm wie Filius (RBZ Wirtschaft Kiel Version), aber ohne Lernpfad, ohne Beispiele und ohne
Tipps in der Hilfe. Freie Software unter der GNU General Public License (GPL v2 oder v3, siehe GPLv2.txt und
GPLv3.txt), auf Basis von Filius 2.9.3 (https://www.lernsoftware-filius.de).
Quellcode: https://github.com/DemandGames/filius-rbz

Exam edition of Filius (RBZ Wirtschaft Kiel Version): the same program without the learning path, examples
and tips in the help. Free software under the GNU GPL (v2 or v3). Sources: https://github.com/DemandGames/filius-rbz
EOF
sed -i 's/\r$//; s/$/\r/' "$K/lizenzen/HINWEIS-Klausurversion.txt"

# build id: version and checksum of the jar, so a changed build never uses an old unpacked folder
KENNUNG="$(echo "$STAND" | tr -cd '0-9')-$(sha256sum "$K/filius-rbz.jar" | cut -c1-8)"
"$MAKENSIS" -V2 -INPUTCHARSET UTF8 -DQUELLE="$K" -DKENNUNG="$KENNUNG" -DSTAND="$STAND" -DICON="$K/Filius.ico" \
  -DOUT="$OUT/Filius-RBZ-Klausur.exe" "$HERE/klausur.nsi"
ls -la "$OUT/Filius-RBZ-Klausur.exe"
echo "Kennung $KENNUNG"
