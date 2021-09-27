package ast;

import compile.SolCode;
import compile.Utils;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

public class BoolOp extends Expression {
    BoolOperator op;
    Expression left, right;
    public BoolOp(Expression l, BoolOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.NTCgenCons(env, now), r = right.NTCgenCons(env, now);
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
        env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, leftContext.lockName), env.hypothesis, location, env.curContractSym.name,
                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

        env.prevContext.lockName = leftContext.lockName;
        Context rightContext = right.genConsVisit(env);
        String ifNameRight = rightContext.valueLabelName;
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bool" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                "Integrity of left hand expression flows to value of this boolean operation"));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                "Integrity of right hand expression flows to value of this boolean operation"));

        return new Context(ifNameRtn, rightContext.lockName);
    }

    public String toSolCode() {
        return SolCode.toBoolOp(left.toSolCode(), Utils.toBoolOp(op), right.toSolCode());
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof BoolOp &&
                op == ((BoolOp) expression).op &&
                left.typeMatch(((BoolOp) expression).left) &&
                right.typeMatch(((BoolOp) expression).right);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(left);
        rtn.add(right);
        return rtn;
    }
}
