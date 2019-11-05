package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstantExpr extends Expression {
    Constant value;
    public ConstantExpr(Constant value) {
        this.value = value;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {

        String ifNameRnt = Utils.getLabelNamePc(env.ctxt);
        return new Context(ifNameRnt, env.prevContext.lockName);
    }
}
