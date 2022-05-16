package ast;

import typecheck.PathOutcome;
import typecheck.VisitEnv;

public class AugAssign extends NonFirstLayerStatement {
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
}
