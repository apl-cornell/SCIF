#!/bin/bash
# Bytecode size, using the SCIF paper's method: the length of the
# compiled bytecode hex string divided by two.  (The paper's artifact
# ships these as .hex files under artifact/code; the byte counts there
# reproduce its Table 1 exactly.)
#
# Sizes here come from the Foundry build of closure/foundry-run, so the
# SCIF-generated code and the hand-written Solidity baseline are
# compiled by the SAME solc with the SAME settings -- unlike the paper,
# whose Solidity baselines had to use each contract's original (much
# older) compiler version.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
FR="$HERE/../../foundry-run"
cd "$FR"
~/.foundry/bin/forge build > /dev/null 2>&1
printf "%-16s %12s %12s %14s   %s\n" "contract" "runtime_B" "initcode_B" "EIP-170_used" "source"
for j in out/*.sol/*.json; do
  name=$(basename "$j" .json)
  case "$name" in *Test|Vm|IERC20|IUniswapRouter*|IAggregator) continue;; esac
  python3 - "$j" "$name" <<'PY'
import json, sys
j, name = sys.argv[1], sys.argv[2]
d = json.load(open(j))
rt = d.get('deployedBytecode', {}).get('object', '') or ''
ic = d.get('bytecode', {}).get('object', '') or ''
rt = rt[2:] if rt.startswith('0x') else rt
ic = ic[2:] if ic.startswith('0x') else ic
if not rt: sys.exit()
src = 'SCIF' if 'full.sol' in j or 'direct.sol' in j else ('Solidity' if 'baseline_raw' in j else '-')
print(f"{name:<16} {len(rt)//2:>12,} {len(ic)//2:>12,} {len(rt)//2/24576*100:>13.1f}%   {src}")
PY
done
echo
echo "EIP-170 limit: 24,576 bytes of runtime bytecode per contract."
