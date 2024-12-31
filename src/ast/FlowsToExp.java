package ast;

import compile.CompileEnv;
import compile.Utils;
import compile.ast.Call;
import compile.ast.SingleVar;
import compile.ast.Statement;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Name lhs, rhs;

    public FlowsToExp(Name a, Name b) {
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
    public compile.ast.Expression solidityCodeGen(List<Statement> result, CompileEnv code) {
        // lhs => rhs:
        // trusts(lhs, rhs);
        return new Call(Utils.TRUSTS_CALL, List.of(new SingleVar("address(" + rhs.id + ")"), new SingleVar("address(" + lhs.id + ")")));
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
    public ScopeContext generateConstraints(NTCEnv env, ScopeContext parent) {
        if (lhs != null && rhs != null) {
            ScopeContext now = new ScopeContext(this, parent);
            Name left = (Name) lhs, right = (Name) rhs;
            Sym lsym = env.getCurSym(left.id), rsym = env.getCurSym(right.id);
            if (lsym instanceof VarSym && rsym instanceof VarSym) {
                boolean lpass, rpass;
                lpass = ((VarSym) lsym).isPrincipalVar();
                rpass = ((VarSym) rsym).isPrincipalVar();
                assert lpass : "dynamic trust queries can only happen between final address variables: " + lsym.getName() + " at " + location.errString();
                assert rpass : "dynamic trust queries can only happen between final address variables: " + rsym.getName() + " at " + location.errString();
                env.addCons(
                            now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
                return now;

            }
        }
        assert false: location.errString();
        return null;
    }

    @Override
    public List<Node> children() {
        List<Node> rtn = new ArrayList<>();
        rtn.add(lhs);
        rtn.add(rhs);
        return rtn;
    }
    @Override
    public java.util.Map<String, compile.ast.Type> readMap(CompileEnv code) {
        Map<String, Type> result = lhs.readMap(code);
        result.putAll(rhs.readMap(code));
        return result;
    }
}
