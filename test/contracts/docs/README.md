# SCIF programs used in the documentation

One file per complete program that appears in `docs/`. Fragments (single statements, method bodies, the built-in file dump) are not mirrored. Keep these in sync with the Markdown: when a code block in `docs/` changes, change the file here, and vice versa.

| File | Where it appears | Status (master @ ddeaa10) |
|---|---|---|
| `IERC20.scif` | Introduction / Your First SCIF Contract | compiles |
| `ERC20.scif` | Introduction / Your First SCIF Contract (linked as the full implementation) | compiles |
| `ERC20_extended.scif` | Your First SCIF Contract, "Extending the ERC20 Token": `ERC20.scif` plus the two events and the `mint`/`burn` solution | compiles |
| `SimpleStorage.scif` | SCIF by Example, "Layout of a SCIF source file" (also Introduction / Layout of a SCIF source file) | compiles |
| `Wallet.scif` | SCIF by Example, "Multi-User Wallet" (first listing) | compiles |
| `Wallet_exceptions.scif` | SCIF by Example, "Multi-User Wallet" (second listing) | fails: `rescue (error e)` — there is no built-in `error` type; the compiler accepts `rescue *` |
| `contracts_ContractName.scif` | Language Basics / Contracts, "Structure of a contract" | fails: `contract ContractName[this]` is not valid syntax; also uses `msg.sender` and `balance[...]`, which do not exist |
| `contracts_IERC20.scif` | Language Basics / Contracts, "Interface" | compiles |
| `expressions_Calls.scif` | Language Basics / Expressions and Statements, "Method calls" (contracts `C` and `D` in one file) | fails: every contract needs a constructor before its methods |
| `reentrancy_Uniswap.scif` | Security Mechanisms / Reentrancy Protection, second listing (the doc's `(*\label{...}*)` LaTeX fragment is dropped here) | fails: `IERC20 tX, tY;` declares two variables in one statement, which the grammar does not allow |

"Status" was checked with the compiler built from master at `ddeaa10` (`java -ea -jar SCIF.jar -c <file>`). None of these files is in the JUnit test lists yet; adding the compiling ones to `TestCompilation.testPositive` is the intended next step, so that the docs cannot drift silently.
