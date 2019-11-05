package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Literal extends Expression {
    @Override
    public Context genConsVisit(VisitEnv env) {
        String ifNameRtn = "LITERAL..." + location.toString();
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameRtn), env.hypothesis, location));

        return new Context(ifNameRtn, env.prevContext.lockName);
    }
}
