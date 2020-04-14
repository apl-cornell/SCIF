package ast;

import compile.SolCode;
import typecheck.*;

public class Expression extends Statement {
    public VarSym getVarInfo(VisitEnv env) {
        return new VarSym();
    }
    public VarSym getVarInfo(NTCEnv env) {
        return new VarSym();
    }

    public String toSolCode() { return "unknown exp"; }

    @Override
    public void SolCodeGen(SolCode code) {
        code.addLine(toSolCode());
    }
}
