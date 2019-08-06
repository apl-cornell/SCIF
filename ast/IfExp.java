package ast;

public class IfExp extends Expression {
    Expression test, body, orelse; //TODO
    public IfExp(Expression t, Expression b, Expression o) {
        test = t;
        body = b;
        orelse = o;
    }


}
