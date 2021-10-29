package ast;

import java.util.HashSet;

public class TrustAtom extends Expression {
    String name;
    IfLabel ifl;
    boolean isIfl;

    public TrustAtom(String name) {
        this.name = name;
        this.isIfl = false;
    }

    public TrustAtom(IfLabel ifl) {
        this.isIfl = true;
        this.ifl = ifl;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof TrustAtom;
    }

    public String toSherrlocFmt(String contractName) {
        if (isIfl) {
            return ifl.toSherrlocFmt();
        } else {
            return contractName + "." + name;
        }
    }
}
