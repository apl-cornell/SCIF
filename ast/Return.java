package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Return extends Statement {
    Expression value;
    public Return(Expression value) {
        this.value = value;
    }
    public Return() {
        this.value = null;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        String prevLock = env.prevContext.lockName;
        if (value == null) return new Context(null, prevLock);
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        Context expContext = value.genConsVisit(env);
        String ifNameValue = expContext.valueLabelName;

        int occur = env.ctxt.indexOf(".");
        FuncInfo funcInfo = env.funcMap.get(occur >= 0 ? env.ctxt.substring(0, occur) : env.ctxt);
        String ifNameRtnValue = funcInfo.getLabelNameRtnValue();
        String ifNameRtnLock = funcInfo.getLabelNameRtnLock();
        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtnValue), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(expContext.lockName, ifNameRtnLock), env.hypothesis, location));
        return new Context(null, prevLock);
    }
}
