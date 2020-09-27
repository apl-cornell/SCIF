package ast;

public class Str extends Literal {
    String value;
    public Str(String x) {
        value = x;
    }

    @Override
    public String toSolCode() {
        return value;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Str &&
                value.equals(((Str) expression).value);
    }
}
