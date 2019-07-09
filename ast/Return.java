package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class Return extends Statement {
    Expression value;
    public Return(Expression value) {
        this.value = value;
    }
    public Return() {
        this.value = null;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        if (value == null) return null;
        String ifNamePc = Utils.getLabelNamePc(ctxt);
        String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);

        int occur = ctxt.indexOf(".");
        FuncInfo funcInfo = funcMap.get(occur >= 0 ? ctxt.substring(0, occur) : ctxt);
        String ifNameReturn = funcInfo.getLabelNameReturn();
        cons.add(Utils.genCons(ifNameReturn, ifNameValue, location));
        cons.add(Utils.genCons(ifNameReturn, ifNamePc, location));
        return null;
    }
}
