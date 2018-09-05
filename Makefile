CUPRUNTIME=java-cup-11b-runtime.jar
CUP=java-cup-11b.jar


all: Lexer.java Parser.java sym.java LexerTest.java
	javac -cp $(CUPRUNTIME):. Lexer.java Parser.java sym.java LexerTest.java

Lexer.java: python3.jflex
	jflex python3.jflex

Parser.java: python3.cup
	java -jar $(CUP) -expect 1000 -debug -interface -parser Parser python3.cup

