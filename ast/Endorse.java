package ast;

import utils.FuncInfo;
import utils.IfConstraint;
import utils.LookupMaps;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class Endorse extends Expression {
    Expression value;
    IfLabel from, to;

    public Endorse(Expression value, IfLabel from, IfLabel to) {
        this.value = value;
        this.from = from;
        this.to = to;
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);
        String ifNameRnt = ctxt + "." + "endorse" + location.toString();

        String fromLabel = from.toSherrlocFmt();
        String toLabel = to.toSherrlocFmt();

        cons.add(Utils.genCons(fromLabel, ifNameValue, location));
        cons.add(Utils.genCons(toLabel, ifNameRnt, location));
        return ifNameRnt;
    }
}
