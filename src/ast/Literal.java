package ast;

import typecheck.sherrlocUtils.Relation;
import typecheck.*;

public abstract class Literal extends Expression {

    @Override
    protected ExpOutcome genFlowConstraints(VisitEnv env, boolean tail_position) {
        String ifNameRtn = "LITERAL..." + location.toString();
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        env.checkFlow(ifNamePc, ifNameRtn, location,
                "A literal is trusted no more than the control flow that evaluates it");

        return new ExpOutcome(ifNameRtn,
                new PathOutcome(new PsiUnit(new Context(ifNamePc, env.inContext.lambda))));
    }

    @Override
    public ScopeContext genTypeConstraints(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // con: tgt should be a supertype of v
        if (this instanceof Num) {
            env.addCons(now.genTypeConstraints(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location, "A number is a uint"));
        } else if (this instanceof Str) {
            env.addCons(now.genTypeConstraints(env.getSymName(BuiltInT.STRING), Relation.EQ, env, location, "A string literal is a string"));
        }
        return now;
    }

    public String toSHErrLocFmt() {
        String ifNameRtn = "LITERAL..." + location.toString();
        return ifNameRtn;
    }

    @Override
    protected TypeSym staticType(VisitEnv env) {
        return env.builtinType(this instanceof Num ? BuiltInT.UINT : BuiltInT.STRING);
    }
}
