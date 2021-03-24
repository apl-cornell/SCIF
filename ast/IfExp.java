package ast;

public class IfExp extends Expression {
    Expression test, body, orelse; //TODO
    public IfExp(Expression t, Expression b, Expression o) {
        test = t;
        body = b;
        orelse = o;
    }


    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof IfExp &&
                test.typeMatch(((IfExp) expression).test) &&
                body.typeMatch(((IfExp) expression).body) &&
                orelse.typeMatch(((IfExp) expression).orelse);
    }
}
