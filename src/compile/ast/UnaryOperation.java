package compile.ast;

public class UnaryOperation extends Expression {
    String op;
    Expression exp;

    public UnaryOperation(String op, Expression exp) {
        this.op = op;
        this.exp = exp;
    }

    @Override
    public String toSolCode() {
        return op + exp.toSolCode();
    }
}
