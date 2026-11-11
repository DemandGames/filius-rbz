#!/bin/bash
# Start test of the exam edition (out/klausur/Filius-RBZ-Klausur, run installer/build-klausur.sh first) on a
# virtual screen. Uses the Java runtime of the Linux package (out/installer/deb, installer/build-deb.sh) or
# the java on PATH. Opens a project file WITH learning path progress (a normal Filius would open the learning
# path) and fails if Filius ended by itself, wrote an exception, did not report the exam edition, or wrote
# the learning path folder. Writes out/starttest-klausur.log and out/starttest-klausur.png.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
K="$ROOT/out/klausur/Filius-RBZ-Klausur"
LOG="$ROOT/out/starttest-klausur.log"
test -f "$K/filius-rbz.jar" || { echo "fehlt: $K (zuerst installer/build-klausur.sh)"; exit 1; }
JAVA="$ROOT/out/installer/deb/opt/filius-rbz/java-runtime/bin/java"
test -x "$JAVA" || JAVA="$(command -v java)"
export JAVA_TOOL_OPTIONS=

# project with progress in the standard learning path (chapter 2 open)
TEST="$(mktemp -d)"
FLS="$TEST/mit lernpfad.fls"
python3 - "$ROOT/lernpfad/standard/start/kapitel-02.fls" "$FLS" <<'PY'
import sys, zipfile
fort = "pfad=standard\nstandard.version=3\nstandard.aktiv=2\n"
for k, s in ((1, 3), (2, 1)):
    fort += "".join(f"standard.k{k}.{n}={v}\n" for n, v in (("seite", s), ("gelesen", "true"), ("erledigt", ""),
             ("praxisFertig", "true"), ("quizFehler", 0), ("fertig", "true" if k == 1 else "false"), ("variante", 0)))
with zipfile.ZipFile(sys.argv[1]) as a, zipfile.ZipFile(sys.argv[2], "w", zipfile.ZIP_DEFLATED) as b:
    for i in a.infolist():
        if i.filename != "projekt/lernpfad.properties":
            b.writestr(i, a.read(i.filename))
    b.writestr("projekt/lernpfad.properties", fort)
PY

cd "$K"
xvfb-run -a -s "-screen 0 1400x900x24" bash -c '
  "$1" -XX:-UsePerfData -Dfilius.speicherort="$5" -jar filius-rbz.jar "$4" > "$2" 2>&1 &
  PID=$!
  sleep 40
  if command -v import > /dev/null; then import -window root "$3" 2> /dev/null; fi
  if kill -0 $PID 2> /dev/null; then kill $PID; wait $PID 2> /dev/null; exit 0; fi
  echo "Filius hat sich vorzeitig beendet"; exit 1
' _ "$JAVA" "$LOG" "$ROOT/out/starttest-klausur.png" "$FLS" "$TEST"
ERG=$?
echo "--- Ausgabe von Filius:"; tail -40 "$LOG"
if grep -E "Exception|^\s+at |Error:" "$LOG" | grep -v "^WARNING" > /dev/null; then
  echo "Fehlermeldungen gefunden"; ERG=1
fi
grep -q "Klausurversion ohne Lernpfad" "$LOG" || { echo "Klausurversion nicht erkannt"; ERG=1; }
if [ -e "$K/lernpfad" ]; then echo "Ordner lernpfad vorhanden"; ERG=1; fi
rm -rf "$TEST"
[ $ERG = 0 ] && echo "Starttest Klausurversion ok"
exit $ERG
