package ast;

import utils.*;

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

        cons.add(Utils.genCons(ifNameValue, fromLabel, location));
        cons.add(Utils.genCons(ifNameRnt, toLabel, location));
        return ifNameRnt;
    }
}
