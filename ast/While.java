package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
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
    public Context genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;

        String prevLock = env.prevContext.lockName;
        Context testContext = test.genConsVisit(env);
        String IfNameTestValue = testContext.valueLabelName;
        String IfNamePcBefore = Utils.getLabelNamePc(env.ctxt);
        env.ctxt += ".While" + location.toString();
        String IfNamePcAfter = Utils.getLabelNamePc(env.ctxt);
        env.cons.add(new Constraint(new Inequality(IfNamePcBefore, IfNamePcAfter), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(IfNameTestValue, IfNamePcAfter), env.hypothesis, location));

        env.varNameMap.incLayer();
        Context lastContext = new Context(testContext), prev2 = null;
        for (Statement stmt : body) {
            if (prev2 != null) {
                env.cons.add(new Constraint(new Inequality(lastContext.lockName, Relation.EQ, prev2.lockName), env.hypothesis, location));
            }
            Context tmp = stmt.genConsVisit(env);
            env.prevContext = tmp;
            prev2 = lastContext;
            lastContext = new Context(tmp);
        }
        env.varNameMap.decLayer();
        env.cons.add(new Constraint(new Inequality(lastContext.lockName, Relation.EQ, prevLock), env.hypothesis, location));

        env.ctxt = originalCtxt;
        return lastContext;
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
