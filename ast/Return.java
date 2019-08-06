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
    public String genConsVisit(VisitEnv env) {
        if (value == null) return null;
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        String ifNameValue = value.genConsVisit(env);

        int occur = env.ctxt.indexOf(".");
        FuncInfo funcInfo = env.funcMap.get(occur >= 0 ? env.ctxt.substring(0, occur) : env.ctxt);
        String ifNameReturn = funcInfo.getLabelNameReturn();
        env.cons.add(new Constraint(new Inequality(ifNameValue, ifNameReturn), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameReturn), env.hypothesis, location));

        return null;
    }
}
