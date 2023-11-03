package compile.ast;

public class Subscript extends Expression {
    Expression target;
    Expression index;

    public Subscript(Expression target, Expression index) {
        this.target = target;
        this.index = index;
    }


    @Override
    public String toSolCode() {
        return target.toSolCode() + "[" + index.toSolCode() + "]";
    }
}
