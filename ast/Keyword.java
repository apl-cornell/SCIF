package ast;

import utils.CodeLocation;

public class Keyword extends Node {
    String arg;
    Expression value;
    public Keyword(String x, Expression y) {
        arg = x;
        value = y;
    }

}
