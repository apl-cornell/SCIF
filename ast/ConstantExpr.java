package ast;

import sherrlocUtils.Relation;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstantExpr extends Expression {
    Constant value;
    public ConstantExpr(Constant value) {
        this.value = value;
    }

    @Override
    public NTCContext NTCgenCons(NTCEnv env, NTCContext parent) {
        //TODO: Constant are boolean
        NTCContext now = new NTCContext(this, parent);
        env.addCons(now.genCons(env.getSymName(BuiltInT.BOOL), Relation.EQ, env, location));
        return now;
    }
    @Override
    public Context genConsVisit(VisitEnv env) {

        String ifNameRnt = Utils.getLabelNamePc(env.ctxt);
        return new Context(ifNameRnt, env.prevContext.lockName);
    }

    @Override
    public String toSolCode() {
        return compile.Utils.toConstant(value);
    }
}
