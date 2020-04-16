package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;

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
    public Context genConsVisit(VisitEnv env) {

        String ifNameRnt = Utils.getLabelNamePc(scopeContext.getSHErrLocName());
        return new Context(ifNameRnt, env.prevContext.lockName);
    }

    @Override
    public String toSolCode() {
        return compile.Utils.toConstant(value);
    }
}
