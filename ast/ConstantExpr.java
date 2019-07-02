package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstantExpr extends Expression {
    Constant value;
    public ConstantExpr(Constant value) {
        this.value = value;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {

        String ifNameRnt = Utils.getIfNamePc(ctxt);
        return ifNameRnt;
    }
}
