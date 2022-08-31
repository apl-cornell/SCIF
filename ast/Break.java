package ast;

import compile.SolCode;
import typecheck.PathOutcome;
import typecheck.VisitEnv;

public class Break extends Statement {
    @Override
    public void SolCodeGen(SolCode code) {
        code.addBreak();
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}
