package ast;

import compile.SolCode;
import typecheck.*;

import java.util.HashSet;

public abstract class Expression extends Node {

    public VarSym getVarInfo(VisitEnv env, boolean tail_position) {
        return null;
    }

    public VarSym getVarInfo(NTCEnv env) {
        return null;
    }

    public String toSolCode() {
        return "unknown exp";
    }

    public abstract ExpOutcome genConsVisit(VisitEnv env, boolean tail_position);

    @Override
    public void solidityCodeGen(SolCode code) {
        code.addLine(toSolCode());
    }

    public void SolCodeGenStmt(SolCode code) {
        logger.debug("SolCodeGenStmt: Being Called");
        code.addLine(toSolCode() + ";");
    }

    public abstract boolean typeMatch(Expression expression);
}
