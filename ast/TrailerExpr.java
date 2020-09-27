package ast;

public abstract class TrailerExpr extends Expression {
    Expression value;

    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof TrailerExpr &&
                value.typeMatch(((TrailerExpr) expression).value);
    }
}
