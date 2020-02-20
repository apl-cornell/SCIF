package typecheck;

import ast.Contract;
import ast.Node;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;

public class NTCContext {
    Node cur;
    NTCContext parent;
    String SHErrLocName;

    public NTCContext(Node cur, NTCContext parent) {
        this.cur = cur;
        this.parent = parent;
        SHErrLocName = calcSHErrLocName();
    }

    private String calcSHErrLocName() {
        return parent.getSHErrLocName() + "." + cur.toSHErrLocFmt();
    }

    public String getSHErrLocName() {
        return SHErrLocName;
    }

    public Constraint genCons(NTCContext rhs, Relation op, NTCEnv env, CodeLocation location) {
        return new Constraint(new Inequality(getSHErrLocName(), op, rhs.getSHErrLocName()), env.globalHypothesis, location);
    }
    public Constraint genCons(String rhs, Relation op, NTCEnv env, CodeLocation location) {
        return new Constraint(new Inequality(getSHErrLocName(), op, rhs), env.globalHypothesis, location);
    }

    public boolean isContractLevel() {
        return (cur instanceof Contract);
    }
}
