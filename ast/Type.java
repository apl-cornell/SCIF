package ast;

import typecheck.ScopeContext;
import typecheck.NTCEnv;

import java.util.HashSet;

public class Type extends Expression {
    public String x;
    public Type(String x) {
        this.x = x;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        return now;
    }

    public String toSHErrLocFmt() {
        return "T_" + x + location;
    }

    public String toSherrloc(String k, String v) {
        return "";
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {
        return;
    }


    public void findPrincipal(HashSet<String> principalSet, String getRidOf) {
        return;
    }

    public String toSolCode() {
        return x;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Type &&
                x.equals(((Type) expression).x);
    }
}
