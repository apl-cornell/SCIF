package ast;

import sherrlocUtils.Relation;
import typecheck.*;

public class ConstantExpr extends Expression {
    Constant value;
    public ConstantExpr(Constant value) {
        this.value = value;
    }

    @Override
    public ScopeContext NTCgenCons(NTCEnv env, ScopeContext parent) {
        //TODO: Constant are boolean
        ScopeContext now = new ScopeContext(this, parent);
        env.addCons(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }
    @Override
    public Context genConsVisit(VisitEnv env, boolean tail_position) {

        String ifNameRnt = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        return new Context(ifNameRnt, env.context.lockName, env.context.inLockName);
    }

    @Override
    public String toSolCode() {
        return compile.Utils.toConstant(value);
    }

    @Override
    public boolean typeMatch(Expression expression) {
        return expression instanceof ConstantExpr &&
                value.equals(((ConstantExpr) expression).value);
    }
}
