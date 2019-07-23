package ast;

import utils.CodeLocation;
import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;

import java.util.ArrayList;
import java.util.HashMap;

public class Assert extends Statement {
    Expression test;
    Expression msg;
    public Assert(Expression test, Expression msg) {
        this.test = test;
        this.msg = msg;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        test.genConsVisit(ctxt, funcMap, cons, varNameMap);
        return null;
    }
}
