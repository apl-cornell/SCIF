package ast;

public class Keyword extends Node {
    String arg;
    Expression value;
    public Keyword(String x, Expression y) {
        arg = x;
        value = y;
    }

    public boolean typeMatch(Keyword keyword) {
        return arg.equals(keyword.arg) && value.typeMatch(keyword.value);
    }
}
