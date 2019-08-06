package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Compare extends Expression {
    Expression left;
    CompareOperator op;
    Expression right;
    public Compare(Expression l, CompareOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public String genConsVisit(VisitEnv env) {
        String ifNameLeft = left.genConsVisit(env);
        String ifNameRight = right.genConsVisit(env);
        String ifNameRtn = env.ctxt + "." + "cmp" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location));

        return ifNameRtn;
    }
}
