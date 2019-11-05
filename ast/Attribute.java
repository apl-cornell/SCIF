package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Attribute extends TrailerExpr {
    Name attr;
    public  Attribute(Expression v, Name a) {
        value = v;
        attr = a;
    }
    @Override
    public Context genConsVisit(VisitEnv env) {
        String prevLockName = env.prevContext.lockName;
        Context tmp = value.genConsVisit(env);
        String ifNameRnt = tmp.valueLabelName;
        env.cons.add(new Constraint(new Inequality(prevLockName, CompareOperator.Eq, tmp.lockName), env.hypothesis, location));
        return new Context(ifNameRnt, tmp.lockName);
    }
}
