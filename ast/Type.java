package ast;

import typecheck.NTCContext;
import typecheck.NTCEnv;

import java.util.HashSet;

public class Type extends Expression {
    public String x;
    public Type(String x) {
        this.x = x;
    }

    @Override
    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        NTCContext now = new NTCContext(this, parent);
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
}
