# Closure evaluation — methodology, scripts, and results

Everything measured for the thesis's evaluation chapter, with the code that produced it. Re-run with `./bin/run-all.sh`; results land in `results/`.

The metrics follow the SCIF paper's own evaluation (OOPSLA'25, §6, Tables 1 and 2) so the numbers can be read next to it: compilation time, bytecode size, run-time security enforcement (RSE) sites, and gas per operation. The paper's artifact is in `artifact/`; its `code/*.hex` files are the compiled bytecode whose byte counts reproduce its Table 1 exactly, which is how its size metric is defined (hex string length ÷ 2).

## Requirements

- The SCIF compiler built (`./gradlew classes` at the repo root).
- Foundry (`forge`) for the on-chain measurements: `curl -L https://foundry.paradigm.xyz | bash && foundryup`.
- Java 26 (the repo's toolchain pin; see the note at the end).

## What is measured, and how

### 1. Compilation time — `bin/compile-time.sh`

Front-end wall time per program: parse and preprocess, regular typecheck, then information-flow typecheck. Both typecheck phases hand their constraints to SHErrLoc, which is where the paper reported most time was spent.

One fresh JVM per program, one run each. That is the cold, user-visible cost and the closest match to the paper's Table 1 column. JVM startup itself (~80 ms) is outside the timed region because it is not compiler work.

Driven by `bin/CompileTimer.java`, which calls the compiler API directly (`Preprocessor.preprocess` → `TypeChecker.regularTypecheck` → `TypeChecker.ifcTypecheck`), the same way the JUnit suite does, and prints whether both phases returned success.

> **Why not the `scif` CLI.** Two defects make it unusable for this measurement (both archived in `../IMPLEMENTATION.md`, neither affecting these results). Imports resolve against the working directory rather than the importing file, so a case study can only be compiled from its own directory; and from any directory other than the repository root, `-c` then hits a path-key mismatch in codegen (`SCIF.java:200`, map built from the path as written, queried by absolute path) and returns 0 without a diagnostic or an output file. Typechecking itself is unaffected — it succeeds, and only codegen is skipped — which is why the harness gets clean numbers by calling the API directly.

### 2. Bytecode size — `bin/bytecode-size.sh`

The paper's method: length of the compiled bytecode hex ÷ 2. Taken from the Foundry build artifacts, reporting both the runtime bytecode (what is stored on chain, and what EIP-170 limits to 24,576 bytes) and the initcode.

Unlike the paper, the SCIF-generated code and the hand-written Solidity baseline here are compiled by the **same solc with the same settings**, so the comparison is not confounded by compiler version. (The paper's Solidity baselines had to use each contract's original, much older pragma.)

**Configuration: one solc, optimizer on, runs=200, via-IR** (`foundry.toml` in both build dirs). Every size and gas number in the chapter uses it. Three reasons: it is what production contracts deploy with; Poly Network's generated code does not compile without via-IR (legacy codegen hits "stack too deep"); and it is evidently what the paper used — under it, our regenerated ERC-20 (2,534 B) and Uniswap (12,201 B) land within 0.3% of the paper's Table 1 (2,539 / 12,232).

### 3. RSE sites — `bin/rse-count.sh`

Static count of `assert(...)` statements whose condition uses a security primitive, split by mechanism: reentrancy locks, the normal entry's sender check (`trusts`), the closure entry's label check (`actsFor`), and the closure invoke's abort check. Library-internal sanity asserts (array lengths, bound masks) are not security enforcement and are excluded.

Every row except the last is Solidity **emitted by the SCIF compiler**; the `variant` column names the role each contract plays in the closure flow. The last row is the **hand-written** unsafe baseline, kept for contrast.

### 4. Gas — `bin/gas.sh`

One end-to-end swap (100 tokens in, 99 out, through a router), implemented four ways, measured with `forge test --gas-report`. EVM gas is deterministic for identical calls, so one measured run is exact; the paper averaged 100 runs because its harness had run-to-run variation.

The four scenarios are the full 2×2 of {Solidity, SCIF} × {relay, direct}:

| scenario | what it is | why |
|---|---|---|
| `raw` | hand-written Solidity, untyped calldata relay | the unsafe idiom closures replace — the Dexible pattern |
| `closure` | SCIF closure relay | this thesis |
| `raw-direct` | hand-written Solidity, no relay (`RawUserDirect`) | completes the square |
| `direct` | SCIF direct call, no relay | with `raw-direct`, isolates the price of IFC |

Only the full square identifies the decomposition: `direct` vs `raw-direct` is IFC alone (same architecture, language changes), `raw` vs `raw-direct` is the relay hop alone (same language, architecture changes). With only three cells, "the overhead is relaying, not IFC" would rest on a comparison that crosses both dimensions at once.

### 5. The full per-program table — `bin/casestudy-table.sh`

The thesis's Table 6.2, replicating the columns of the paper's Table 1: LoC, checking time, explicit endorses, runtime bytecode, and RSE sites, for the paper's seven case studies **regenerated through the current compiler** plus this thesis's two programs. The generated Solidity lives in `casestudy-build/` and compiles with the same solc configuration as everything else.

Row rule: a row sums the deployable contracts defined in that case's own file. Uniswap's row is the exchange contract alone (ERC-20 has its own row), matching the paper; the aggregator's row is its three contracts together.

RSE counting, refined for the old-style case studies: an RSE site is a compiler-inserted `assert` over a security primitive (locks, `trusts`, `actsFor`, `closureOk`). Not counted: `assert(true)` (statically discharged, stripped by the optimizer), the `this == msg.sender` / `assert(false)` internal-dispatch guards, and asserts lowered from source-level application checks. Sanity check: this classifier gives Dexible 0, exactly the paper's value for it.

A Solidity-implementation column (the paper's "Solidity bytecode size") was collected but **left out of the thesis table**: its numbers mix provenances — the artifact's `code/*-sol.hex` for the paper's cases (measured at each contract's original pragma, evidently unoptimized: HODLWallet 1,991 and SysEscrow 2,439 come out *larger* than their SCIF versions) against our own solc for the aggregator's baseline — and two cells are missing (Dexible, Poly Network, as in the paper). The data stays in the results table below.

**On baselines.** The SCIF paper compares a SCIF contract against the original Solidity contract it reimplements. No such pair exists here: the old implementation has no forwardable pending call, so there is nothing to reimplement. Both baselines were therefore written for this evaluation — `foundry-run/src/baseline_raw.sol` (the unsafe Solidity idiom) and `foundry-run/scif/DirectCall.scif` (the same swap without a relay).

## Files

```
bin/CompileTimer.java   front-end timing harness (compiler API, not the CLI)
bin/classpath.sh        resolves the compiler classpath via gradle
bin/compile-time.sh     metric 1
bin/bytecode-size.sh    metric 2
bin/rse-count.sh        metric 3
bin/gas.sh              metric 4
bin/casestudy-table.sh  metric 5 (regenerates casestudy-build/src)
bin/run-all.sh          all of them, into results/
casestudy-build/        the seven paper case studies, compiler-regenerated
results/                measurement output (regenerated, safe to delete)
```

The on-chain harness lives in `../foundry-run`: `src/full.sol` (SCIF closure output), `src/direct.sol` (SCIF direct-call output), `src/baseline_raw.sol` (Solidity baseline), `test/Run.t.sol` (scenarios), `regen.sh` (recompile the SCIF sources).

## Results (2026-08-01, MacBook Pro, Apple M3, 16 GB RAM, JDK 26, solc 0.8.35 via Foundry 1.7.1, optimizer runs=200 + via-IR)

### Compilation time

| program | regular (ms) | IFC (ms) | total (ms) |
|---|---|---|---|
| ERC-20 | 271.9 | 0.5 | 272.5 |
| Uniswap (w/ ERC-20) | 239.7 | 0.4 | 240.1 |
| Dexible swap | 232.5 | 0.3 | 232.8 |
| KoET | 234.3 | 0.3 | 234.7 |
| Poly Network | 235.1 | 0.3 | 235.4 |
| HODLWallet | 230.2 | 0.3 | 230.5 |
| SysEscrow | 234.4 | 0.3 | 234.7 |
| **closure aggregator** | **234.7** | **0.3** | **235.0** |
| direct call (baseline) | 232.8 | 0.3 | 233.1 |

All seven of the paper's case studies and both of ours land in the same 230–275 ms band; run-to-run spread on that fixed cost is about ±10%, so only the shape of the table is meaningful, not its last digits.

Two things to say about this table.

**SHErrLoc has become dramatically faster since the paper.** The paper reported 0.13 s to 113 s for these same case studies; every one of them now checks in about a quarter of a second. Uniswap, its most expensive case at 113 s, is now 0.24 s.

**At this point the cost is fixed, not per-program.** A second program checked in the same JVM costs 4.3 ms, and in steady state Uniswap (281 lines) takes 7.9 ms while Dexible (30 lines) takes 2.3 ms. The ~250 ms is one-time initialization — class loading, built-in contracts, solver setup — paid once per compiler invocation. Closure programs sit inside the same band as everything else: adding closures does not change the shape of the cost.

Failure is the expensive case, not success: a program that violates an IFC constraint takes ~260 ms in the IFC phase alone, because SHErrLoc then searches for the best error explanation. That is the behavior the paper described, and it only runs when there is an error to report.

### Bytecode size

All numbers below are under the chapter-wide configuration (optimizer, runs=200, via-IR); the optimizer roughly halves every contract relative to the earlier unoptimized measurements, without changing the shape of the comparison.

| contract | runtime (B) | initcode (B) | EIP-170 used | source |
|---|---|---|---|---|
| RawDexible | 635 | 661 | 2.6% | hand-written Solidity |
| RawRouter | 461 | 487 | 1.9% | hand-written Solidity |
| RawUser | 540 | 566 | 2.2% | hand-written Solidity |
| UserDirect | 658 | 676 | 2.7% | SCIF output, no closure |
| UniswapRouter | 1,639 | 1,657 | 6.7% | SCIF output + closures |
| Aggregator | 1,822 | 1,840 | 7.4% | SCIF output + closures |
| User | 2,278 | 2,296 | 9.3% | SCIF output + closures |

Deployment gas tracks size directly: storing runtime code costs 200 gas per byte; deploying `User` costs 545,641 gas in total, of which 455,600 is the code deposit for its 2,278 runtime bytes.

The growth over the hand-written baseline is the injected runtime library (five fixed functions) and one extra entry point per `closurable` method. Both are **per-contract fixed costs**: they do not grow with how many closures a contract creates or invokes. The largest generated contract uses 9.3% of the EIP-170 limit.

### The full per-program table (thesis Table 6.2)

| program | LoC | time (s) | endorses | bytecode (B) | Solidity (B) | RSE |
|---|---|---|---|---|---|---|
| ERC-20 | 91 | 0.27 | 4 | 2,534 | 2,097† | 11 |
| Uniswap (with ERC-20) | 317 | 0.24 | 39 | 12,201 | 10,553† | 42 |
| Dexible swap | 34 | 0.23 | 0 | 2,910 | — | 0 |
| KoET | 166 | 0.23 | 2 | 2,782 | 2,758† | 8 |
| Poly Network | 110 | 0.24 | 5 | 5,143 | — | 6 |
| HODLWallet | 74 | 0.23 | 3 | 1,421 | 1,991† | 6 |
| SysEscrow | 139 | 0.23 | 1 | 1,632 | 2,439† | 9 |
| **Aggregator with closures** | **81** | **0.24** | **1** | **5,739** | **1,636** | **8** |
| Same swap, no closure | 27 | 0.23 | 0 | 658 | — | 1 |

† from the artifact's `-sol.hex` at each contract's original pragma (possibly unoptimized — HODLWallet and SysEscrow coming out *larger* than their SCIF versions suggests so); — where no Solidity implementation exists. ERC-20 and Uniswap regenerated today land within 0.3% of the paper's Table 1; the others differ more because the compiler has evolved since (trust store, codegen changes).

Regenerating the seven case studies surfaced one codegen bug, now fixed (`src/ast/AnnAssign.java`): every struct-typed local was declared `storage`, which is a solc type error when the initializer is a call returning a memory struct (Poly Network's `merkleProve`). The fix marks a local `storage` only when its initializer is a storage reference (SysEscrow's `escrows[hash]` locals keep their by-reference semantics); the full JUnit suite passes.

### RSE sites

| contract | variant | lock | trust | actsFor | success | total |
|---|---|---|---|---|---|---|
| UniswapRouter | SCIF: closurable target | 2 | 1 | 2 | 0 | 5 |
| Aggregator | SCIF: closure invoker | 1 | 0 | 0 | 1 | 2 |
| User | SCIF: closure creator | 0 | 1 | 0 | 0 | 1 |
| UserDirect | SCIF: no closure | 0 | 1 | 0 | 0 | 1 |
| *(4 contracts)* | *hand-written Solidity* | 0 | 0 | 0 | 0 | 0 |

The first four rows are all compiler output. Being `closurable` costs the target two acts-for assertions at its closure entry, beside the one sender check its normal entry already had; invoking costs one success assertion. Creating a closure costs nothing at run time.

The last row is the hand-written unsafe idiom, which enforces nothing at run time. That is the vulnerability, not a baseline to be beaten.

### Gas

The full 2×2 of {Solidity, SCIF} × {relay, direct}, so the cost of IFC and the cost of relaying separate:

| scenario | gas | vs raw relay |
|---|---|---|
| raw Solidity relay (unsafe) | 96,444 | — |
| **SCIF closure relay (this thesis)** | **111,161** | **+15.3%** |
| raw Solidity direct call (no relay) | 87,508 | −9.3% |
| SCIF direct call (no relay) | 88,344 | −8.4% |

Decomposition: **IFC alone** (SCIF direct vs raw direct) **+1.0%** — in line with the paper's −3% to +8% per operation; **the relay hop alone** (raw relay vs raw direct) +10.2%; **securing the relay** (closure vs raw relay) +15.3%.

One-time: 43,956 gas to register a delegation, once per user/relay pair — the on-chain analog of a token approval.

(The unoptimized three-way measurements were 105,626 / 122,445 / 93,535 — +15.9% / −11.4%; the optimizer shrinks the absolute numbers and barely moves the ratios. The raw-direct cell was added later, directly under the optimized configuration.)

For context, the SCIF paper reports −3% to +8% for its case studies. Those compare the same operation with and without IFC; this comparison additionally crosses a trust boundary, which is where the difference lies.

## Caveats

- Tokens are mocks. Real ERC-20 storage writes raise all three gas numbers together; the differences between them are unaffected.
- Both baselines were written for this evaluation (see "On baselines" above); they are not artifacts of the SCIF paper.
- Compilation times exclude JVM startup and are single cold runs; the run-to-run spread is a few percent, well below the effects discussed.
- `build.gradle` pins the Java toolchain to 26. It was 21, but no JDK 21 is present on this machine; the full suite (186 tests) passes under 26.
