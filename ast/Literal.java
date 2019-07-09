package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class Literal extends Expression {
    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameRnt = "LITERAL..." + location.toString();
        String ifNamePc = Utils.getLabelNamePc(ctxt);
        cons.add(Utils.genCons(ifNameRnt, ifNamePc, location));
        return ifNameRnt;
    }
}
