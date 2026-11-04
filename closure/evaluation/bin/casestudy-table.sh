#!/bin/bash
# The full per-program table (thesis Table 6.2): LoC, checking time,
# explicit endorses, runtime bytecode, and RSE sites, for SCIF's seven
# existing case studies plus the two closure programs of this thesis.
#
# Regenerates every case study's Solidity with the current compiler,
# compiles it with one fixed solc configuration (optimizer + via-IR,
# runs=200 -- see ../casestudy-build/foundry.toml), and counts.
#
# Row rule: a row sums the deployable contracts DEFINED IN that case's
# own source file.  Imported files that are themselves cases (ERC-20
# under Uniswap) count in their own row only, which matches the SCIF
# paper's Table 1 (its Uniswap row is the exchange contract alone).
#
# The checking-time column is compile-time.sh's job, not this script's.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$HERE/../../.."
BUILD="$HERE/../casestudy-build"
FR="$HERE/../../foundry-run"

cd "$ROOT"

# case-file -> the .scif sources it needs generated (main file first)
gen() {
  for f in IERC20 IExchange IPolyCallee IECCUtils IEthCrossChainData \
           ERC20_depmap Dexible KoET EthCrossChainManager HODLWallet \
           SysEscrow Uniswap_ERC20; do
    ./scif -c "test/contracts/applications/$f.scif" -o "$BUILD/src/$f.sol" >/dev/null 2>&1
    [ -s "$BUILD/src/$f.sol" ] || { echo "generation failed: $f" >&2; exit 1; }
  done
}

gen
( cd "$BUILD" && ~/.foundry/bin/forge build >/dev/null 2>&1 ) || { echo "solc failed" >&2; exit 1; }
( cd "$FR"    && ~/.foundry/bin/forge build >/dev/null 2>&1 ) || { echo "solc failed (foundry-run)" >&2; exit 1; }

python3 - "$BUILD" "$FR" "$ROOT" <<'PY'
import json, re, sys, os, glob
build, fr, root = sys.argv[1:4]

# program -> (label, scif source, contracts in its own file, foundry out dir)
CASES = [
    ("ERC-20",                    "test/contracts/applications/ERC20_depmap.scif",          ["ERC20"],        build),
    ("Uniswap (with ERC-20)",     "test/contracts/applications/Uniswap_ERC20.scif",         ["Uniswap"],      build),
    ("Dexible swap",              "test/contracts/applications/Dexible.scif",               ["Dexible"],      build),
    ("KoET",                      "test/contracts/applications/KoET.scif",                  ["KoET"],         build),
    ("Poly Network",              "test/contracts/applications/EthCrossChainManager.scif",  ["EthCrossChainManager"], build),
    ("HODLWallet",                "test/contracts/applications/HODLWallet.scif",            ["HODLWallet"],   build),
    ("SysEscrow",                 "test/contracts/applications/SysEscrow.scif",             ["SysEscrow"],    build),
    ("Closure aggregator",  "test/contracts/basic/IRouterClosureCompile.scif",        ["User", "Aggregator", "UniswapRouter"], fr),
    ("Direct call, no relay",     "closure/foundry-run/scif/DirectCall.scif",               ["UserDirect"],   fr),
]

def runtime_size(outdir, name):
    for p in glob.glob(os.path.join(outdir, "out", "**", f"{name}.json"), recursive=True):
        code = json.load(open(p)).get("deployedBytecode", {}).get("object", "")
        return (len(code) - 2) // 2 if code.startswith("0x") else len(code) // 2
    raise SystemExit(f"no artifact for {name}")

# RSE site = compiler-inserted assert using a security primitive:
# reentrancy locks, sender/trust checks, closure label checks, closure
# abort check.  assert(true) (statically discharged, stripped by the
# optimizer), internal-dispatch guards (this==sender with assert(false)
# arms), and source-level application asserts are not RSE.
def rse(paths):
    n = 0
    for path in paths:
        for x in re.findall(r'assert\(([^;]*)\)\s*;', open(path).read()):
            if re.search(r'\b(bypassLocks|acquireLock|releaseLock)\(', x): n += 1
            elif re.search(r'\btrusts(_[0-9a-f]+)?\(', x): n += 1
            elif re.search(r'\bactsFor\(', x): n += 1
            elif 'closureOk' in x: n += 1
    return n

# generated .sol carrying each program's contracts
GENSOL = {
    "Closure aggregator":    [os.path.join(fr, "src", "full.sol")],
    "Direct call, no relay": [os.path.join(fr, "src", "direct.sol")],
}

hdr = f"{'program':<26}{'LoC':>5}{'endorses':>9}{'bytecode(B)':>12}{'RSE':>5}"
print(hdr); print("-" * len(hdr))
for label, src, contracts, outdir in CASES:
    srcpath = os.path.join(root, src)
    loc = open(srcpath).read().count("\n")  # wc -l, matching compile-time.sh
    endorses = len(re.findall(r'\bendorse\b', open(srcpath).read()))
    size = sum(runtime_size(outdir, c) for c in contracts)
    sols = GENSOL.get(label) or [os.path.join(build, "src",
             os.path.basename(src).replace(".scif", ".sol"))]
    print(f"{label:<26}{loc:>5}{endorses:>9}{size:>12,}{rse(sols):>5}")

print()
print("bytecode: runtime (deployed) bytecode, solc via Foundry, optimizer on,")
print("runs=200, via-IR; one row sums the contracts defined in the case's file.")
PY
