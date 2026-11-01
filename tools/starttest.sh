#!/bin/bash
# Start test of the Linux package (out/installer/deb, run installer/build-deb.sh first): starts Filius with
# its own Java runtime on a virtual screen (xvfb-run), waits, and fails if Filius ended by itself or wrote an
# exception. Writes out/starttest.log and out/starttest.png (if ImageMagick "import" is there).
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
P="$ROOT/out/installer/deb/opt/filius-rbz"
LOG="$ROOT/out/starttest.log"
test -x "$P/java-runtime/bin/java" || { echo "fehlt: $P (zuerst installer/build-deb.sh)"; exit 1; }
export JAVA_TOOL_OPTIONS=
cd "$P"
HOME_TEST="$(mktemp -d)"
xvfb-run -a -s "-screen 0 1280x900x24" bash -c '
  HOME="$1" ./java-runtime/bin/java -XX:-UsePerfData -jar filius-rbz.jar > "$2" 2>&1 &
  PID=$!
  sleep 40
  if command -v import > /dev/null; then import -window root "$3" 2> /dev/null; fi
  if kill -0 $PID 2> /dev/null; then kill $PID; wait $PID 2> /dev/null; exit 0; fi
  echo "Filius hat sich vorzeitig beendet"; exit 1
' _ "$HOME_TEST" "$LOG" "$ROOT/out/starttest.png"
ERG=$?
echo "--- Ausgabe von Filius:"; tail -40 "$LOG"
if grep -E "Exception|^\s+at |Error:" "$LOG" | grep -v "^WARNING" > /dev/null; then
  echo "Fehlermeldungen gefunden"; ERG=1
fi
[ $ERG = 0 ] && echo "Starttest ok: Filius läuft"
exit $ERG
