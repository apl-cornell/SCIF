package ast;

import typecheck.ExpOutcome;
import typecheck.VisitEnv;

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
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
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
