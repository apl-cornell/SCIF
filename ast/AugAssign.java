package ast;

import utils.CodeLocation;

public class AugAssign extends Statement {
    Expression target;
    BinaryOperator op;
    Expression value;
    public AugAssign(Expression target, BinaryOperator op, Expression value) {
        this.target = target;
        this.op = op;
        this.value = value;
    }

}
