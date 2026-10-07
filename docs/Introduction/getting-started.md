# Getting Started

SCIF (**S**mart **C**ontract **I**nformation **F**low) is a language for writing smart contracts whose security requirements are checked at compile time. The SCIF compiler type-checks the information flow of a program and, if the program passes, translates it to Solidity. In this page we will cover how to:

- Install the SCIF compiler
- Compile a first contract and find the generated Solidity
- Deploy the generated Solidity with the Foundry toolchain (optional)
- Use the compiler's command-line options

## Dependencies

Ensure the following are installed:

- Java 21 or later, to run the compiler
- Foundry (optional), to build and deploy the generated Solidity

## Installing the SCIF compiler

### Using the prebuilt JAR

Download the prebuilt [SCIF JAR](https://github.com/apl-cornell/SCIF/releases/download/latest/SCIF.jar). Run it with:

```bash
java -ea -jar SCIF.jar -c [path_to_SCIF_contract]
```

### Building from source

SCIF is hosted on [GitHub](https://github.com/apl-cornell/SCIF). Building from source requires the following:

- Git
- JDK 21 (this specific version is required)

```bash
git clone --recurse-submodules https://github.com/apl-cornell/SCIF.git
cd SCIF
./gradlew fatJar
```

This produces `build/libs/SCIF.jar`, the same artifact as the prebuilt JAR. Alternatively, the `scif` script in the repository root runs the compiler straight from the source tree, with the same arguments:

```bash
./scif -c [path_to_SCIF_contract]
```

## Compiling your first contract

Save the following contract as `SimpleStorage.scif`. It stores a single unsigned integer that only the contract itself is trusted to change; the [SCIF by Example](./scif-by-example) page walks through its structure in detail.

```scif
contract SimpleStorage {
  uint{this} storedData;

  constructor() {
    super();
  }

  void set{this}(uint x) {
    storedData = x;
  }

  public uint get() {
    return storedData;
  }
}
```

Compile it with:

```bash
java -ea -jar SCIF.jar -c SimpleStorage.scif
```

The compiler parses the file, runs regular type checking and *information-flow type checking*, and finally generates Solidity. On success it prints nothing and writes the output next to where you ran it.

::: info Output location
The generated file takes the input's name with a `.sol` extension and is written to the current working directory (`SimpleStorage.scif` → `./SimpleStorage.sol`); use `-o <file>` to choose another path. If a contract imports other SCIF files, those files are not compiled automatically. The generated Solidity imports the matching `.sol` files, so every imported file needs to be compiled with its own `-c` command, with the resulting `.sol` placed in the same directory. Compiler logs go to `./logs/`.
:::

### Error diagnostics

Suppose we make `set` public so that anyone can call it, but keep assigning its argument to the high-integrity field `storedData`, which is labeled `{this}` and may only be influenced by data the contract trusts:

```scif
  public void set(uint x) {
    storedData = x;
  }
```

A public function can be called by anyone, so `x` carries only the caller's integrity, while `storedData` is trusted by the contract. The compiler rejects the assignment and lists the locations most likely to be wrong, with the constraint each one violates:

```
Static type error.
Code locations most likely to be wrong:
1. SimpleStorage.scif, line 2, column 14: Variable storedData may be labeled incorrectly.
Constraint violated: s11.SimpleStorage.storedData..lbl==s11.SimpleStorage.this.
  uint{this} storedData;
             ^

2. SimpleStorage.scif, line 9, column 18: Integrity of the value being assigned must be trusted to allow this assignment.
Constraint violated: s11.SimpleStorage.set.x..lbl<=s11.SimpleStorage.storedData..lbl.
    storedData = x;
                 ^

3. SimpleStorage.scif, line 8, column 19: Argument x may be labeled incorrectly.
Constraint violated: s11.SimpleStorage.set.x..lbl==s11.SimpleStorage.set.sender.
  public void set(uint x) {
                  ^
```

The compiler reports a ranked list of locations: the first entry is the most likely cause of the error, and the others are further places where a change would also make the program type-check. If `set` is indeed meant to be callable by everyone, its argument is untrusted by design, and the only way to store it in `storedData` is to explicitly *endorse* it first. This is a fundamental rule of SCIF: untrusted data never flows into trusted state implicitly. Every such flow must appear in the code as an explicit endorsement, so that the decision to trust an input is always made deliberately and never by accident.

```scif
  public void set(uint x) {
    storedData = endorse(x, sender -> this);
  }
```

Syntax errors are reported in the same format, with the position of the unexpected token:

```
Broken.scif, line 3, column 1: Syntax error at }
}
^
```

When compilation fails, no `.sol` file is written.

### Generated Solidity

The output is ordinary Solidity, so it can be built and deployed with the usual tools. A few things to be aware of when reading or deploying it:

- The output targets Solidity 0.8.27 or later, as declared by its `pragma`.
- Each contract is self-contained. Although nearly all of SCIF's typechecking happens at compile time, a small part that must remain dynamic is placed directly inside the contract.
- Every `public` SCIF function becomes a public Solidity function named `<name>_<hash>`. The hash is computed from the full signature, labels included, so functions with different security labels always get different names. External callers and the ABI use these hashed names to invoke the correct functions.
- Exceptions declared with `throws` do not revert. The generated function returns a status code and ABI-encoded data instead, so the caller can handle the exception while all state changes stay in place.
- The generated code reverts, rolling back state, in four cases: a `revert` statement, a failed `assert`, a failure inside an `atomic` block, and an exception that reaches a point with no handler for it.

## Deploying SCIF contracts

After compilation you have a Solidity file that carries the security guarantees checked by SCIF. The following steps deploy it with Foundry.

```bash
mkdir scif_simple_storage
cd scif_simple_storage
forge init --no-git
```

Copy `SimpleStorage.sol` into the `src` folder and build:

```bash
forge build
```

To deploy to a local test chain, start `anvil` in another terminal and run:

```bash
forge create src/SimpleStorage.sol:SimpleStorage \
  --rpc-url http://127.0.0.1:8545 \
  --private-key <PRIVATE_KEY> \
  --broadcast
```

To interact with the contract, use the hashed entry-point names from the generated Solidity (they are also listed in the ABI under `out/SimpleStorage.sol/SimpleStorage.json`):

```bash
cast call <CONTRACT_ADDRESS> \
  "get_4ce559922d5bd02dde1aff0cb59457aa3e1221ceecdbbaa72755f045001f13d6()(uint256)" \
  --rpc-url http://127.0.0.1:8545
```

## Command-line reference

```
java -ea -jar SCIF.jar [-c | -t | -p | -l] [-o <file>] [-lg <dir>] [-debug] <file>...
```

| Option | Meaning |
|---|---|
| `-c`, `--compiler` | Type-check and compile to Solidity. This is the default when no mode is given. |
| `-t`, `--typechecker` | Run regular and information-flow type checking only; no Solidity is generated. |
| `-p`, `--parser` | Parse only, reporting syntax errors. |
| `-l`, `--lexer` | Print the token stream of the input. |
| `-o <file>` | Where to write the generated `.sol` file (compile mode). Defaults to the input's name with a `.sol` extension, in the current directory. |
| `-lg <dir>` | Directory for the constraint logs written by `-t` and `-p`. Defaults to `./.scif`. |
| `-debug` | Print extra diagnostics, including the name of the output file. |
| `-h`, `--help` | Show the usage message. |
| `-V`, `--version` | Print the compiler version. |

The `scif` script in the source tree accepts the same options, for example `./scif -t SimpleStorage.scif`.
