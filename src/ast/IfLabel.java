package ast;

import java.util.Map;
import typecheck.Label;
import typecheck.ScopeContext;
import typecheck.VarSym;
import typecheck.VisitEnv;

import java.util.HashSet;

public abstract class IfLabel extends Expression {

    /**
     * Generate a corresponding name in ShErrLoc format for this label's value
     * @return
     */
    // public abstract String toSHErrLocFmt();

    // public abstract String toSHErrLocFmt(String namespace);

    // public abstract String toSHErrLocFmt(String k, String v);

    // public abstract String toSherrlocFmtApply(HashSet<String> strSet, int no);

    // public abstract void replace(String k, String v);

    // public abstract void findPrincipal(HashSet<String> principalSet, String getRidOf);

    // public abstract void findPrincipal(HashSet<String> principalSet);

    public abstract boolean typeMatch(IfLabel begin_pc);
    public VarSym getVarInfo(VisitEnv env, boolean tail_position, Map<String, String> dependentMapping) {
        assert false;
        return null;
    }

}
