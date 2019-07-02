LIBPATH=lib
CUP=java-cup-11b.jar

default: wyvern 
	
all: wyvern sherrloc-build

wyvern: Lexer.java Parser.java sym.java LexerTest.java TypeChecker.java ${wildcard ${LIBPATH}/*.java}
	javac -cp .:${LIBPATH}/* Lexer.java Parser.java sym.java LexerTest.java TypeChecker.java

Lexer.java: python3.jflex
	jflex python3.jflex

Parser.java: python3.cup
	java -jar ${LIBPATH}/${CUP} -expect 1000 -interface -parser Parser python3.cup

sherrloc-build:
	cd sherrloc; ant
