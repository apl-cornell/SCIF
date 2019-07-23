package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Attribute extends TrailerExpr {
    Name attr;
    Context ctx;
    public  Attribute(Expression v, Name a, Context c) {
        value = v;
        attr = a;
        ctx = c;
    }
    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameRnt = value.genConsVisit(ctxt, funcMap, cons, varNameMap);
        return ifNameRnt;
    }
}
