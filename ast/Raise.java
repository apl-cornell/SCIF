package ast;

import typecheck.ContractSym;
import typecheck.PathOutcome;
import typecheck.VisitEnv;

public class Raise extends FirstLayerStatement {
    Expression exception;
    Expression from;
    public Raise(Expression exception, Expression from) {
        this.exception = exception;
        this.from = from;
    }
    public Raise(Expression exception) {
        this.exception = exception;
    }
    public Raise() {
    }

    @Override
    public void globalInfoVisit(ContractSym contractSym) {

    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }
}
