#!/bin/bash
# Compilation time per program (SCIF paper Table 1, "Compilation time" column).
#
# One fresh JVM per program, single run: this is the cold, user-visible
# cost, and it is the closest match to how the paper reported the number.
# JVM startup (~80ms here) is excluded because it is not compiler work.
#
#   ./compile-time.sh            # the paper's case studies + our closure programs
#   ./compile-time.sh <file>...  # specific programs
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
CP="$HERE:$($HERE/classpath.sh)"

run_one() {  # <label> <abs-path-to-scif>
  local label="$1" file="$2"
  # Imports resolve relative to the CWD, so run from the file's directory.
  ( cd "$(dirname "$file")" && java -cp "$CP" CompileTimer 1 "$(basename "$file")" | tail -1 ) \
    | awk -v l="$label" -F'\t' '{printf "%-28s %10s %10s %10s   %s\n", l, $2, $3, $4, $5}'
}

printf "%-28s %10s %10s %10s   %s\n" "program" "regular" "ifc(ms)" "total" "ok"
if [ $# -gt 0 ]; then
  for f in "$@"; do run_one "$(basename "$f")" "$(cd "$(dirname "$f")" && pwd)/$(basename "$f")"; done
  exit 0
fi

A="$ROOT/test/contracts/applications"
# The SCIF paper's case studies (Table 1), re-measured on this machine.
run_one "ERC-20"            "$A/ERC20_depmap.scif"
run_one "Uniswap (w/ERC20)" "$A/Uniswap_ERC20.scif"
run_one "Dexible swap"      "$A/Dexible.scif"
run_one "KoET"              "$A/KoET.scif"
run_one "Poly Network"      "$A/EthCrossChainManager.scif"
run_one "HODLWallet"        "$A/HODLWallet.scif"
run_one "SysEscrow"         "$A/SysEscrow.scif"
echo
# This thesis.
run_one "closure aggregator" "$ROOT/test/contracts/basic/IRouterClosureCompile.scif"
run_one "direct call (base)"  "$ROOT/closure/foundry-run/scif/DirectCall.scif"
