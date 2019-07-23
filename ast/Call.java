package ast;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Call extends TrailerExpr {
    ArrayList<Expression> args;
    ArrayList<Keyword> keywords;
    //TODO: starargs, kwargs
    public Call() {
        this.args = new ArrayList<>();
        this.keywords = new ArrayList<>();
    }
    public Call(Expression x, ArrayList<Expression> ys) {
        value = x;
        args = ys;
        keywords = null;
    }
    public Call(Expression x, ArrayList<Expression> ys, ArrayList<Keyword> zs) {
        value = x;
        args = ys;
        keywords = zs;
    }
    public void addArg(Expression arg) {
        this.args.add(arg);
    }
    public void addKeyword(Keyword keyword) {
        this.keywords.add(keyword);
    }

    @Override
    public String genConsVisit(String ctxt, HashMap<String, FuncInfo> funcMap, ArrayList<IfConstraint> cons, LookupMaps varNameMap) {
        //TODO: Assuming value is a Name for now
        String funcName = ((Name) value).id;
        String ifNamePc = Utils.getLabelNamePc(ctxt);
        /*if (funcName.equals(Utils.ENDORCEFUNCNAME)) {
            //TODO: didn't add explicit ifLabel expression parsing at this point
            String ifNameExp = args.get(0).genConsVisit(ctxt, funcMap, cons, varNameMap);
            String ifNameFrom = args.get(1).genConsVisit(ctxt, funcMap, cons, varNameMap);
            String ifNameTo = args.get(2).genConsVisit(ctxt, funcMap, cons, varNameMap);
            String ifNameRnt = ctxt + "." + "endorse" + location.toString();
            cons.add(Utils.genCons(ifNameExp, ifNameFrom, location));
            cons.add(Utils.genCons(ifNameFrom, ifNameExp, location));
            cons.add(Utils.genCons(ifNameRnt, ifNameTo, location));
            cons.add(Utils.genCons(ifNameTo, ifNameRnt, location));
            return ifNameRnt;
        }
        else*/ {
            FuncInfo funcInfo = funcMap.get(funcName);
            String ifNameFuncCall = funcInfo.getLabelNameCallBefore();
            cons.add(Utils.genCons(ifNamePc, ifNameFuncCall, location));

            //TODO: keywords style arg assign
            for (int i = 0; i < args.size(); ++i) {
                Expression arg = args.get(i);
                String ifNameArgValue = arg.genConsVisit(ctxt, funcMap, cons, varNameMap);
                String ifNameArgLabel = funcInfo.getLabelNameArg(i);
                cons.add(Utils.genCons(ifNameArgValue, ifNameArgLabel, arg.location));
                cons.add(Utils.genCons(ifNamePc, ifNameArgLabel, arg.location));
            }
            String ifNameFuncReturn = funcInfo.getLabelNameReturn();
            return ifNameFuncReturn;
        }
    }
}
