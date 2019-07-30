package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Name extends Variable {
    public String id;
    Context ctx;
    public Name(String x) {
        id = x;
        ctx = null;
    }
    public Name(String x, Context y) {
        id = x;
        ctx = y;
    }


    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        // assuming the name would be a variable name
        String ifNameRnt = varNameMap.getName(id);
        return ifNameRnt;
    }

    public VarInfo getVarInfo(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        VarInfo rnt = varNameMap.getInfo(id);
        return rnt;
    }
}
