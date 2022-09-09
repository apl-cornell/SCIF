package ast;

public class Num<T extends Number> extends Literal {
    T value;
    public Num(T x) {
        value = x;
    }

    @Override
    public String toSolCode() {
        return value.toString();
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Num &&
                value.equals(((Num) expression).value);
    }
}
