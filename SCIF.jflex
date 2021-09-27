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
        return new Symbol(sym.NAME, yyline + 1, yycolumn + 1, new String(yytext()));
    }
   
    Symbol key(int symbolId) {
        return new Symbol(keywords.get(yytext()), yyline + 1, yycolumn + 1, yytext());
    }

    protected void init_keywords() {
        keywords.put("false",   Integer.valueOf(sym.FALSE));
        keywords.put("none",    Integer.valueOf(sym.NONE));
        keywords.put("true",    Integer.valueOf(sym.TRUE));
        keywords.put("and",     Integer.valueOf(sym.AND));
        keywords.put("as",      Integer.valueOf(sym.AS));
        keywords.put("assert",  Integer.valueOf(sym.ASSERT));
        keywords.put("async",   Integer.valueOf(sym.ASYNC));
        keywords.put("await",   Integer.valueOf(sym.AWAIT));
        keywords.put("break",   Integer.valueOf(sym.BREAK));
        keywords.put("class",   Integer.valueOf(sym.CLASS));
        keywords.put("continue",Integer.valueOf(sym.CONTINUE));
        keywords.put("def",     Integer.valueOf(sym.DEF));
        keywords.put("del",     Integer.valueOf(sym.DEL));
        keywords.put("elif",    Integer.valueOf(sym.ELIF));
        keywords.put("else",    Integer.valueOf(sym.ELSE));
        keywords.put("except",  Integer.valueOf(sym.EXCEPT));
        keywords.put("finally", Integer.valueOf(sym.FINALLY));
        keywords.put("for",     Integer.valueOf(sym.FOR));
        keywords.put("from",    Integer.valueOf(sym.FROM));
        keywords.put("global",  Integer.valueOf(sym.GLOBAL));
        keywords.put("if",      Integer.valueOf(sym.IF));
        keywords.put("import",  Integer.valueOf(sym.IMPORT));
        keywords.put("in",      Integer.valueOf(sym.IN));
        keywords.put("is",      Integer.valueOf(sym.IS));
        keywords.put("lambda",  Integer.valueOf(sym.LAMBDA));
        keywords.put("nonlocal",Integer.valueOf(sym.NONLOCAL));
        keywords.put("not",     Integer.valueOf(sym.NOT));
        keywords.put("or",      Integer.valueOf(sym.OR));
        keywords.put("pass",    Integer.valueOf(sym.PASS));
        keywords.put("raise",   Integer.valueOf(sym.RAISE));
        keywords.put("return",  Integer.valueOf(sym.RETURN));
        keywords.put("try",     Integer.valueOf(sym.TRY));
        keywords.put("while",   Integer.valueOf(sym.WHILE));
        keywords.put("with",    Integer.valueOf(sym.WITH));
        keywords.put("yield",   Integer.valueOf(sym.YIELD));
        keywords.put("endorse", Integer.valueOf(sym.ENDORSE));
        keywords.put("autoendorse", Integer.valueOf(sym.AUTOENDORSE));
        // keywords.put("const", Integer.valueOf(sym.CONST));
        keywords.put("map", Integer.valueOf(sym.MAP));
        keywords.put("key", Integer.valueOf(sym.KEY));
        keywords.put("contract", Integer.valueOf(sym.CONTRACT));
        keywords.put("interface", Integer.valueOf(sym.INTERFACE));
        keywords.put("struct", Integer.valueOf(sym.STRUCT));
        keywords.put("lock", Integer.valueOf(sym.GUARD));
        keywords.put("ref", Integer.valueOf(sym.REF));
        keywords.put("extends", Integer.valueOf(sym.EXTENDS));
        keywords.put("super", Integer.valueOf(sym.SUPER));
        // keywords.put("lock", Integer.valueOf(sym.LOCK));
        keywords.put("let", Integer.valueOf(sym.LET));
        keywords.put("then", Integer.valueOf(sym.THEN));
        keywords.put("else", Integer.valueOf(sym.ELSE));
        keywords.put("new", Integer.valueOf(sym.NEW));
        keywords.put("final", Integer.valueOf(sym.FINAL));
        keywords.put("static", Integer.valueOf(sym.STATIC));
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
indent = \t

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
    "&&"    { return op(sym.AND); }
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
