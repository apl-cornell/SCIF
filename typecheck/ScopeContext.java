package typecheck;

import ast.*;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;

public class ScopeContext {
    Node cur;
    ScopeContext parent;
    String SHErrLocName;

    public ScopeContext(String specifiedName) {
        cur = null;
        parent = null;
        SHErrLocName = specifiedName;
    }

    public ScopeContext(Node cur, ScopeContext parent) {
        this.cur = cur;
        this.parent = parent;
        SHErrLocName = calcSHErrLocName();
    }

    private String calcSHErrLocName() {

        String localPostfix;
        if (cur instanceof Contract)
            localPostfix = ((Contract) cur).contractName;
        else if (cur instanceof FunctionSig)
            localPostfix = ((FunctionSig) cur).name;
        else if (cur instanceof If)
            localPostfix = "if" + cur.locToString();
        else if (cur instanceof While)
            localPostfix = "while" + cur.locToString();
        else if (cur instanceof Interface)
            localPostfix = ((Interface) cur).contractName;
        else if (cur instanceof GuardBlock)
            localPostfix = "guardBlock" + cur.locToString();
        else
            localPostfix = cur.toSHErrLocFmt();

        if (parent != null)
            return parent.getSHErrLocName() + "." + localPostfix;
        else
            return localPostfix;
    }

    public String getSHErrLocName() {
        return SHErrLocName;
    }

    public Constraint genCons(ScopeContext rhs, Relation op, NTCEnv env, CodeLocation location) {
        return new Constraint(new Inequality(getSHErrLocName(), op, rhs.getSHErrLocName()), env.globalHypothesis, location);
    }
    public Constraint genCons(String rhs, Relation op, NTCEnv env, CodeLocation location) {
        return new Constraint(new Inequality(getSHErrLocName(), op, rhs), env.globalHypothesis, location);
    }

    public boolean isContractLevel() {
        if (cur instanceof Contract)
            return true;
        if ((cur instanceof FunctionDef) || (cur instanceof FunctionSig) || (parent == null))
            return false;
        return parent.isContractLevel();
    }

    public ScopeContext getParent() {
        return parent;
    }

    public String getFuncName() {
        ScopeContext now = this;
        while (!(now.cur instanceof FunctionSig))
            now =  now.parent;
        return ((FunctionSig) now.cur).name;
    }
}
