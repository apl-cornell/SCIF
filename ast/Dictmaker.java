package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

//TODO: incomplete
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
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "dictmaker" + location.toString();
        String prevLock = env.prevContext.lockName;
        Context lasttmp = null;
        for (Expression value: values) {
            if (lasttmp != null) {
                env.cons.add(new Constraint(new Inequality(prevLock, Relation.EQ, lasttmp.lockName), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                env.prevContext.lockName = prevLock;
            }
            Context tmp = value.genConsVisit(env);
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                    ""));
            lasttmp = tmp;
        }
        return lasttmp;
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Dictmaker &&
                Utils.arrayExpressionTypeMatch(keys, ((Dictmaker) expression).keys) &&
                Utils.arrayExpressionTypeMatch(values, ((Dictmaker) expression).values);
    }
}
