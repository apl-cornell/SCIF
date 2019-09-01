package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import sherrlocUtils.Relation;
import typecheck.*;

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
    public String genConsVisit(VisitEnv env) {
        //TODO: Assuming value is a Name for now
        String funcName = ((Name) value).id;
        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
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
            FuncInfo funcInfo = env.funcMap.get(funcName);
            if (funcInfo instanceof PolyFuncInfo) {
                ((PolyFuncInfo)funcInfo).apply();
            }
            String ifNameFuncCall = funcInfo.getLabelNameCallBefore();
            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameFuncCall), env.hypothesis, location));


            //TODO: keywords style arg assign
            for (int i = 0; i < args.size(); ++i) {
                Expression arg = args.get(i);
                String ifNameArgValue = arg.genConsVisit(env);
                String ifNameArgLabel = funcInfo.getLabelNameArg(i);
                env.cons.add(new Constraint(new Inequality(ifNameArgValue, Relation.LEQ, ifNameArgLabel), env.hypothesis, location));

                env.cons.add(new Constraint(new Inequality(ifNamePc, Relation.LEQ, ifNameArgLabel), env.hypothesis, location));

            }
            if (funcInfo instanceof PolyFuncInfo) {
                String ifNameCallBeforeLabel = funcInfo.getLabelNameCallBefore();
                String ifNameCallAfterLabel = funcInfo.getLabelNameCallAfter();
                String ifCallBeforeLabel = funcInfo.getCallBeforeLabel();
                String ifCallAfterLabel = funcInfo.getCallAfterLabel();
                if (ifNameCallBeforeLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallBeforeLabel, Relation.EQ, ifNameCallBeforeLabel), location));

                    //env.cons.add(new Constraint(new Inequality(ifNameCallBeforeLabel, ifCallBeforeLabel), func.location));

                }
                if (ifNameCallAfterLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifCallAfterLabel, Relation.EQ, ifNameCallAfterLabel), location));

                    //env.cons.add(new Constraint(new Inequality(ifNameCallAfterLabel, ifCallAfterLabel), func.location));

                }

                String ifNameReturnLabel = funcInfo.getLabelNameReturn();
                String ifReturnLabel = funcInfo.getReturnLabel();
                if (ifReturnLabel != null) {
                    env.cons.add(new Constraint(new Inequality(ifReturnLabel, Relation.EQ, ifNameReturnLabel), location));

                    //env.cons.add(new Constraint(new Inequality(ifNameReturnLabel, ifReturnLabel), func.location));

                }

                for (int i = 0; i < funcInfo.parameters.size(); ++i) {
                    VarInfo arg = funcInfo.parameters.get(i);
                    if (arg.type.ifl == null) continue;;
                    String ifNameArgLabel = funcInfo.getLabelNameArg(i);
                    String ifArgLabel = ((PolyFuncInfo) funcInfo).getArgLabel(i);
                    if (ifArgLabel != null) {
                        env.cons.add(new Constraint(new Inequality(ifNameArgLabel, Relation.LEQ, ifArgLabel), location));

                        //env.cons.add(new Constraint(new Inequality(ifArgLabel, ifNameArgLabel), arg.location));

                    }
                }
            }
            String ifNameFuncReturn = funcInfo.getLabelNameReturn();
            return ifNameFuncReturn;
        }
    }
}
