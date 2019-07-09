package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FunctionDef extends Statement {
    Expression name;
    Arguments args;
    ArrayList<Statement> body;
    ArrayList<Expression> decoratorList;
    Expression rnt;
    public FunctionDef(Expression name, Arguments args, ArrayList<Statement> body, ArrayList<Expression> decoratorList, Expression rnt) {
        this.name = name;
        this.args = args;
        this.body = body;
        this.decoratorList = decoratorList;
        this.rnt = rnt;
    }
    public void setDecoratorList(ArrayList<Expression> decoratorList) {
        this.decoratorList = decoratorList;
    }


    @Override
    public void globalInfoVisit(HashMap<String, VarInfo> varMap, HashMap<String, FuncInfo> funcMap) {
        String funcId;
        IfLabel callLabel = null, returnLabel = null;
        if (name instanceof LabeledType) {
            funcId = ((LabeledType) name).x.id;
            callLabel = ((LabeledType) name).ifl;
        } else {
            funcId = ((Name) name).id;
        }
        if (rnt instanceof LabeledType) {
            returnLabel = ((LabeledType) rnt).ifl;
        }

        ArrayList<VarInfo> argsInfo = args.globalInfoVisit();
        funcMap.put(funcId, new FuncInfo(funcId, callLabel, argsInfo, returnLabel, location));
    }


    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        String funcName = "";
        if (name instanceof LabeledType) {
            funcName = ((LabeledType) name).x.id;
        } else {
            funcName = ((Name) name).id;
        }
        ctxt += funcName;// + location.toString();

        String ifNamePc = Utils.getLabelNamePc(ctxt);
        FuncInfo funcInfo = funcMap.get(funcName);

        if (name instanceof LabeledType) {
            String ifNameCall = funcInfo.getLabelNameCallAfter();
            cons.add(Utils.genCons(ifNamePc, ifNameCall, location));
            cons.add(Utils.genCons(ifNameCall, ifNamePc, location));
        }

        varNameMap.incLayer();
        args.genConsVisit(ctxt, funcMap, cons, varNameMap);
        for (Statement stmt : body) {
            stmt.genConsVisit(ctxt, funcMap, cons, varNameMap);
        }
        varNameMap.decLayer();
        return null;
    }
    public void findPrincipal(HashSet<String> principalSet) {
        if (name instanceof LabeledType) {
            ((LabeledType) name).ifl.findPrincipal(principalSet);
        }
        args.findPrincipal(principalSet);

        if (rnt instanceof LabeledType) {
            ((LabeledType) rnt).ifl.findPrincipal(principalSet);
        }
    }
}
