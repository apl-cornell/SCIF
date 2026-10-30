# Known bugs (exposition set)

One file per bug found while reserving built-in keywords on branch
`reserve-builtin-keywords`. **None of these bugs is fixed.** Each file's header
comment states what should happen ("Expected") and what does happen
("Actual"). Nothing here is listed in a JUnit test, so `./gradlew test` is
unaffected.

## Run them all

From the repo root:

```bash
d=$(mktemp -d); for f in test/contracts/knownBugs/*.scif; do n=$(basename "$f" .scif); ./gradlew -q run --args="-c $f -o $d/$n.sol" > "$d/$n.log" 2>&1; if [ -s "$d/$n.sol" ]; then echo "COMPILES  $n"; else echo "FAILS     $n"; fi; done; echo "outputs in $d"
```

Each `.sol` file holds the generated Solidity, and each `.log` holds the
compiler's messages. A single file:

```bash
./gradlew run --args="-c test/contracts/knownBugs/TopParamBypass.scif -o /tmp/out.sol"
```

Expected result of the loop: the files marked "compiles" below print
`COMPILES`; everything else, including both `_Control` files, prints `FAILS`.

## Suggested walkthrough

### 1. Silent security bugs: accepted, but the generated code is unsafe

| File | Result | What goes wrong |
|---|---|---|
| `TopParamBypass.scif` | compiles | `TOP` isn't reserved. With a parameter named `TOP`, the type checker reads `{TOP}` as the built-in most-trusted principal, but the run-time check `trusts(TOP, msg.sender)` uses the caller's own argument. Any caller passes by sending their own address. |
| `TopParamBypass_Control.scif` | rejected (correct) | The same contract with the parameter named `p` instead. Shows the type checker would otherwise catch it. |
| `TrustsOverride.scif` | compiles | A user `trusts()` that returns `true` silently replaces the built-in, so every trust check passes. |
| `BypassLocksOverride.scif` | compiles | A user `bypassLocks()` silently replaces the built-in, so re-entrant calls get through while the lock is held. |
| `AcquireLockOverride.scif` | compiles | A user `acquireLock()` silently replaces the built-in, so `lock(this)` never sets the lock. |
| `ResultSkipsIntegrity.scif` | compiles | `result = v;` skips the integrity check, so untrusted input is returned as `{this}`. |
| `ResultSkipsIntegrity_Control.scif` | rejected (correct) | The same method with `return v;` instead. |

### 2. Root cause of the override bugs

| File | Result | What goes wrong |
|---|---|---|
| `MissingOverride.scif` | compiles | `override` is never required. Any same-named method replaces the inherited one (`Contract.java:581`). |
| `ReleaseLockOverride.scif` | compiles | A user `releaseLock()` silently replaces the built-in, so the lock stays held for the rest of the transaction. Lower impact. |

### 3. Accepted by SCIF, but the generated Solidity is broken

`solc` was not available, so "should not compile" is an expectation from
reading the generated code, not an observed `solc` error.

| File | Result | What goes wrong |
|---|---|---|
| `TopLabelUndeclared.scif` | compiles | A `{TOP}` method label emits `trusts(TOP, msg.sender)`, but `TOP` is never declared. |
| `PrivateValueUndeclared.scif` | compiles | A private method using `value` emits `return value;` with no `value` declared. |
| `SolidityNamePassthrough.scif` | compiles | A state variable named `msg` becomes `uint256 msg;`, breaking every generated `msg.sender`. The same applies to `block`, `type`, `memory`, etc. |

### 4. Compiler crashes instead of error messages

| File | Result | What goes wrong |
|---|---|---|
| `ReturnInLockCrash.scif` | crash | `return` inside `lock(...)` in a labelled method: `NullPointerException` (`Utils.java:826`, from `GuardBlock.java:73`). |
| `StateVarNamedMLock.scif` | crash | State variable `m_lock`: `AssertionError` (`Contract.java:517`). |
| `StateVarNamedTrusts.scif` | crash | State variable named `trusts`, `bypassLocks`, `acquireLock` or `releaseLock`: `AssertionError` (`Contract.java:587`). |
| `StateVarNamedUint.scif` | crash | State variable named after a built-in type: `RuntimeException` (`StateVariableDeclaration.java:78`). |
| `MethodNamedAddress.scif` | crash | Method named after a built-in type: `RuntimeException` (`FunctionSig.java:188`). |

### 5. Bugs that aren't in a `.scif` file

- **The compiler only works with `-ea`.** `TypeChecker.java:47` runs a required pass inside an `assert`. Without `-ea`, nothing is compiled, no error is printed, and the exit code is 0. `./gradlew run` always enables assertions; plain `java` without `-ea` shows the bug.
- **`build/libs/SCIF.jar` crashes on every input** with `NoSuchMethodError` on `java_cup.runtime.SymbolFactory.newSymbol`.
  - The `fatJar` task packages `configurations.runtimeClasspath`, which excludes Cornell CUP (`lib/java_cup.jar`).
  - So the only CUP runtime in the jar is an old copy bundled inside `SHErrLoc.jar`.
  - CI publishes this jar, so the release jar is probably affected (not verified).
- **`TestParsing.testPositive` swallows `SyntaxError`**, so a positive parsing test passes even if the file doesn't parse.
- **`test/contracts/basic/DynamicTest.scif` uses `msg.sender`**, which isn't SCIF, and no longer parses. No test runs it.
- **The "Structure of a contract" example in `docs/LanguageBasics/contracts.md` is invalid**: `contract ContractName[this]`, and `balance` instead of `balances`.
- **The error for a parameter named `balance` is confusing.** Calling `balance(...)` in that method reports "contract not found: balance".

## Already fixed on this branch (for contrast)

These were the same kind of silent miscompile before `this`, `sender`, `any`,
`result`, `value` and `now` were reserved. They live in
`test/contracts/parsing/` as registered negative tests:

- `ValueShadow.scif`
- `NowShadow.scif`
- `ResultShadow.scif`
- `SenderShadow.scif`

On this branch each one fails with `Syntax error at '<name>'`. On `origin/master`
each one compiles silently into wrong code (see their header comments). To
show the "before" side:

```bash
git worktree add /tmp/scif-old origin/master
cd /tmp/scif-old && git submodule update --init
./gradlew run --args="-c $OLDPWD/test/contracts/parsing/ValueShadow.scif -o /tmp/old.sol"
cd - && git worktree remove --force /tmp/scif-old
```

## Each built-in name at each declaration site

From compiling a small probe for every combination, with `-ea`, at commit
`7bd1dbc`. The compiler source is identical at `fbe48c2`.

| Name | State variable | Local variable | Parameter | Method |
|---|---|---|---|---|
| `TOP` | rejected | **accepted** | **accepted (`TopParamBypass`)** | rejected |
| `balance`, `send` | rejected | rejected | accepted; harmless, confusing error if then called | rejected |
| `trusts`, `bypassLocks`, `acquireLock`, `releaseLock` | crash | rejected | accepted; shadows the function in generated code (expected `solc` error) | **accepted: silent override** |
| `uint`, `address` | crash | rejected | accepted; `bool uint` / `bool address` are Solidity syntax errors | crash |
| `m_lock` | crash | not probed | not probed | not probed |
