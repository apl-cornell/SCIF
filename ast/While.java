package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class While extends Statement {
    Expression test;
    ArrayList<Statement> body;
    ArrayList<Statement> orelse; //TODO: ignoring for now
    public While(Expression test, ArrayList<Statement> body, ArrayList<Statement> orelse) {
        this.test = test;
        this.body = body;
        this.orelse = orelse;
    }
    public While(Expression test, ArrayList<Statement> body) {
        this.test = test;
        this.body = body;
        this.orelse = null;
    }

    @Override
    public String genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;

        String IfNameTest = test.genConsVisit(env);
        String IfNamePcBefore = Utils.getLabelNamePc(env.ctxt);
        env.ctxt += ".While" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(env.ctxt);
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(IfNameTest, IfNamePcAfter), env.hypothesis, location));


        env.varNameMap.incLayer();
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        env.varNameMap.decLayer();

        env.ctxt = originalCtxt;
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        for (Statement stmt : body) {
            stmt.findPrincipal(principalSet);
        }
        for (Statement stmt : orelse) {
            stmt.findPrincipal(principalSet);
        }
    }

}
