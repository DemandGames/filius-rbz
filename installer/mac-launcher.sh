#!/bin/sh
# Filius (RBZ Wirtschaft Kiel Version) - starter of the macOS app (__NAME__ build).
# Runs the Java runtime inside the app with filius-rbz.jar; the working folder is Contents/app.
HERE="$(cd "$(dirname "$0")/.." && pwd)"
if [ "__UNAME__" = "arm64" ] && [ "$(uname -m)" != "arm64" ]; then
  osascript -e 'display dialog "Diese Version von Filius ist für Macs mit Apple-Chip (M1 bis M4). Für einen Mac mit Intel-Prozessor bitte die Intel-Version laden." buttons {"OK"} default button 1 with title "Filius (RBZ Wirtschaft Kiel Version)" with icon stop' > /dev/null 2>&1
  exit 1
fi
cd "$HERE/app" || exit 1
exec "$HERE/runtime/bin/java" -XX:-UsePerfData \
  -Xdock:name="Filius RBZ" -Xdock:icon="$HERE/Resources/Filius.icns" \
  -Dapple.awt.application.name="Filius RBZ" -Dapple.awt.application.appearance=system \
  -jar "$HERE/app/filius-rbz.jar" "$@"
