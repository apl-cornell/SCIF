package ast;

import java.util.ArrayList;
import java.util.List;

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
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        return rtn;
    }
}
