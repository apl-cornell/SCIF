package ast;

import java.util.ArrayList;

public class Try extends NonFirstLayerStatement {
    ArrayList<Statement> body;
    ArrayList<ExceptHandler> handlers;
    ArrayList<Statement> orelse;
    ArrayList<Statement> finalbody;
    public Try(ArrayList<Statement> body, ArrayList<ExceptHandler> handlers, ArrayList<Statement> orelse, ArrayList<Statement> finalbody) {
        this.body = body;
        this.handlers = handlers;
        this.orelse = orelse;
        this.finalbody = finalbody;
    }

}
