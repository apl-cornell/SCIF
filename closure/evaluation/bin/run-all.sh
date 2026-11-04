#!/bin/bash
# Run every measurement and refresh results/.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
R="$HERE/../results"
mkdir -p "$R"
echo "[1/5] compile time";   "$HERE/compile-time.sh"     > "$R/compile-time.txt"     2>&1
echo "[2/5] bytecode size";  "$HERE/bytecode-size.sh"    > "$R/bytecode-size.txt"    2>&1
echo "[3/5] RSE sites";      "$HERE/rse-count.sh"        > "$R/rse-count.txt"        2>&1
echo "[4/5] gas";            "$HERE/gas.sh"              > "$R/gas.txt"              2>&1
echo "[5/5] case studies";   "$HERE/casestudy-table.sh"  > "$R/casestudy-table.txt"  2>&1
echo "done -> $R"
