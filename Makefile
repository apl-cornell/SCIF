LIBPATH=lib
CUP=java-cup-11b.jar#polyglot-cup.jar

CLASSPATH = .:$(LIBPATH)/genson-1.5.jar:$(LIBPATH)/log4j-api-2.12.1.jar:$(LIBPATH)/log4j-core-2.12.1.jar:$(LIBPATH)/picocli-4.0.0-beta-1b.jar:$(LIBPATH)/${CUP}:$(LIBPATH)/sherrloc.jar:$(LIBPATH)/commons-cli-1.2.jar

default: all

all: SCIF 

slc: sherrloc-build

SCIF: TypeChecker.class LexerTest.class Parser.class SCIF.java
	javac -cp ${CLASSPATH} SCIF.java

TypeChecker.class: Parser.class Lexer.class TypeChecker.java ${wildcard ast/*.java} ${wildcard typecheck/*.java} ${wildcard sherrlocUtils/*.java} ${wildcard compile/*.java}
	javac -Xlint:deprecation -cp ${CLASSPATH} TypeChecker.java ast/*.java typecheck/*.java sherrlocUtils/*.java

LexerTest.class: Lexer.java LexerTest.java
	javac -cp ${CLASSPATH} LexerTest.java 

Lexer.class: Lexer.java 
	javac -cp ${CLASSPATH} Lexer.java

Lexer.java: SCIF.jflex 
	jflex SCIF.jflex

Parser.class: Lexer.java Parser.java ${wildcard ast/*.java} ${wildcard typecheck/*.java}
	javac -cp ${CLASSPATH} ast/*.java typecheck/*.java sym.java Parser.java

Parser.java: SCIF.cup
	java -jar ${LIBPATH}/${CUP} -expect 1000 -interface -parser Parser SCIF.cup

sherrloc-build:
	cd sherrloc; ant jar; mv lib/SHErrLoc.jar ../../../lib

clean:
	rm -f *.class
	rm -f Lexer.java
	rm -f Parser.java
	rm -f ast/*.class typecheck/*.class sherrlocUtils/*.class

.PHONY: clean sherrloc-build default all
