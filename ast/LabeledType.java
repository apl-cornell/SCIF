package ast;

import utils.CodeLocation;

public class LabeledType extends Expression {
    Name x;
    IfLabel ifl;
    public LabeledType(Name x, IfLabel ifl) {
        this.x = x;
        this.ifl = ifl;
    }

}
