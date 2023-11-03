package compile.ast;

public class BinaryExpression extends Expression {
    String op;
    Expression left;
    Expression right;


    public BinaryExpression(String op, Expression left, Expression right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toSolCode() {
        return "(" + left.toSolCode() + " " + op + " " + right.toSolCode() + ")";
    }
}
