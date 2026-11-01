#!/bin/bash
# Assembles the ready-to-run program folder out/Filius-RBZ-Wirtschaft-Kiel (Windows, with Java runtime and
# browser engine) from the original Filius 2.9.3 files and this version, plus licences and the sources.
# The original program files are copied unchanged. Result: out/Filius-RBZ-Wirtschaft-Kiel/ and
# out/Filius-RBZ-Wirtschaft-Kiel.zip. Run tools/deps.sh and tools/runtimes.sh first (without the Windows
# runtime the folder is built without the browser engine).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
BASE="$ROOT/third_party/filius-2.9.3"
DEPS="${DEPS:-$ROOT/.deps}"
B="$ROOT/out/build"
DIST="$ROOT/out/Filius-RBZ-Wirtschaft-Kiel"

"$ROOT/build.sh"
rm -rf "$DIST" && mkdir -p "$DIST/lib" "$DIST/lizenzen"
# the help (hilfe/) comes completely from dist-files, so no outdated pictures remain
for f in filius.jar config tmpl img lib Changelog.md Einfuehrung_Filius.pdf; do
  if [ -e "$BASE/$f" ]; then cp -r "$BASE/$f" "$DIST/"; fi
done
# this version runs in German and English only
rm -f "$DIST/config/Desktop_fr_FR.txt" "$DIST/tmpl/"*fr_FR*
sed -i '/locale=fr_FR/d; s/are en_GB and de_DE\./are de_DE and en_GB./' "$DIST/config/filius.ini"
cp -r "$BASE/beispiele" "$DIST/beispiele"
# the modem is not part of Filius (RBZ Wirtschaft Kiel Version), so its example is left out
rm -f "$DIST/beispiele/modem_beispiel.fls"
cp "$DEPS/flatlaf-3.7.2.jar" "$DIST/lib/"
cp "$B/filius-rbz.jar" "$DIST/"

# browser engine: Java runtime for Windows (with the modules JavaFX needs) and JavaFX WebKit
if [ -d "$DEPS/rt-win-x64" ] && [ -f "$B/filius-rbz-fx.jar" ]; then
  cp -r "$DEPS/rt-win-x64" "$DIST/java-runtime"
  mkdir -p "$DIST/lib/javafx"
  for m in base graphics controls media web swing; do
    cp "$DEPS/javafx-win/javafx-$m-"*"-win.jar" "$DIST/lib/javafx/"
  done
  cp "$B/filius-rbz-fx.jar" "$DIST/lib/javafx/"
else
  echo "Hinweis: ohne Browser-Engine gebaut (tools/runtimes.sh fehlt)"
fi
cp -r "$ROOT/dist-files/." "$DIST/"
rm -f "$DIST/hilfe/"*fr_FR* "$DIST/hilfe/gfx/"*fr_FR*

# learning paths: chapter texts (editable by teachers), pictures and start networks, German and English;
# advanced path in lernpfad/, standard path in lernpfad/standard/
for p in "" "standard/"; do
  for l in "" "en/"; do
    mkdir -p "$DIST/lernpfad/$p$l"
    cp "$ROOT/lernpfad/$p$l"kapitel-*.txt "$DIST/lernpfad/$p$l"
    for d in bilder start; do
      if [ -d "$ROOT/lernpfad/$p$l$d" ]; then cp -r "$ROOT/lernpfad/$p$l$d" "$DIST/lernpfad/$p$l"; fi
    done
  done
done
chmod +x "$DIST/filius-rbz.sh"

# licences
cp "$BASE/GPLv2.txt" "$BASE/GPLv3.txt" "$DIST/lizenzen/"
cp "$ROOT/third_party/licenses/FlatLaf-Apache-2.0.txt" "$DIST/lizenzen/"
if [ -d "$DIST/java-runtime" ]; then
  cp "$ROOT/dist-files-engine/OpenJFX-und-Java-Laufzeit.txt" "$DIST/lizenzen/"
fi
cp "$ROOT/src/resources/filius/modern/fonts/OFL-"*.txt "$DIST/lizenzen/"

# sources of this version as one zip next to the licences (GPL). The learning paths are in the program
# folder anyway, the original Filius 2.9.3 is available from its authors; everything is also at
# https://github.com/DemandGames/filius-rbz
python3 - "$ROOT" "$DIST/lizenzen/quellcode-filius-rbz.zip" <<'PY'
import os, sys, zipfile
root, ziel = sys.argv[1], sys.argv[2]
teile = ['src', 'design', 'dist-files', 'dist-files-engine', 'installer', 'tools', '.github',
         'build.sh', 'package.sh', 'BUILD.md', 'README.md', 'LICENSE', 'third_party/licenses',
         'third_party/filius-2.9.3/README.md', 'third_party/filius-2.9.3/SHA256SUMS']
z = zipfile.ZipFile(ziel, 'w', zipfile.ZIP_DEFLATED)
for t in teile:
    p = os.path.join(root, t)
    if os.path.isfile(p):
        z.write(p, os.path.join('filius-rbz', t))
    elif os.path.isdir(p):
        for base, dirs, files in os.walk(p):
            dirs[:] = sorted(d for d in dirs if d != '__pycache__')
            for f in sorted(files):
                q = os.path.join(base, f)
                z.write(q, os.path.join('filius-rbz', os.path.relpath(q, root)))
z.close()
PY

# Windows line endings for files opened on Windows
for f in "$DIST/Filius starten.bat" "$DIST/LIESMICH.txt" "$DIST/README.txt"; do
  sed -i 's/\r$//; s/$/\r/' "$f"
done

(cd "$ROOT/out" && rm -f Filius-RBZ-Wirtschaft-Kiel.zip && zip -qr Filius-RBZ-Wirtschaft-Kiel.zip Filius-RBZ-Wirtschaft-Kiel)
echo "packaged $DIST ($(du -sh "$DIST" | cut -f1)), zip $(du -h "$ROOT/out/Filius-RBZ-Wirtschaft-Kiel.zip" | cut -f1)"
