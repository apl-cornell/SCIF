package ast;

public class AugAssign extends NonFirstLayerStatement {
    Expression target;
    BinaryOperator op;
    Expression value;
    public AugAssign(Expression target, BinaryOperator op, Expression value) {
        this.target = target;
        this.op = op;
        this.value = value;
    }

}
