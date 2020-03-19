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
}
