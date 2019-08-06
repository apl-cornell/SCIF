package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

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
    public String genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;
        String funcName = "";
        if (name instanceof LabeledType) {
            funcName = ((LabeledType) name).x.id;

            ((LabeledType) name).ifl.findPrincipal(env.principalSet);
        } else {
            funcName = ((Name) name).id;
        }
        env.ctxt += funcName;// + location.toString();

        args.genConsVisit(env);

        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        FuncInfo funcInfo = env.funcMap.get(funcName);

        if (name instanceof LabeledType) {
            String ifNameCall = funcInfo.getLabelNameCallAfter();
            env.cons.add(new Constraint(new Inequality(ifNameCall, ifNamePc), env.hypothesis, location));

            env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameCall), env.hypothesis, location));

        }

        env.varNameMap.incLayer();
        args.genConsVisit(env);
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        env.varNameMap.decLayer();


        if (rnt instanceof LabeledType) {
            if (rnt instanceof DepMap) {
                ((DepMap) rnt).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) rnt).ifl.findPrincipal(env.principalSet);
            }
        }
        env.ctxt = originalCtxt;
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
