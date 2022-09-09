package ast;

import compile.SolCode;
import typecheck.NTCEnv;
import typecheck.PathOutcome;
import typecheck.ScopeContext;
import typecheck.VisitEnv;

public class AugAssign extends Statement {

    Expression target;
    BinaryOperator op;
    Expression value;

    public AugAssign(Expression target, BinaryOperator op, Expression value) {
        this.target = target;
        this.op = op;
        this.value = value;
    }

    @Override
    public PathOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        return null;
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }

    @Override
    public void solidityCodeGen(SolCode code) {

    }
}
