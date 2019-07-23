package ast;

import utils.CodeLocation;

public class DepMap extends LabeledType {
    public DepMap(Name x, IfLabel ifl) {
        super(x, ifl);
    }
    public DepMap(IfLabel ifl) {
        super(null, ifl);
    }
}
