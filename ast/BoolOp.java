package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class BoolOp extends Expression {
    BoolOperator op;
    Expression left, right;
    public BoolOp(Expression l, BoolOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        NTCContext now = new NTCContext(this, parent);
        NTCContext l = left.NTCgenCons(env, now), r = right.NTCgenCons(env, now);
        env.cons.add(now.genCons(l, Relation.LEQ, env, location));
        env.cons.add(now.genCons(r, Relation.LEQ, env, location));
        env.cons.add(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }
    @Override
    public Context genConsVisit(VisitEnv env) {
        String prevLockName = env.prevContext.lockName;

        Context leftContext = left.genConsVisit(env);
        String ifNameLeft = leftContext.valueLabelName;
        env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, leftContext.lockName), env.hypothesis, location));

        env.prevContext.lockName = leftContext.lockName;
        Context rightContext = right.genConsVisit(env);
        String ifNameRight = rightContext.valueLabelName;
        String ifNameRtn = env.ctxt + "." + "bool" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location));

        return new Context(ifNameRtn, rightContext.lockName);
    }
}
