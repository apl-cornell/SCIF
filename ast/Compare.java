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
    public Context genConsVisit(VisitEnv env, boolean tail_position) {
        Context context = env.context;
        Context curContext = new Context(context.valueLabelName, typecheck.Utils.getLabelNameLock(location), context.inLockName);

        Context leftContext = left.genConsVisit(env, false);
        String ifNameLeft = leftContext.valueLabelName;
        // env.prevContext = prevContext = leftContext;

        // env.prevContext.lockName = leftContext.lockName;
        env.context = context;
        Context rightContext = right.genConsVisit(env, false);
        String ifNameRight = rightContext.valueLabelName;
        // env.prevContext = prevContext = rightContext;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "cmp" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, left.location, env.curContractSym.name,
                "Integrity of left hand expression doesn't flow to value of this compare operation"));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, right.location, env.curContractSym.name,
                "Integrity of right hand expression doesn't flow to value of this compare operation"));

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.inLockName), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        } else {
            env.cons.add(new Constraint(new Inequality(curContext.lockName, context.lockName), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_LAST_OPERATION));
        }

        return new Context(ifNameRtn, curContext.lockName, curContext.inLockName);
    }

    public String toSolCode() {
        return SolCode.toCompareOp(left.toSolCode(), Utils.toCompareOp(op), right.toSolCode());

    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof Compare &&
                left.typeMatch(((Compare) expression).left) &&
                op == ((Compare) expression).op &&
                right.typeMatch(((Compare) expression).right);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(left);
        rtn.add(right);
        return rtn;
    }
}
