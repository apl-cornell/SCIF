package ast;

import compile.SolCode;
import compile.Utils;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

// Assume operators are INT
public class BinOp extends Expression {
    Expression left, right;
    BinaryOperator op;
    public BinOp(Expression l, BinaryOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        logger.debug("binOp:");
        NTCContext now = new NTCContext(this, parent);
        NTCContext l = left.NTCgenCons(env, now);
        logger.debug("binOp/left:");
        logger.debug(l.toString());
        NTCContext r = right.NTCgenCons(env, now);
        logger.debug("binOp/right:");
        logger.debug(r.toString());
        env.cons.add(now.genCons(l, Relation.LEQ, env, location));
        env.cons.add(now.genCons(r, Relation.LEQ, env, location));
        env.cons.add(now.genCons(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location));
        return now;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        String prevLockName = env.prevContext.lockName;

        Context leftContext = left.genConsVisit(env);
        String ifNameLeft = leftContext.valueLabelName;
        env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, leftContext.lockName), env.hypothesis, location));

        env.prevContext.lockName = leftContext.lockName;
        logger.debug("binOp/right:\n");
        logger.debug(right.toString());
        Context rightContext = right.genConsVisit(env);
        String ifNameRight = rightContext.valueLabelName;
        String ifNameRtn = env.ctxt + "." + "bin" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location));

        return new Context(ifNameRtn, rightContext.lockName);
    }

    public String toSolCode() {
        return SolCode.toBinOp(left.toSolCode(), Utils.toBinOp(op), right.toSolCode());
    }
}
