package typecheck;

import ast.*;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ScopeContext {
    Node cur;
    public HashMap<ExceptionTypeSym, CodeLocation> exceptionMap;
    HashSet<ExceptionTypeSym> funcExceptionSet;
    ScopeContext parent;
    String SHErrLocName;

    public ScopeContext(String specifiedName) {
        cur = null;
        parent = null;
        exceptionMap = new HashMap<ExceptionTypeSym, CodeLocation>();
        funcExceptionSet = new HashSet<ExceptionTypeSym>();
        SHErrLocName = specifiedName;
    }

    public ScopeContext(Node cur, ScopeContext parent) {
        this.cur = cur;
        this.parent = parent;
        exceptionMap = new HashMap<>();
        if (parent != null)
            funcExceptionSet = parent.funcExceptionSet;
        else
            funcExceptionSet = new HashSet<>();
        SHErrLocName = calcSHErrLocName();
    }

    public ScopeContext(Node cur, ScopeContext parent, HashSet<ExceptionTypeSym> funcExceptionSet) {
        this.cur = cur;
        this.parent = parent;
        exceptionMap = new HashMap<ExceptionTypeSym, CodeLocation>(parent.exceptionMap);
        this.funcExceptionSet = funcExceptionSet;
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

    public CodeLocation noUncheckedExceptions() {
        for (Map.Entry<ExceptionTypeSym, CodeLocation> p : exceptionMap.entrySet()) {
            if (!funcExceptionSet.contains(p.getKey())) {
                // printExceptionSet();
                return p.getValue();
            }
        }
        return null;
    }

    public void mergeExceptions(ScopeContext rtn) {
        for (Map.Entry<ExceptionTypeSym, CodeLocation> p : rtn.exceptionMap.entrySet()) {
            if (!exceptionMap.containsKey(p.getKey())) {
                exceptionMap.put(p.getKey(), p.getValue());
            }
        }
    }
    public void mergeExceptions(HashMap<ExceptionTypeSym, CodeLocation> map) {
        for (Map.Entry<ExceptionTypeSym, CodeLocation> p : map.entrySet()) {
            if (!exceptionMap.containsKey(p.getKey())) {
                exceptionMap.put(p.getKey(), p.getValue());
            }
        }
    }

    public void removeException(TypeSym toTypeSym) {
        if (!(toTypeSym instanceof ExceptionTypeSym s))
            return;
        exceptionMap.remove(s);
    }

    public void printExceptionSet() {
        System.err.println(funcExceptionSet.size());
        for (ExceptionTypeSym t : funcExceptionSet) {
            System.err.println(t + t.name);
        }
    }
}
