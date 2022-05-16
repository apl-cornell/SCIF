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
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(location), typecheck.Utils.getLabelNameLock(location));

        env.inContext = beginContext;
        ExpOutcome lo = left.genConsVisit(env, false);
        String ifNameLeft = lo.valueLabelName;

        env.inContext = new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        ExpOutcome ro = right.genConsVisit(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bin" + location.toString();

        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                "Integrity of left hand expression doesn't flow to value of this binary operation"));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location, env.curContractSym.name,
                "Integrity of right hand expression doesn't flow to value of this binary operation"));

        typecheck.Utils.contextFlow(env, ro.psi.getNormalPath().c, endContext, right.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(new Inequality(ro.psi.getNormalPath().c.lambda, beginContext.lambda), env.hypothesis, location, env.curContractSym.name,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        ro.psi.join(lo.psi);
        ro.psi.setNormalPath(endContext);

        return new ExpOutcome(ifNameRtn, ro.psi);
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
