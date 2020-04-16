package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Setmaker extends Expression {
    ArrayList<Expression> elements;
    public Setmaker(ArrayList<Expression> elements) {
        this.elements = elements;
    }
    public Setmaker() {
        this.elements = new ArrayList<>();
    }
    public void addElement(Expression element) {
        this.elements.add(element);
    }
    @Override
    public Context genConsVisit(VisitEnv env) {
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "setmaker" + location.toString();
        String prevLock = env.prevContext.lockName;
        Context lasttmp = null;
        for (Expression value: elements) {
            if (lasttmp != null) {
                env.cons.add(new Constraint(new Inequality(prevLock, Relation.EQ, lasttmp.lockName), env.hypothesis, location));
                env.prevContext.lockName = prevLock;
            }
            Context tmp = value.genConsVisit(env);
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location));
            lasttmp = tmp;
        }
        return lasttmp;
    }
}
