package ast;

import compile.SolCode;
import typecheck.*;

import java.util.HashSet;

public abstract class Expression extends Node {
    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        return new VarSym();
    }
    public VarSym getVarInfo(NTCEnv env) {
        return new VarSym();
    }

    public String toSolCode() { return "unknown exp"; }

    public abstract ExpOutcome genConsVisit(VisitEnv env, boolean tail_position);

    @Override
    public void SolCodeGen(SolCode code) {
        code.addLine(toSolCode());
    }

    public void SolCodeGenStmt(SolCode code) {
        logger.debug("SolCodeGenStmt: Being Called");
        code.addLine(toSolCode() + ";");
    }

    @Override
    public void findPrincipal(HashSet<String> principalSet) {

    }
    public abstract boolean typeMatch(Expression expression);
}
