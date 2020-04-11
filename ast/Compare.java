package ast;

import compile.SolCode;
import compile.Utils;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class Compare extends Expression {
    Expression left;
    CompareOperator op;
    Expression right;
    public Compare(Expression l, CompareOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        //TODO: not included: Is, NotIs, In, NotIn
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.NTCgenCons(env, now), r = right.NTCgenCons(env, now);

        env.addCons(l.genCons(r, Relation.EQ, env, location));
        if (op == CompareOperator.Eq || op == CompareOperator.NotEq) {
            //env.addCons(l.genCons(r, Relation.EQ, env, location));
        } else if (op == CompareOperator.Lt || op == CompareOperator.LtE
                || op == CompareOperator.Gt || op == CompareOperator.GtE) {
            env.addCons(l.genCons(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location));
        }
        env.addCons(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
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
        String ifNameRtn = env.ctxt + "." + "cmp" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location));

        return new Context(ifNameRtn, rightContext.lockName);
    }

    public String toSolCode() {
        return SolCode.toCompareOp(left.toSolCode(), Utils.toCompareOp(op), right.toSolCode());

    }
    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(left);
        rtn.add(right);
        return rtn;
    }
}
