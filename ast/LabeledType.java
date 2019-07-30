package ast;

import utils.CodeLocation;

public class LabeledType extends Expression {
    public Name x;
    public IfLabel ifl;
    public LabeledType(Name x, IfLabel ifl) {
        this.x = x;
        this.ifl = ifl;
    }

    public String toSherrloc(String k, String v) {
        return "";
    }
}
