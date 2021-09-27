package ast;

import compile.SolCode;
import compile.Utils;
import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

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
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        logger.debug("binOp:");
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.NTCgenCons(env, now);
        // logger.debug("binOp/left:");
        // logger.debug(l.toString());
        ScopeContext r = right.NTCgenCons(env, now);
        // logger.debug("binOp/right:");
        // logger.debug(r.toString());
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
        env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, leftContext.lockName), env.hypothesis, location, env.curContractSym.name,
                typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));

        env.prevContext.lockName = leftContext.lockName;
        // logger.debug("binOp/right:\n");
        // logger.debug(right.toString());
        Context rightContext = right.genConsVisit(env);
        String ifNameRight = rightContext.valueLabelName;
        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bin" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                "Integrity of left hand expression flows to value of this binary operation"));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                "Integrity of right hand expression flows to value of this binary operation"));

        return new Context(ifNameRtn, rightContext.lockName);
    }

    public String toSolCode() {
        return SolCode.toBinOp(left.toSolCode(), Utils.toBinOp(op), right.toSolCode());
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof BinOp &&
                left.typeMatch(((BinOp) expression).left) &&
                op == ((BinOp) expression).op &&
                right.typeMatch(((BinOp) expression).right);
    }

    @Override
    public ArrayList<Node> children() {
        ArrayList<Node> rtn = new ArrayList<>();
        rtn.add(left);
        rtn.add(right);
        return rtn;
    }
}
