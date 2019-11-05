package ast;

import typecheck.Context;
import typecheck.VisitEnv;

import java.util.ArrayList;

public class EndorseBlock extends Statement {
    IfLabel l_from, l_to;
    ArrayList<Statement> body;

    public EndorseBlock(IfLabel l_from, IfLabel l_to, ArrayList<Statement> body) {
        this.l_from = l_from;
        this.l_to = l_to;
        this.body = body;
    }

    public Context genConsVisit(VisitEnv env) {
        
    }
}
