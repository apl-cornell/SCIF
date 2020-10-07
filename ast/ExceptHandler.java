package ast;

import java.util.ArrayList;

public class ExceptHandler extends NonFirstLayerStatement {
    Expression type;
    Str name;
    ArrayList<Statement> body;
    public ExceptHandler(Expression type, Str name, ArrayList<Statement> body) {
        this.type = type;
        this.name = name;
        this.body = body;
    }
    public void setBody(ArrayList<Statement> body) {
        this.body = body;
    }

}
