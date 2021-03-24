package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

public abstract class Literal extends Expression {
    @Override
    public Context genConsVisit(VisitEnv env) {
        String ifNameRtn = "LITERAL..." + location.toString();
        String ifNamePc = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtn), env.hypothesis, location));

        return new Context(ifNameRtn, env.prevContext.lockName);
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        ScopeContext now = new ScopeContext(this, parent);
        // con: tgt should be a supertype of v
        if (this instanceof Num) {
            env.cons.add(now.genCons(env.getSymName(BuiltInT.UINT), Relation.EQ, env, location));
        } else if (this instanceof Str) {
            env.cons.add(now.genCons(env.getSymName(BuiltInT.STRING), Relation.EQ, env, location));
        }
        return now;
    }

    public String toSHErrLocFmt() {
        String ifNameRtn = "LITERAL..." + location.toString();
        return ifNameRtn;
    }
}
