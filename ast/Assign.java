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
            String ifNameTgt = "";
            if (target instanceof Name) {
                //Assuming target is Name
                ifNameTgt = varNameMap.getName(((Name) target).id);
                VarInfo varInfo = varNameMap.getInfo(((Name) target).id);
                if (varInfo instanceof TestableVarInfo) {
                    ((TestableVarInfo) varInfo).tested = false;
                    ((TestableVarInfo) varInfo).testedLabel = Utils.DEAD;
                }
            } else if (target instanceof Subscript) {
                ifNameTgt = target.genConsVisit(ctxt, funcMap, cons, varNameMap);
            } else {
                //TODO: error handling
            }
            cons.add(Utils.genCons(ifNameValue, ifNameTgt, location));
            cons.add(Utils.genCons(ifNamePc, ifNameTgt, location));
        }
        return "";
    }
}
