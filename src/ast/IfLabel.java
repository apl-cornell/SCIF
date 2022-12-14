package ast;

import typecheck.ScopeContext;
import typecheck.VisitEnv;

import java.util.HashSet;

public abstract class IfLabel extends Expression {

    /**
     * Generate a corresponding name in ShErrLoc format for this label's value
     * @param defContext
     * @return
     */
    public abstract String toSherrlocFmt(ScopeContext defContext);

    // public abstract String toSherrlocFmt(String namespace);

    // public abstract String toSherrlocFmt(String k, String v);

    // public abstract String toSherrlocFmtApply(HashSet<String> strSet, int no);

    // public abstract void replace(String k, String v);

    // public abstract void findPrincipal(HashSet<String> principalSet, String getRidOf);

    // public abstract void findPrincipal(HashSet<String> principalSet);

    public abstract boolean typeMatch(IfLabel begin_pc);

}
