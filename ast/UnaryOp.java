package ast;

import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;

public class UnaryOp extends Expression {
    UnaryOperator op;
    Expression operand;
    public UnaryOp(UnaryOperator x, Expression y) {
        op = x;
        operand = y;
    }

    @Override
    public String genConsVisit(VisitEnv env) {
        return operand.genConsVisit(env);
    }
}
