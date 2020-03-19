LIBPATH=lib
CUP=java-cup-11b.jar

default: Wyvern
	
all: Wyvern sherrloc-build

Wyvern: SolCompiler.class TypeChecker.class LexerTest.class Parser.class Wyvern.java
	javac -cp .:${LIBPATH}/* Wyvern.java

SolCompiler.class: TypeChecker.class SolCompiler.java ${wildcard compile/*.java} ${wildcard ast/*.java} ${wildcard typecheck/*.java} ${wildcard sherrlocUtils/*.java}
	javac -cp .:${LIBPATH}/* SolCompiler.java TypeChecker.java compile/*.java ast/*.java typecheck/*.java sherrlocUtils/*.java

TypeChecker.class: Parser.class Lexer.class TypeChecker.java ${wildcard ast/*.java} ${wildcard typecheck/*.java} ${wildcard sherrlocUtils/*.java}
	javac -cp .:${LIBPATH}/* TypeChecker.java ast/*.java typecheck/*.java sherrlocUtils/*.java

LexerTest.class: Lexer.java LexerTest.java
	javac -cp .:${LIBPATH}/* LexerTest.java 

Lexer.class: Lexer.java 
	javac -cp .:${LIBPATH}/* Lexer.java

Lexer.java: Wyvern.jflex 
	jflex Wyvern.jflex

Parser.class: Lexer.java Parser.java ${wildcard ast/*.java} ${wildcard typecheck/*.java}
	javac -cp .:${LIBPATH}/* ast/*.java typecheck/*.java sym.java Parser.java

Parser.java: Wyvern.cup
	java -jar ${LIBPATH}/${CUP} -expect 1000 -interface -parser Parser Wyvern.cup

sherrloc-build:
	cd sherrloc; ant

clean:
	rm -f *.class
	rm -f Lexer.java
	rm -f Parser.java
	rm -f ast/*.class typecheck/*.class sherrlocUtils/*.class
