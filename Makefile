LIBPATH=lib
CUP=java-cup-11b.jar

default: Wyvern
	
all: Wyvern sherrloc-build

Wyvern: TypeChecker.class LexerTest.class Parser.class Wyvern.java
	javac -cp .:${LIBPATH}/* Wyvern.java

TypeChecker.class: Lexer.class Parser.class TypeChecker.java ${wildcard ast/*.java} ${wildcard utils/*.java}
	javac -cp .:${LIBPATH}/* TypeChecker.java ast/*.java utils/*.java

LexerTest.class: Lexer.java LexerTest.java
	javac -cp .:${LIBPATH}/* LexerTest.java 

Lexer.class: Lexer.java sym.java
	javac -cp .:${LIBPATH}/* Lexer.java

Lexer.java: Wyvern.jflex 
	jflex Wyvern.jflex

Parser.class: Parser.java sym.java ${wildcard ast/*.java} ${wildcard utils/*.java} 
	javac -cp .:${LIBPATH}/* Parser.java sym.java ast/*.java utils/*.java

Parser.java: Wyvern.cup
	java -jar ${LIBPATH}/${CUP} -expect 1000 -interface -parser Parser Wyvern.cup

sherrloc-build:
	cd sherrloc; ant
