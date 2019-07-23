package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Subscript extends TrailerExpr {
    Expression index; //TODO: to be slice
    Context ctx;
    public Subscript(Expression v, Expression i, Context c) {
        value = v;
        index = i;
        ctx = c;
    }
    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameIndex = index.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameRnt = ctxt + "." + "Subscript" + location.toString();
        cons.add(Utils.genCons(ifNameValue, ifNameRnt, location));
        cons.add(Utils.genCons(ifNameIndex, ifNameRnt, location));
        return ifNameRnt;
    }
}
