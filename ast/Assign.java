package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Assign extends Statement {
    ArrayList<Expression> targets;
    Expression value;
    public Assign(ArrayList<Expression> targets, Expression value) {
        this.targets = targets;
        this.value = value;
    }


    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String ifNamePc = Utils.getLabelNamePc(ctxt);
        String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);

        for (Expression target : targets) {
            //Assuming target is Name
            String ifNameTgt = varNameMap.get(((Name) target).id);
            cons.add(Utils.genCons(ifNameValue, ifNameTgt, location));
            cons.add(Utils.genCons(ifNamePc, ifNameTgt, location));
        }
        return "";
    }
}
