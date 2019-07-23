package ast;

import utils.CodeLocation;

public class TrailerExpr extends Expression {
    Expression value;

    public void setValue(Expression value) {
        this.value = value;
    }
}
