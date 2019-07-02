# Wyvern

## Usage
```make all``` to compile all files including sherrloc

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

* same name functions
* for
* with
* augassign
* exception related
* only support top-level functions
* compact if