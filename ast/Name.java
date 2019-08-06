package ast;

import typecheck.*;

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
    public String genConsVisit(VisitEnv env) {
        // assuming the name would be a variable name
        String ifNameRnt = env.varNameMap.getInfo(id).labelToSherrlocFmt();
        return ifNameRnt;
    }

    public VarInfo getVarInfo(VisitEnv env) {
        VarInfo rnt = env.varNameMap.getInfo(id);
        return rnt;
    }
}
