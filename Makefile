LIBPATH=lib
CUP=polyglot-cup.jar

CLASSPATH = .:$(LIBPATH)/genson-1.5.jar:$(LIBPATH)/log4j-api-2.12.1.jar:$(LIBPATH)/log4j-core-2.12.1.jar:$(LIBPATH)/picocli-4.0.0-beta-1b.jar:$(LIBPATH)/polyglot-cup.jar

default: Wyvern
	
all: Wyvern sherrloc-build

Wyvern: TypeChecker.class LexerTest.class Parser.class Wyvern.java
	javac -cp ${CLASSPATH} Wyvern.java

TypeChecker.class: Parser.class Lexer.class TypeChecker.java ${wildcard ast/*.java} ${wildcard typecheck/*.java} ${wildcard sherrlocUtils/*.java}
	javac -cp ${CLASSPATH} TypeChecker.java ast/*.java typecheck/*.java sherrlocUtils/*.java

LexerTest.class: Lexer.java LexerTest.java
	javac -cp ${CLASSPATH} LexerTest.java 

Lexer.class: Lexer.java 
	javac -cp ${CLASSPATH} Lexer.java

Lexer.java: Wyvern.jflex 
	jflex Wyvern.jflex

Parser.class: Lexer.java Parser.java ${wildcard ast/*.java} ${wildcard typecheck/*.java}
	javac -cp ${CLASSPATH} ast/*.java typecheck/*.java sym.java Parser.java

Parser.java: Wyvern.cup
	java -jar ${LIBPATH}/${CUP} -expect 1000 -interface -parser Parser Wyvern.cup

sherrloc-build:
	cd sherrloc; ant

clean:
	rm -f *.class
	rm -f Lexer.java
	rm -f Parser.java
	rm -f ast/*.class typecheck/*.class sherrlocUtils/*.class
