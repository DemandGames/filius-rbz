#!/bin/bash
# Filius (RBZ Wirtschaft Kiel Version) - Start fuer Linux (und macOS mit installiertem Java 17 oder neuer)
script_path=$(readlink -f "$0")
installation_path=$(dirname "$script_path")
if [ -x "$installation_path/java-runtime/bin/java" ]; then
  JAVA="$installation_path/java-runtime/bin/java"
else
  JAVA=java
fi
exec "$JAVA" -jar "$installation_path/filius-rbz.jar" "$@"
