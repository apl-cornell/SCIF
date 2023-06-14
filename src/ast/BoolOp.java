package ast;

import compile.SolCode;
import compile.Utils;
import java.util.List;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
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
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        ScopeContext l = left.ntcGenCons(env, now), r = right.ntcGenCons(env, now);
        env.addCons(now.genCons(l, Relation.LEQ, env, location));
        env.addCons(now.genCons(r, Relation.LEQ, env, location));
        env.addCons(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        env.inContext = beginContext;
        ExpOutcome lo = left.genConsVisit(env, false);
        String ifNameLeft = lo.valueLabelName;

        env.inContext = new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        ExpOutcome ro = right.genConsVisit(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bool" + location.toString();

        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis(), location,
                env.curContractSym().getName(),
                "Integrity of left hand expression doesn't flow to value of this boolean operation"));
        env.cons.add(
                new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of right hand expression doesn't flow to value of this boolean operation"));

        typecheck.Utils.contextFlow(env, ro.psi.getNormalPath().c, endContext, right.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.cons.add(new Constraint(
                    new Inequality(endContext.lambda, beginContext.lambda),
                    env.hypothesis(), location, env.curContractSym().getName(),
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION));
        }

        ro.psi.join(lo.psi);
        ro.psi.setNormalPath(endContext);

        return new ExpOutcome(ifNameRtn, ro.psi);
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
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(left);
        rtn.add(right);
        return rtn;
    }
}
