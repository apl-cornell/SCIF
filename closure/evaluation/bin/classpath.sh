#!/bin/bash
# Resolve the SCIF compiler's runtime classpath (gradle is the source of truth).
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CACHE="$ROOT/closure/evaluation/results/.classpath"
if [ ! -s "$CACHE" ] || [ "$ROOT/build.gradle" -nt "$CACHE" ]; then
  (cd "$ROOT" && ./gradlew printCp -q 2>/dev/null | tail -1 > "$CACHE")
fi
cat "$CACHE"
