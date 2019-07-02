package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class BoolOp extends Expression {
    BoolOperator op;
    Expression left, right;
    public BoolOp(Expression l, BoolOperator x, Expression r) {
        left = l;
        op = x;
        right = r;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameLeft = left.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameRight = right.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameRnt = ctxt + "." + "bool" + location.toString();
        cons.add(Utils.genCons(ifNameRnt, ifNameLeft, location));
        cons.add(Utils.genCons(ifNameRnt, ifNameRight, location));
        return ifNameRnt;
    }
}
