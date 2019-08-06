package ast;

import java.util.HashSet;

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

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        ifl.findPrincipal(principalSet);
    }


    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        ifl.findPrincipal(principalSet, getRidOf);
    }
}
