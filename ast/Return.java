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
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);

        int occur = env.ctxt.indexOf(".");
        FuncInfo funcInfo = env.funcMap.get(occur >= 0 ? env.ctxt.substring(0, occur) : env.ctxt);
        String ifNameRtnLock = funcInfo.getLabelNameRtnLock();
        String ifNameRtnValue = funcInfo.getLabelNameRtnValue();
        if (value == null) {
            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location));
            return new Context(null, prevLock);
        }

        Context expContext = value.genConsVisit(env);
        String ifNameValue = expContext.valueLabelName;
        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameRtnValue), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtnValue), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(expContext.lockName, ifNameRtnLock), env.hypothesis, location));
        return new Context(null, prevLock);
    }
}
