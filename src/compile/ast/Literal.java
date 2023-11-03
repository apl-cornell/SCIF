package compile.ast;

public class Literal extends Expression {
    String value;
    public Literal(String value) {
        this.value = value;
    }

    @Override
    public String toSolCode() {
        return value;
    }
}
