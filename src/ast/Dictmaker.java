package ast;

import java.util.List;
import typecheck.*;

import java.util.ArrayList;

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
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        //TODO
        return null;
        /*String ifNameRtn = scopeContext.getSHErrLocName() + "." + "dictmaker" + location.toString();
        String prevLock = env.prevContext.lambda;
        Context lasttmp = null;
        for (Expression value: values) {
            if (lasttmp != null) {
                env.cons.add(new Constraint(new Inequality(prevLock, Relation.EQ, lasttmp.lambda), env.hypothesis, location, env.curContractSym.name,
                        Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
                env.prevContext.lambda = prevLock;
            }
            Context tmp = value.genConsVisit(env, );
            String ifNameValue = tmp.valueLabelName;
            env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                    ""));
            lasttmp = tmp;
        }
        return lasttmp;*/
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Dictmaker &&
                Utils.arrayExpressionTypeMatch(keys, ((Dictmaker) expression).keys) &&
                Utils.arrayExpressionTypeMatch(values, ((Dictmaker) expression).values);
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        return null;
    }
    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.addAll(keys);
        rtn.addAll(values);
        return rtn;
    }
}
