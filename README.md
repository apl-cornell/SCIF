# SCIF

## Prerequisite

* [JFlex](https://jflex.de/)
* Java 21
* Ant
* Gradle

## Installation

```console
git clone --recurse-submodules git@github.com:Neroysq/SCIF.git
cd SCIF
./gradlew build
```

## Usage

```console
Usage: SCIF [-t | -p | -l | -c] [-hV] [-debug] [-lg=<m_outputFileNames>...]...
            [-o=<m_solFileNames>...]... <m_inputFiles>...
A set of tools for a new smart contract language with information flow control,
SCIF.
      <m_inputFiles>...     The source code file(s).
  -c, --compiler            Compile to Solidity (default)
      -debug
  -h, --help                Show this help message and exit.
  -l, --lexer               Tokenize
      -lg=<m_outputFileNames>...
                            The log file.
  -o=<m_solFileNames>...    The output file.
  -p, --parser              Parse: ast json as log
  -t, --typechecker         Information flow typecheck: constraints as log
  -V, --version             Print version information and exit.
```

Compile a Wallet example to `./tmp.sol`:

```console
./scif -c test/contracts/ifcTypechecking/Wallet_lock_exception.scif -o ./tmp.sol
```

## Documentation

### Prerequisite

* yarn
* node.js 18

### Run locally

```console
yarn install
yarn docs:dev
```

