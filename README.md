# SCIF

## Prerequisite

* [JFlex](https://jflex.de/)
* Java 15
* Ant

## Usage
`make all` to compile all files including sherrloc

```console
Usage: SCIF (-t | -p | -l | -c) [-hV] -i=<inputFiles>...
            [-i=<inputFiles>...]... [-lg=<outputFileNames>...]...
A set of tools for a new smart contract language with information flow control,
SCIF.
  -c, --compiler        Compile to Solidity
  -h, --help            Show this help message and exit.
  -i=<inputFiles>...    The source code file(s).
  -l, --lexer           Tokenize
      -lg=<outputFileNames>...
                        The log file.
  -p, --parser          Parse: ast json as log
  -t, --typechecker     Information flow typecheck: constraints as log
  -V, --version         Print version information and exit.
```

An example:

```bash
./scif -c -i=SCIFex/Wallet.java
```

## TODO List
* finish the full pass
* more examples: multiWallet, Uniswap...
* default annotations
* `struct`
* all built-in functions/variables 
* augassign 
* exception related
