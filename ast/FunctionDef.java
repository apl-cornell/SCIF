package ast;

import sherrlocUtils.Constraint;
import sherrlocUtils.Inequality;
import typecheck.*;

import java.util.ArrayList;

public class FunctionDef extends FunctionSig {
    ArrayList<Statement> body;
    public FunctionDef(String name, FuncLabels funcLabels, Arguments args, ArrayList<Statement> body, ArrayList<String> decoratorList, Type rnt) {
        super(name, funcLabels, args, decoratorList, rnt);
        this.body = body;
    }

    @Override
    public Context genConsVisit(VisitEnv env) {
        String originalCtxt = env.ctxt;
        Context originalContext = env.prevContext;
        String funcName = name;
        env.ctxt += funcName;// + location.toString();

        args.genConsVisit(env);

        String ifNamePc = Utils.getLabelNamePc(env.ctxt);
        FuncInfo funcInfo = env.funcMap.get(funcName);


        String ifNameCall = funcInfo.getLabelNameCallPc();
        env.cons.add(new Constraint(new Inequality(ifNameCall, ifNamePc), env.hypothesis, location));
        env.cons.add(new Constraint(new Inequality(ifNamePc, ifNameCall), env.hypothesis, location));

        String ifNameCallLock = funcInfo.getLabelNameCallLock();

        Context funcBeginContext = new Context(ifNamePc, ifNameCallLock);
        env.prevContext = funcBeginContext;

        env.varNameMap.incLayer();
        args.genConsVisit(env);
        Context tmp = env.prevContext;
        for (Statement stmt : body) {
            tmp = stmt.genConsVisit(env);
            env.prevContext = tmp;
        }
        env.varNameMap.decLayer();

        env.cons.add(new Constraint(new Inequality(tmp.lockName, funcInfo.getLabelNameRtnLock()), env.hypothesis, location));

        /*if (rtn instanceof LabeledType) {
            if (rtn instanceof DepMap) {
                ((DepMap) rtn).findPrincipal(env.principalSet);
            } else {
                ((LabeledType) rtn).ifl.findPrincipal(env.principalSet);
            }
        }*/
        env.ctxt = originalCtxt;
        // don't recover
        // env.prevContext = originalContext;
        return env.prevContext;
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
