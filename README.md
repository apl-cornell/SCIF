# Wyvern

## Prerequisite

* [JFlex](https://jflex.de/)
* Java 15
* Ant


## Usage
```make all``` to compile all files including sherrloc

```console
~/G/Wyvern ❯❯❯ ./wyvern
Missing required parameter: <inputFile>
Usage: wyvern (-t | -p | -l) [-hV] <inputFile> [<outputFileNames>...]
A set of tools for a new smart contract language with information flow control,
Vyperflow.
      <inputFile>     The source code file.
      [<outputFileNames>...]
                      The log file.
  -h, --help          Show this help message and exit.
  -l, --lexer         Tokenize
  -p, --parser        Parse: ast json as log
  -t, --typechecker   Information flow typecheck: constraints as log
  -V, --version       Print version information and exit.
```

Lexer:

```bash
java -cp "lib/*":. LexerTest test/parser/case1.py
```

Parser:

```bash
java -cp "lib/*":. Parser test/parser/case1.py  2>| ast.err
```
```ast.err``` stores a json type ast

TypeChecker:

```bash
java -cp "lib/*":. TypeChecker test/Wyvern/case2.wy ./tmp.cons
sherrloc/sherrloc -c tmp.cons 
```
```tmp.cons``` stores the constrains generated

## Known Issues

* same name functions (vyper doesn't support)
* for 
* with (vyper doesn't support)
* augassign 
* exception related
* only support top-level functions (vyper doesn't support)
* compact if (vyper doesn't support)
