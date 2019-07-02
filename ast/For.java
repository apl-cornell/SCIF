package ast;

import java.util.ArrayList;

public class For extends Statement {
    Variable target;
    Expression iter;
    ArrayList<Statement> body;
    ArrayList<Statement> orelse;
    public For(Variable target, Expression iter, ArrayList<Statement> body, ArrayList<Statement> orelse) {
        this.target = target;
        this.iter = iter;
        this.body = body;
        this.orelse = orelse;
    }
    public For(Variable target, Expression iter, ArrayList<Statement> body) {
        this.target = target;
        this.iter = iter;
        this.body = body;
        this.orelse = null;
    }
    //TODO: gen Constaints
}
