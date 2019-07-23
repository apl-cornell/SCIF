package ast;

import utils.*;

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
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        return operand.genConsVisit(ctxt, funcMap, cons, varNameMap);
    }
}
