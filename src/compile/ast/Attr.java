package compile.ast;

public class Attr extends Expression {
    Expression target;
    String attr;

    public Attr(Expression target, String attr) {
        this.target = target;
        this.attr = attr;
    }

    @Override
    public String toSolCode() {
        return target.toSolCode() + "." + attr;
    }
}
