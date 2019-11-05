package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Dictmaker extends Expression {
    ArrayList<Expression> keys, values;
    public Dictmaker(ArrayList<Expression> keys, ArrayList<Expression> values) {
        this.keys = keys;
        this.values = values;
    }
    public Dictmaker() {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
    }
    public void addPair(Expression key, Expression value) {
        this.keys.add(key);
        this.values.add(value);
    }
    @Override
    public Context genConsVisit(VisitEnv env) {
        String ifNameRtn = env.ctxt + "." + "dictmaker" + location.toString();
        String prevLock = env.prevContext.lockName;
        Context lasttmp = null;
        for (Expression value: values) {
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
