package ast;

import java.util.ArrayList;
import java.util.List;
import typecheck.BuiltInT;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.Sym;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;

public class FlowsToExp extends Expression {
    Expression lhs, rhs;

    public FlowsToExp(Expression a, Expression b) {
        lhs = a;
        rhs = b;
    }

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        env.inContext = beginContext;
        ExpOutcome lo = lhs.genConsVisit(env, false);
        String ifNameLeft = lo.valueLabelName;

        env.inContext = new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        ExpOutcome ro = rhs.genConsVisit(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bool" + location.toString();

        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis(), location,
                env.curContractSym().getName(),
                "Integrity of left hand expression doesn't flow to value of this flows-to operation"));
        env.cons.add(
                new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis(), location,
                        env.curContractSym().getName(),
                        "Integrity of right hand expression doesn't flow to value of this flows-to operation"));

        typecheck.Utils.contextFlow(env, ro.psi.getNormalPath().c, endContext, rhs.location);
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

    @Override
    public boolean typeMatch(Expression expression) {
        if (expression instanceof FlowsToExp o) {
            return lhs.typeMatch(o.lhs) && rhs.typeMatch(o.rhs);
        } else {
            return false;
        }
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        if (lhs instanceof Name && rhs instanceof Name) {
            ScopeContext now = new ScopeContext(this, parent);
            Name left = (Name) lhs, right = (Name) rhs;
            Sym lsym = env.getCurSym(left.id), rsym = env.getCurSym(right.id);
            if (lsym instanceof VarSym && rsym instanceof VarSym) {
                if (((VarSym) lsym).isFinal && ((VarSym) rsym).isFinal && ((VarSym) lsym).isPrincipalVar() && ((VarSym) rsym).isPrincipalVar()) {
                    env.addCons(
                            now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
                    return now;
                }
            }
        }
        throw new RuntimeException("dynamic trust queries can only happen between final address variables");
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(lhs);
        rtn.add(rhs);
        return rtn;
    }
}
