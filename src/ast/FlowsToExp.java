package ast;

import compile.CompileEnv;
import compile.Utils;
import compile.ast.Call;
import compile.ast.SingleVar;
import compile.ast.Statement;
import compile.ast.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import typecheck.BuiltInT;
import typecheck.Context;
import typecheck.ExpOutcome;
import typecheck.NTCEnv;
import typecheck.ScopeContext;
import typecheck.TypeSym;
import typecheck.VarSym;
import typecheck.VisitEnv;
import typecheck.exceptions.SemanticException;
import typecheck.sherrlocUtils.Relation;

public class FlowsToExp extends Expression {
    Name lhs, rhs;

    public FlowsToExp(Name a, Name b) {
        lhs = a;
        rhs = b;
    }

    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position)
            throws SemanticException {
        Context beginContext = env.inContext;
        Context endContext = new Context(typecheck.Utils.getLabelNamePc(toSHErrLocFmt()),
                typecheck.Utils.getLabelNameLock(toSHErrLocFmt()));

        env.inContext = beginContext;
        ExpOutcome lo = lhs.genIFConstraints(env, false);
        String ifNameLeft = lo.valueLabelName;

        env.inContext = new Context(lo.psi.getNormalPath().c.pc, beginContext.lambda);
        ExpOutcome ro = rhs.genIFConstraints(env, false);
        String ifNameRight = ro.valueLabelName;

        String ifNameRtn = scopeContext.getSHErrLocName() + "." + "bool" + location.toString();

        env.checkFlow(ifNameLeft, ifNameRtn, location,
                "The result of a flows-to test is trusted no more than its left operand");
        env.checkFlow(ifNameRight, ifNameRtn, location,
                "The result of a flows-to test is trusted no more than its right operand");

        typecheck.Utils.contextFlow(env, ro.psi.getNormalPath().c, endContext, rhs.location);
        // env.outContext = endContext;

        if (!tail_position) {
            env.checkFlow(endContext.lambda, beginContext.lambda, location,
                    typecheck.Utils.ERROR_MESSAGE_LOCK_IN_NONLAST_OPERATION);
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
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) throws SemanticException {
        ScopeContext now = new ScopeContext(this, parent);
        for (Name operand : List.of((Name) lhs, (Name) rhs)) {
            if (!(env.getCurSym(operand.id) instanceof VarSym v) || !v.isPrincipalTyped()) {
                throw new SemanticException(
                        "dynamic trust queries can only happen between address or contract variables: "
                                + operand.id, location);
            }
        }
        env.addCons(now.genTypeConstraints(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location, "A flows-to test returns a bool"));
        return now;
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

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return env.builtinType(BuiltInT.BOOL);
    }
}
