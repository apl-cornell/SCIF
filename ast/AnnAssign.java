package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AnnAssign extends Statement {
    public Expression target;
    public Expression annotation;
    public Expression value;
    public boolean isConst;
    public boolean simple; //if the target is a pure name or an expression

    public AnnAssign(Expression target, Expression annotation, Expression value, boolean simple, boolean isConst) {
        this.target = target;
        this.annotation = annotation;
        this.value = value;
        this.simple = simple;
        this.isConst = isConst;
    }

    public void setTarget(Expression target) {
        this.target = target;
    }
    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    @Override
    public void globalInfoVisit(HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap) {
        if (simple) {
            String id = ((Name) target).id;
            CodeLocation loc = location;
            varMap.put(id, Utils.toVarInfo(target, annotation, isConst, loc));
        } else {
            //TODO
        }
    }


    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        if (!simple) {
            //TODO
        }
        String ifNameTgt;
        if (!ctxt.isEmpty()) {
            ifNameTgt = (ctxt.equals("") ? "" : ctxt + ".") + ((Name) target).id;
            String id = ((Name) target).id;
            CodeLocation loc = location;
            varNameMap.add(((Name) target).id, ifNameTgt, Utils.toVarInfo(target, annotation, isConst, loc));
            if (annotation instanceof LabeledType) {
                String ifLabel = ((LabeledType) annotation).ifl.toSherrlocFmt();
                cons.add(Utils.genCons(ifLabel, ifNameTgt, location));
                cons.add(Utils.genCons(ifNameTgt, ifLabel, location));
            }
        } else {
            ifNameTgt = ((Name) target).id;
        }
        String ifNamePc = Utils.getLabelNamePc(ctxt);
        cons.add(Utils.genCons(ifNamePc, ifNameTgt, location));
        if (value != null) {
            String ifNameValue = value.genConsVisit(ctxt, funcMap, cons, varNameMap);
            cons.add(Utils.genCons(ifNameValue, ifNameTgt, value.location));
        }
        return null;
    }

    public void findPrincipal(HashSet<String> principalSet) {
        if (annotation instanceof LabeledType) {
            ((LabeledType) annotation).ifl.findPrincipal(principalSet);
        }
    }
}
