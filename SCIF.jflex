import java_cup.runtime.*;
import java.util.Stack;
import java.util.HashMap;


/**
 * token : (https://github.com/python/cpython/blob/master/Parser/tokenizer.c)
    "ENDMARKER",
    "NAME",
    "NUMBER",
    "STRING",
    "NEWLINE",
    "INDENT",
    "DEDENT",
    "LPAR",
    "RPAR",
    "LSQB",
    "RSQB",
    "COLON",
    "COMMA",
    "SEMI",
    "PLUS",
    "MINUS",
    "STAR",
    "SLASH",
    "VBAR",
    "AMPER",
    "LESS",
    "GREATER",
    "EQUAL",
    "DOT",
    "PERCENT",
    "LBRACE",
    "RBRACE",
    "EQEQUAL",
    "NOTEQUAL",
    "LESSEQUAL",
    "GREATEREQUAL",
    "TILDE",
    "CIRCUMFLEX",
    "LEFTSHIFT",
    "RIGHTSHIFT",
    "DOUBLESTAR",
    "PLUSEQUAL",
    "MINEQUAL",
    "STAREQUAL",
    "SLASHEQUAL",
    "PERCENTEQUAL",
    "AMPEREQUAL",
    "VBAREQUAL",
    "CIRCUMFLEXEQUAL",
    "LEFTSHIFTEQUAL",
    "RIGHTSHIFTEQUAL",
    "DOUBLESTAREQUAL",
    "DOUBLESLASH",
    "DOUBLESLASHEQUAL",
    "AT",
    "ATEQUAL",
    "RARROW",
    "ELLIPSIS",
    "OP",
    "<ERRORTOKEN>",
    "COMMENT",
    "NL",
    "ENCODING",
    "<N_TOKENS>"

    Wyvern-specific
    "ENDORSE",
    "AUTOENDORSE",
    "GUARD"
 */


%%

%class Lexer
%implements sym
%unicode
%cup
%line
%column

%{
    StringBuffer sb = new StringBuffer();
    HashMap<String, Integer> keywords;
    int inbrace;

    Symbol op(int tokenId) {
        return new Symbol(tokenId, yyline + 1, yycolumn + 1);
    }

    Symbol op(int tokenId, Object value) {
        return new Symbol(tokenId, yyline + 1, yycolumn + 1, value);
    }

    Symbol id() {
        return new Symbol(sym.NAME, yyline + 1, yycolumn + 1, yytext());
    }
   
    Symbol key(int symbolId) {
        return new Symbol(keywords.get(yytext()), yyline + 1, yycolumn + 1, yytext());
    }

    protected void init_keywords() {
        keywords.put("false",   new Integer(sym.FALSE));
        keywords.put("none",    new Integer(sym.NONE));
        keywords.put("true",    new Integer(sym.TRUE));
        keywords.put("and",     new Integer(sym.AND));
        keywords.put("as",      new Integer(sym.AS));
        keywords.put("assert",  new Integer(sym.ASSERT));
        keywords.put("async",   new Integer(sym.ASYNC));
        keywords.put("await",   new Integer(sym.AWAIT));
        keywords.put("break",   new Integer(sym.BREAK));
        keywords.put("class",   new Integer(sym.CLASS));
        keywords.put("continue",new Integer(sym.CONTINUE));
        keywords.put("def",     new Integer(sym.DEF));
        keywords.put("del",     new Integer(sym.DEL));
        keywords.put("elif",    new Integer(sym.ELIF));
        keywords.put("else",    new Integer(sym.ELSE));
        keywords.put("except",  new Integer(sym.EXCEPT));
        keywords.put("finally", new Integer(sym.FINALLY));
        keywords.put("for",     new Integer(sym.FOR));
        keywords.put("from",    new Integer(sym.FROM));
        keywords.put("global",  new Integer(sym.GLOBAL));
        keywords.put("if",      new Integer(sym.IF));
        keywords.put("import",  new Integer(sym.IMPORT));
        keywords.put("in",      new Integer(sym.IN));
        keywords.put("is",      new Integer(sym.IS));
        keywords.put("lambda",  new Integer(sym.LAMBDA));
        keywords.put("nonlocal",new Integer(sym.NONLOCAL));
        keywords.put("not",     new Integer(sym.NOT));
        keywords.put("or",      new Integer(sym.OR));
        keywords.put("pass",    new Integer(sym.PASS));
        keywords.put("raise",   new Integer(sym.RAISE));
        keywords.put("return",  new Integer(sym.RETURN));
        keywords.put("try",     new Integer(sym.TRY));
        keywords.put("while",   new Integer(sym.WHILE));
        keywords.put("with",    new Integer(sym.WITH));
        keywords.put("yield",   new Integer(sym.YIELD));
        keywords.put("endorse", new Integer(sym.ENDORSE));
        keywords.put("autoendorse", new Integer(sym.AUTOENDORSE));
        keywords.put("const", new Integer(sym.CONST));
        keywords.put("map", new Integer(sym.MAP));
        keywords.put("key", new Integer(sym.KEY));
        keywords.put("contract", new Integer(sym.CONTRACT));
        keywords.put("interface", new Integer(sym.INTERFACE));
        keywords.put("struct", new Integer(sym.STRUCT));
        keywords.put("guard", new Integer(sym.GUARD));
        keywords.put("ref", new Integer(sym.REF));
        keywords.put("extends", new Integer(sym.EXTENDS));
        keywords.put("super", new Integer(sym.SUPER));
        keywords.put("lock", new Integer(sym.LOCK));
        keywords.put("let", new Integer(sym.LET));
        keywords.put("then", new Integer(sym.THEN));
        keywords.put("else", new Integer(sym.ELSE));
        keywords.put("new", new Integer(sym.NEW));
    }


%}

%init{
    this.keywords = new HashMap<>();
    init_keywords();
%init}

//, STRING

NEWLINE = \n | \r
digit = [0-9]
uppercase = [A-Z]
lowercase = [a-z]

letter = {uppercase}|{lowercase}
NAME = ( {letter} | "_") ({letter} | {digit} | "_")*
comment_body = .*
comment         = "//"{comment_body}
whitespace      = [ \n\t]
// encodingDeclaration = ""[^\n]*"coding"[:=][^\n]*
indent = \t | "    "

unicodeescape = "\\u"[a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9]
unicodeescape32 = "\\u"[a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9]
octescape = "\\"[0-7][0-7][0-7]
hexescape = "\\"[xX][0-9a-fA-F][0-9a-fA-F]

// number
NUMBER = {integer} | {floatnumber} | {imagnumber}

integer = {decinteger} | {bininteger} | {octinteger} | {hexinteger}
decinteger = {nonzerodigit} ("_"? {digit})* | "0"+ (["_"] "0")*
bininteger = "0" ("b" | "B") ("_"? {bindigit})+
octinteger = "0" ("o" | "O") ("_"? {octdigit})+
hexinteger = "0" ("x" | "X") ("_"? {hexdigit})+
nonzerodigit = [1-9]
bindigit = "0" | "1"
octdigit = [0-7]
hexdigit = {digit} | [a-f] | [A-F]

floatnumber = {pointfloat} | {exponentfloat}
pointfloat = {digitpart}? {fraction} | {digitpart} "."
exponentfloat = ({digitpart} | {pointfloat}) {exponent}
digitpart = {digit} ("_"? {digit})*
fraction = "." {digitpart}
exponent = ("e" | "E") ("+" | "-")? {digitpart}

imagnumber =  ({floatnumber} | {digitpart}) ("j" | "J")

invalid = "$" | "?" | "`"



%%

<YYINITIAL> {
    {comment}   {}
//    "True"  { return op(sym.TRUE); }
//    "False" { return op(sym.FALSE); }
//    "None"  { return op(sym.NONE); }
    "..."   { return op(sym.ELLIPSIS); }
    {NAME}  { 
            Integer i = keywords.get(yytext());
            if (i == null) return id();
            else return key(i.intValue());
        }
//    "\""    {
//            yybegin(STRING);
//            sb.setlength(0);
//        }

    "("     {
            return op(sym.LPAR); 
        }
    ")"     {
            return op(sym.RPAR); 
        }
    "["     {
            return op(sym.LSQB); 
        }
    "]"     {
            return op(sym.RSQB); 
    }
    ":"     { return op(sym.COLON); }
    ","     { return op(sym.COMMA); }
    ";"     { return op(sym.SEMI); }
    "+"     { return op(sym.PLUS); }
    "-"     { return op(sym.MINUS); }
    "*"     { return op(sym.STAR); }
    "/"     { return op(sym.SLASH); }
    "|"     { return op(sym.VBAR); }
    "&"     { return op(sym.AMPER); }
    "<"     { return op(sym.LESS); }
    ">"     { return op(sym.GREATER); }
    "="     { return op(sym.EQUAL); }
    "."     { return op(sym.DOT); }
    "%"     { return op(sym.PERCENT); }
    "{"     {
            return op(sym.LBRACE); 
    }
    "}"     {
            return op(sym.RBRACE); 
        }
    "^"     { return op(sym.CIRCUMFLEX); }
    "~"     { return op(sym.TILDE); }
    "@"     { return op(sym.AT); }
    "=="    { return op(sym.EQEQUAL); }
    "!="    { return op(sym.NOTEQUAL); }
    "<>"    { return op(sym.NOTEQUAL); }
    "<="    { return op(sym.LESSEQUAL); }
    "<<"    { return op(sym.LEFTSHIFT); }
    ">="    { return op(sym.GREATEREQUAL); }
    ">>"    { return op(sym.RIGHTSHIFT); }
    "+="    { return op(sym.PLUSEQUAL); }
    "-="    { return op(sym.MINEQUAL); }
    "->"    { return op(sym.RARROW); }
    "**"    { return op(sym.DOUBLESTAR); }
    "*="    { return op(sym.STAREQUAL); }
    //"//"    { return op(sym.DOUBLESLASH); }
    "/="    { return op(sym.SLASHEQUAL); }
    "|="    { return op(sym.VBAREQUAL); }
    "%="    { return op(sym.PERCENTEQUAL); }
    "&="    { return op(sym.AMPEREQUAL); }
    "^="    { return op(sym.CIRCUMFLEXEQUAL); }
    "@="    { return op(sym.ATEQUAL); }
    "<<="   { return op(sym.LEFTSHIFTEQUAL); }
    ">>="   { return op(sym.RIGHTSHIFTEQUAL); }
    "**="   { return op(sym.DOUBLESTAREQUAL); }
    //"//="   { return op(sym.DOUBLESLASHEQUAL); }
    "=>"    { return op(sym.EQUALGREATER); }

    {integer}   {
        return op(sym.NUMBER, yytext());
    }

    {NEWLINE} {
    }
    {indent}  {}

    {invalid} { return op(sym.ERROR, yytext()); }
    {whitespace} { }
    .       { return op(sym.ERROR, yytext()); }

    <<EOF>> {
            return op(sym.EOF);
    }
}


[^]         {
    return op(sym.ERROR, "Unrecognizable char: " + yytext());
}

//<STRING> {
//
//}
