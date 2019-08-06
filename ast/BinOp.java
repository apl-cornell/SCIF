package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class BinOp extends Expression {
    Expression left, right;
    BinaryOperator op;
    public BinOp(Expression l, BinaryOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public String genConsVisit(VisitEnv env) {
        String ifNameLeft = left.genConsVisit(env);
        String ifNameRight = right.genConsVisit(env);
        String ifNameRtn = env.ctxt + "." + "bin" + location.toString();
        env.cons.add(new Constraint(new Inequality(ifNameLeft, ifNameRtn), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNameRight, ifNameRtn), env.hypothesis, location));

        return ifNameRtn;
    }
}
