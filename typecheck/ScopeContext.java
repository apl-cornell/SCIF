package typecheck;

import ast.*;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

import java.util.HashMap;
import java.util.Map;

public class ScopeContext {
    Node cur;
    HashMap<ExceptionTypeSym, Boolean> funcExceptionMap;
    ScopeContext parent;
    String SHErrLocName;

    public ScopeContext(String specifiedName) {
        cur = null;
        parent = null;
        funcExceptionMap = new HashMap<ExceptionTypeSym, Boolean>();
        SHErrLocName = specifiedName;
    }

    public ScopeContext(Node cur, ScopeContext parent) {
        this.cur = cur;
        this.parent = parent;
        if (parent != null)
            funcExceptionMap = new HashMap<>(parent.funcExceptionMap);
        else
            funcExceptionMap = new HashMap<>();
        SHErrLocName = calcSHErrLocName();
    }

    public ScopeContext(Node cur, ScopeContext parent, HashMap<ExceptionTypeSym, Boolean> funcExceptionMap) {
        this.cur = cur;
        this.parent = parent;
        this.funcExceptionMap = new HashMap<>(funcExceptionMap);
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
        else if (cur instanceof Program)
            localPostfix = ((Program) cur).programName;
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

        return new Constraint(new Inequality(getSHErrLocName(), op, rhs.getSHErrLocName()), env.globalHypothesis, location, env.curContractSym.name, "");
    }
    public Constraint genCons(String rhs, Relation op, NTCEnv env, CodeLocation location) {
        return new Constraint(new Inequality(getSHErrLocName(), op, rhs), env.globalHypothesis, location, env.curContractSym.name, "");
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

    // static Genson genson = new GensonBuilder().useClassMetadata(true).useIndentation(true).useRuntimeType(true).create();
    @Override
    public String toString() {
        return SHErrLocName;
    }

    public boolean isCheckedException(ExceptionTypeSym t, boolean extern) {
        return funcExceptionMap.containsKey(t) && (extern || funcExceptionMap.get(t));
    }

    public void addException(ExceptionTypeSym t, boolean inTx) {
        funcExceptionMap.put(t, inTx);
    }

    public void removeException(ExceptionTypeSym t) {
        funcExceptionMap.remove(t);
    }

    public void printExceptionSet() {
        System.err.println(funcExceptionMap.size());
        for (Map.Entry<ExceptionTypeSym, Boolean> t : funcExceptionMap.entrySet()) {
            System.err.println(t + t.getKey().name);
        }
    }
}
