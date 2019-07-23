package ast;

import utils.*;

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
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameLeft = left.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameRight = right.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameRnt = ctxt + "." + "bin" + location.toString();
        cons.add(Utils.genCons(ifNameLeft, ifNameRnt, location));
        cons.add(Utils.genCons(ifNameRight, ifNameRnt, location));
        return ifNameRnt;
    }
}
