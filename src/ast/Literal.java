package ast;

import typecheck.sherrlocUtils.Constraint;
import typecheck.sherrlocUtils.Inequality;
import typecheck.sherrlocUtils.Relation;
import typecheck.*;

public abstract class Literal extends Expression {

    @Override
    public ExpOutcome genConsVisit(VisitEnv env, boolean tail_position) {
        String ifNameRtn = "LITERAL..." + location.toString();
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtn), env.hypothesis(), location,
                env.curContractSym().getName(),
                "Control flow must be trusted to use this literal"));

        return new ExpOutcome(ifNameRtn,
                new PathOutcome(new PsiUnit(new Context(ifNamePc, env.inContext.lambda))));
    }

    @Override
    public ScopeContext ntcGenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // con: tgt should be a supertype of v
        if (this instanceof Num) {
            env.addCons(now.genCons(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location));
        } else if (this instanceof Str) {
            env.addCons(now.genCons(env.getSymName(BuiltInT.STRING), Relation.EQ, env, location));
        }
        return now;
    }

    public String toSHErrLocFmt() {
        String ifNameRtn = "LITERAL..." + location.toString();
        return ifNameRtn;
    }
}
