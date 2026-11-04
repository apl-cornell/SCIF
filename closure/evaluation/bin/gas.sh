#!/bin/bash
# Gas for one end-to-end swap, four ways (SCIF paper Table 2 style:
# the same operation implemented in Solidity and in SCIF).
#
# EVM gas is deterministic for identical calls, so a single measured
# run is exact; the paper averaged 100 runs because its harness had
# run-to-run variation.
#
# Scenarios (all move 100 tokens in, 99 out, through a router), a full
# 2x2 of {Solidity, SCIF} x {relay, direct} so the cost of information
# flow control and the cost of relaying separate:
#   raw         hand-written Solidity, untyped calldata relay (unsafe)
#   closure     SCIF closure relay      (this thesis)
#   raw-direct  hand-written Solidity, no relay
#   direct      SCIF direct call, no relay
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE/../../foundry-run"
out=$(~/.foundry/bin/forge test --gas-report 2>&1)
val() { echo "$out" | grep -E "^\| $1 " | awk -F'|' '{gsub(/ /,"",$4); print $4}' | head -1; }
raw=$(val doSwap); rawd=$(val doSwapDirect)
clo=$(val "doSwap_e7daf0[0-9a-f]*"); dir=$(val "doSwapDirect_a1422e[0-9a-f]*")
reg=$(val "registerTrust_52ac52[0-9a-f]*")
[ -z "$clo" ] && clo=$(echo "$out" | grep -oE "doSwap_[0-9a-f]+ *\| *[0-9]+" | grep -oE "[0-9]+$" | head -1)
[ -z "$dir" ] && dir=$(echo "$out" | grep -oE "doSwapDirect_[0-9a-f]+ *\| *[0-9]+" | grep -oE "[0-9]+$" | head -1)
[ -z "$reg" ] && reg=$(echo "$out" | grep -oE "registerTrust_[0-9a-f]+ *\| *[0-9]+" | grep -oE "[0-9]+$" | head -1)
python3 - "$raw" "$clo" "$rawd" "$dir" "$reg" <<'PY'
import sys
raw, clo, rawd, dirv, reg = (int(x) if x else 0 for x in sys.argv[1:6])
print(f"{'scenario':<40}{'gas':>10}{'vs raw':>10}")
print(f"{'raw Solidity relay (unsafe)':<40}{raw:>10,}{'--':>10}")
print(f"{'SCIF closure relay (this thesis)':<40}{clo:>10,}{(clo-raw)/raw*100:>+9.1f}%")
print(f"{'raw Solidity direct call (no relay)':<40}{rawd:>10,}{(rawd-raw)/raw*100:>+9.1f}%")
print(f"{'SCIF direct call (no relay)':<40}{dirv:>10,}{(dirv-raw)/raw*100:>+9.1f}%")
print()
print(f"IFC alone   (SCIF direct  vs raw direct):{(dirv-rawd)/rawd*100:>+9.1f}%")
print(f"relay alone (raw relay    vs raw direct):{(raw-rawd)/rawd*100:>+9.1f}%")
print(f"secured relay (closure    vs raw relay): {(clo-raw)/raw*100:>+9.1f}%")
print()
print(f"one-time: delegation registration{reg:>15,} gas (once per user/relay pair)")
PY
