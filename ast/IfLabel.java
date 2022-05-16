package ast;

import typecheck.VisitEnv;

import java.util.HashSet;

public abstract class IfLabel extends Expression {
    public abstract String toSherrlocFmt();
    public abstract String toSherrlocFmt(String namespace);
    public abstract String toSherrlocFmt(String k, String v);
    public abstract String toSherrlocFmtApply(HashSet<String> strSet, int no);
    public abstract void replace(String k, String v);

    public abstract void findPrincipal(HashSet<String> principalSet, String getRidOf);

    @Override
    public abstract void findPrincipal(HashSet<String> principalSet);

    public abstract boolean typeMatch(IfLabel begin_pc);

}
