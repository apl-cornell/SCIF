package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FunctionDef extends FunctionSig {
    ArrayList<Statement> body;
    public FunctionDef(String name, FuncLabels funcLabels, Arguments args, ArrayList<Statement> body, ArrayList<String> decoratorList, Type rnt) {
        super(name, funcLabels, args, decoratorList, rnt);
        this.body = body;
    }

    @Override
    public String genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;
        String funcName = name;
        env.ctxt += funcName;// + location.toString();

        args.genConsVisit(env);

        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        FuncInfo funcInfo = env.funcMap.get(funcName);

        String ifNameCall = funcInfo.getLabelNameCallPc();
        env.cons.add(new Constraint(new Inequality(ifNameCall, ifNamePc), env.hypothesis, location));

        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameCall), env.hypothesis, location));

        env.varNameMap.incLayer();
        args.genConsVisit(env);
        for (Statement stmt : body) {
            stmt.genConsVisit(env);
        }
        env.varNameMap.decLayer();


        /*if (rtn instanceof LabeledType) {
            if (rtn instanceof DepMap) {
                ((DepMap) rtn).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) rtn).ifl.findPrincipal(env.principalSet);
            }
        }*/
        env.ctxt = originalCtxt;
        return null;
    }
    /*public void findPrincipal(HashSet<String> principalSet) {
        if (sig.name instanceof LabeledType) {
            ((LabeledType) sig.name).ifl.findPrincipal(principalSet);
        }
        sig.args.findPrincipal(principalSet);

        if (sig.rnt instanceof LabeledType) {
            ((LabeledType) sig.rnt).ifl.findPrincipal(principalSet);
        }
    }*/
}
